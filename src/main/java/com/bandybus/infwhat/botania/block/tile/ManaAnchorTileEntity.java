package com.bandybus.infwhat.botania.block.tile;

import com.bandybus.infwhat.botania.config.BotaniaAdditionConfig;
import com.bandybus.infwhat.botania.device.DeviceRanges;
import com.bandybus.infwhat.botania.device.DeviceState;
import com.bandybus.infwhat.botania.device.LinkEntry;
import com.bandybus.infwhat.botania.device.ManaNodeKind;
import com.bandybus.infwhat.botania.grid.ManaNetworkData;
import com.bandybus.infwhat.botania.util.ManaAutoTargets;
import com.bandybus.infwhat.botania.util.ManaReceiverHelper;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import vazkii.botania.api.mana.IManaReceiver;
import vazkii.botania.common.block.tile.mana.TilePool;

import java.util.ArrayList;
import java.util.List;

/**
 * Withdraws mana from the network buffer and pushes it into linked storage blocks and into mana
 * pools inside its scan box.
 *
 * <p>The anchor first calculates how much room its neighbours have, then withdraws at most that
 * amount. Any mana a neighbour refuses is returned to the network, so a full pool cannot silently
 * delete the buffer.
 */
public class ManaAnchorTileEntity extends AbstractManaEndpointTileEntity implements ITickable {

	private static final int SCAN_INTERVAL = 20;
	private static final int DEFAULT_REACH = 1;

	private int scanCooldown;
	private boolean nodeRegistered;

	public ManaAnchorTileEntity() {
	}

	@Override
	public void onLoad() {
		super.onLoad();
		ensureRegistered();
	}

	/** Placement does not call {@link #onLoad()} in 1.12, so the first tick registers too. */
	private void ensureRegistered() {
		if (nodeRegistered || world == null || world.isRemote) {
			return;
		}
		ManaNetworkData data = ManaNetworkData.get(world);
		if (data != null) {
			data.register(world, pos, ManaNodeKind.ANCHOR);
			nodeRegistered = true;
		}
	}

	@Override
	public void update() {
		if (world == null || world.isRemote) {
			return;
		}
		ensureRegistered();
		if (!BotaniaAdditionConfig.wirelessEnabled()
				|| !BotaniaAdditionConfig.canOperate(world)) {
			return;
		}
		ManaNetworkData network = ManaNetworkData.get(world);
		if (network == null) {
			return;
		}
		network.validateLinks(world, pos);
		if (scanCooldown-- <= 0) {
			scan(network);
			scanCooldown = SCAN_INTERVAL;
		}
		if (!isEnabled() || !canOperate(world)) {
			return;
		}

		int perTick = effectiveRate();
		if (perTick <= 0) {
			return;
		}
		List<BlockPos> targets = network.anchorOutputs(world, pos);
		if (targets.isEmpty()) {
			return;
		}

		List<ResolvedTarget> resolved = new ArrayList<ResolvedTarget>();
		long totalSpace = 0L;
		for (BlockPos targetPos : targets) {
			IManaReceiver receiver = ManaReceiverHelper.find(world, targetPos);
			if (receiver == null || receiver.isFull() || !receiver.canRecieveManaFromBursts()) {
				continue;
			}
			boolean exact = receiver instanceof TilePool;
			int space;
			if (exact) {
				space = ManaReceiverHelper.poolSpace((TilePool) receiver);
			} else {
				// Unknown receiver types have no max-mana contract. Give them at most one tick's
				// budget; their accepted amount is not measurable, so it is never refunded.
				space = perTick;
			}
			if (space <= 0) {
				continue;
			}
			resolved.add(new ResolvedTarget(receiver, space, exact));
			totalSpace += space;
		}
		if (resolved.isEmpty() || totalSpace <= 0L) {
			return;
		}

		int wanted = (int) Math.min(perTick, totalSpace);
		long withdrawn = network.withdraw(world, pos, wanted);
		if (withdrawn <= 0L) {
			return;
		}

		int remaining = (int) withdrawn;
		int remainingTargets = resolved.size();
		for (ResolvedTarget target : resolved) {
			if (remaining <= 0) {
				break;
			}
			int share = Math.max(1, remaining / Math.max(1, remainingTargets));
			int amount = Math.min(Math.min(share, target.space), remaining);
			remainingTargets--;
			if (amount <= 0) {
				continue;
			}
			int before = target.exact
					? ((TilePool) target.receiver).getCurrentMana() : 0;
			target.receiver.recieveMana(amount);
			int after = target.exact
					? ((TilePool) target.receiver).getCurrentMana() : 0;
			int accepted = Math.min(amount, Math.max(0, after - before));
			if (target.exact) {
				// Mana pools have a precise current/max contract, so a shortfall is refunded.
				remaining -= accepted;
			} else {
				// For an unknown receiver we cannot distinguish "consumed instantly" from
				// "rejected". Treat the whole amount as consumed; refunding it could duplicate
				// mana when the receiver used it immediately.
				remaining -= amount;
			}
		}
		if (remaining > 0) {
			network.deposit(world, pos, remaining, BotaniaAdditionConfig.maxPerChannel());
		}
	}

	private void scan(ManaNetworkData network) {
		List<BlockPos> candidates = new ArrayList<BlockPos>();
		for (BlockPos candidate : DeviceRanges.box(pos,
				settings().reachX(DEFAULT_REACH), settings().reachY(DEFAULT_REACH),
				settings().reachZ(DEFAULT_REACH),
				settings().offsetX(), settings().offsetY(), settings().offsetZ())) {
			if (candidate.equals(pos) || !world.isBlockLoaded(candidate)
					|| ManaReceiverHelper.isCreativePool(world, candidate)
					|| !settings().isTargetEnabled(candidate)) {
				continue;
			}
			IManaReceiver receiver = ManaReceiverHelper.find(world, candidate);
			if (receiver != null && ManaAutoTargets.isTarget(world.getTileEntity(candidate))) {
				candidates.add(candidate.toImmutable());
			}
		}

		for (BlockPos existing : new ArrayList<BlockPos>(autoTargets())) {
			if (!candidates.contains(existing)) {
				network.autoDisconnect(world, pos, existing);
			}
		}

		List<BlockPos> linked = new ArrayList<BlockPos>();
		for (BlockPos candidate : candidates) {
			if (network.hasDirectEdge(world, pos, candidate)) {
				if (network.isAutoEdge(world, pos, candidate)) {
					linked.add(candidate);
				}
				continue;
			}
			ManaNetworkData.ConnectResult result = network.autoConnect(world, pos, candidate);
			if ((result.result() == ManaNetworkData.Result.OK
					|| result.result() == ManaNetworkData.Result.ALREADY)
					&& network.isAutoEdge(world, pos, candidate)) {
				linked.add(candidate);
			}
		}

		if (!linked.equals(autoTargets())) {
			autoTargets().clear();
			autoTargets().addAll(linked);
			markDirty();
			if (world != null && !world.isRemote) {
				vazkii.botania.api.internal.VanillaPacketDispatcher
						.dispatchTEToNearbyPlayers(world, pos);
			}
		}
	}

	/** Per-tick output budget, clamped to the configured ceiling. */
	public int effectiveRate() {
		return Math.min(settings().effectiveRate(BotaniaAdditionConfig.manaPerTick()),
				BotaniaAdditionConfig.maxManaPerTick());
	}

	@Override
	public int reachX() {
		return settings().reachX(DEFAULT_REACH);
	}

	@Override
	public int reachY() {
		return settings().reachY(DEFAULT_REACH);
	}

	@Override
	public int reachZ() {
		return settings().reachZ(DEFAULT_REACH);
	}

	@Override
	public int offsetX() {
		return settings().offsetX();
	}

	@Override
	public int offsetY() {
		return settings().offsetY();
	}

	@Override
	public int offsetZ() {
		return settings().offsetZ();
	}

	@Override
	public boolean showRange() {
		return settings().showRange();
	}

	@Override
	public DeviceState createState(World worldIn) {
		ManaNetworkData network = ManaNetworkData.get(worldIn);
		List<LinkEntry> links = network == null
				? new ArrayList<LinkEntry>()
				: network.links(worldIn, pos);
		return new DeviceState(settings().rate(), effectiveRate(), isEnabled(),
				network == null ? 0L : network.buffer(worldIn, pos),
				BotaniaAdditionConfig.maxPerChannel(),
				network == null ? "" : network.networkName(worldIn, pos),
				reachX(), reachY(), reachZ(),
				offsetX(), offsetY(), offsetZ(), showRange(), links);
	}

	@Override
	public net.minecraft.util.math.AxisAlignedBB getRenderBoundingBox() {
		int radius = Math.max(1, Math.max(reachX(), reachZ()) + 1);
		return new net.minecraft.util.math.AxisAlignedBB(pos).grow(radius, reachY() + 1, radius);
	}

	private static final class ResolvedTarget {

		private final IManaReceiver receiver;
		private final int space;
		private final boolean exact;

		private ResolvedTarget(IManaReceiver receiver, int space, boolean exact) {
			this.receiver = receiver;
			this.space = space;
			this.exact = exact;
		}
	}
}
