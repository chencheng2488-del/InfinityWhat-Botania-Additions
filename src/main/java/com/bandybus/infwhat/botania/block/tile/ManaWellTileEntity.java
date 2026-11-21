package com.bandybus.infwhat.botania.block.tile;

import com.bandybus.infwhat.botania.config.BotaniaAdditionConfig;
import com.bandybus.infwhat.botania.device.DeviceRanges;
import com.bandybus.infwhat.botania.device.DeviceState;
import com.bandybus.infwhat.botania.device.LinkEntry;
import com.bandybus.infwhat.botania.device.ManaDeviceHost;
import com.bandybus.infwhat.botania.device.ManaDeviceSettings;
import com.bandybus.infwhat.botania.device.ManaNodeKind;
import com.bandybus.infwhat.botania.grid.ManaNetworkData;
import com.bandybus.infwhat.botania.util.ManaAutoTargets;
import com.bandybus.infwhat.botania.util.ManaReceiverHelper;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import vazkii.botania.api.mana.IManaReceiver;
import vazkii.botania.common.block.tile.mana.TilePool;

import java.util.ArrayList;
import java.util.List;

/**
 * Finite, configurable mana source.
 *
 * <p>The well pushes its per-tick budget towards explicitly linked receivers and towards mana
 * pools and working infusion devices inside its scan box. The scan box is the same box drawn by
 * the client when "Show range" is on. There is no channel: linking is explicit, nearby targets
 * are connected automatically.
 */
public class ManaWellTileEntity extends TileEntity implements ITickable, ManaDeviceHost {

	private static final String TAG_ENABLED = "Enabled";
	private static final String TAG_AUTO_TARGETS = "AutoTargets";
	private static final int SCAN_INTERVAL = 20;

	private final ManaDeviceSettings settings = new ManaDeviceSettings();
	private final List<BlockPos> clientLinks = new ArrayList<BlockPos>();
	private final List<BlockPos> autoTargets = new ArrayList<BlockPos>();
	private boolean enabled = true;
	private int scanCooldown;
	private boolean nodeRegistered;

	public ManaWellTileEntity() {
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
			data.register(world, pos, ManaNodeKind.WELL);
			nodeRegistered = true;
		}
	}

	@Override
	public void update() {
		if (world == null || world.isRemote) {
			return;
		}
		ensureRegistered();
		if (!BotaniaAdditionConfig.manaWellEnabled()
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
		if (!enabled) {
			return;
		}
		pushMana(network);
	}

	private void scan(ManaNetworkData network) {
		List<BlockPos> candidates = new ArrayList<BlockPos>();
		for (BlockPos candidate : DeviceRanges.box(pos,
				settings.reachX(BotaniaAdditionConfig.radius()),
				settings.reachY(BotaniaAdditionConfig.radius()),
				settings.reachZ(BotaniaAdditionConfig.radius()),
				settings.offsetX(), settings.offsetY(), settings.offsetZ())) {
			if (candidate.equals(pos) || !world.isBlockLoaded(candidate)
					|| ManaReceiverHelper.isCreativePool(world, candidate)
					|| !settings.isTargetEnabled(candidate)) {
				continue;
			}
			IManaReceiver receiver = ManaReceiverHelper.find(world, candidate);
			if (receiver != null && ManaAutoTargets.isTarget(world.getTileEntity(candidate))) {
				candidates.add(candidate.toImmutable());
			}
		}

		for (BlockPos existing : new ArrayList<BlockPos>(autoTargets)) {
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

		if (!linked.equals(autoTargets)) {
			autoTargets.clear();
			autoTargets.addAll(linked);
			markDirty();
			sync();
		}
	}

	private void pushMana(ManaNetworkData network) {
		int budget = effectiveRate();
		if (budget <= 0) {
			return;
		}
		List<BlockPos> targets = network.wellOutputs(world, pos);
		if (targets.isEmpty()) {
			return;
		}
		int remaining = targets.size();
		for (BlockPos target : targets) {
			if (budget <= 0) {
				return;
			}
			int share = Math.max(1, budget / Math.max(1, remaining));
			remaining--;
			IManaReceiver receiver = ManaReceiverHelper.find(world, target);
			if (receiver == null || receiver.isFull() || !receiver.canRecieveManaFromBursts()) {
				continue;
			}
			int space = Integer.MAX_VALUE;
			if (receiver instanceof TilePool) {
				space = ManaReceiverHelper.poolSpace((TilePool) receiver);
			}
			int amount = Math.min(Math.min(share, space), budget);
			if (amount > 0) {
				receiver.recieveMana(amount);
				budget -= amount;
			}
		}
	}

	public boolean toggleEnabled() {
		enabled = !enabled;
		markDirty();
		return enabled;
	}

	/** Per-tick output budget, clamped to the configured ceiling. */
	public int effectiveRate() {
		int fallback = BotaniaAdditionConfig.manaPerTick();
		return Math.min(settings.effectiveRate(fallback),
				BotaniaAdditionConfig.maxManaPerTick());
	}

	@Override
	public ManaDeviceSettings settings() {
		return settings;
	}

	@Override
	public boolean isMasterEnabled() {
		return enabled;
	}

	@Override
	public void setMasterEnabled(boolean value) {
		if (enabled != value) {
			enabled = value;
			markDirty();
		}
	}

	@Override
	public List<BlockPos> clientLinks() {
		return clientLinks;
	}

	@Override
	public List<BlockPos> clientAutoTargets() {
		return autoTargets;
	}

	@Override
	public int reachX() {
		return settings.reachX(BotaniaAdditionConfig.radius());
	}

	@Override
	public int reachY() {
		return settings.reachY(BotaniaAdditionConfig.radius());
	}

	@Override
	public int reachZ() {
		return settings.reachZ(BotaniaAdditionConfig.radius());
	}

	@Override
	public int offsetX() {
		return settings.offsetX();
	}

	@Override
	public int offsetY() {
		return settings.offsetY();
	}

	@Override
	public int offsetZ() {
		return settings.offsetZ();
	}

	@Override
	public boolean showRange() {
		return settings.showRange();
	}

	@Override
	public DeviceState createState(World worldIn) {
		ManaNetworkData network = ManaNetworkData.get(worldIn);
		List<LinkEntry> links = network == null
				? new ArrayList<LinkEntry>()
				: network.links(worldIn, pos);
		return new DeviceState(settings.rate(), effectiveRate(), enabled, 0L, 0L,
				network == null ? "" : network.networkName(worldIn, pos),
				reachX(), reachY(), reachZ(),
				offsetX(), offsetY(), offsetZ(), showRange(), links);
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound tag) {
		super.writeToNBT(tag);
		tag.setBoolean(TAG_ENABLED, enabled);
		settings.save(tag);
		if (world != null && !world.isRemote) {
			ManaNetworkData.writeLinks(tag, world, pos);
		}
		ManaNetworkData.writePositions(tag, TAG_AUTO_TARGETS, autoTargets);
		return tag;
	}

	@Override
	public void readFromNBT(NBTTagCompound tag) {
		super.readFromNBT(tag);
		enabled = !tag.hasKey(TAG_ENABLED) || tag.getBoolean(TAG_ENABLED);
		settings.load(tag);
		clientLinks.clear();
		clientLinks.addAll(ManaNetworkData.readLinks(tag));
		autoTargets.clear();
		autoTargets.addAll(ManaNetworkData.readPositions(tag, TAG_AUTO_TARGETS));
	}

	@Override
	public NBTTagCompound getUpdateTag() {
		return writeToNBT(new NBTTagCompound());
	}

	@Override
	public SPacketUpdateTileEntity getUpdatePacket() {
		return new SPacketUpdateTileEntity(pos, 3, getUpdateTag());
	}

	@Override
	public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity packet) {
		readFromNBT(packet.getNbtCompound());
	}

	@Override
	public AxisAlignedBB getRenderBoundingBox() {
		int radius = Math.max(1, Math.max(reachX(), reachZ()) + 1);
		return new AxisAlignedBB(pos).grow(radius, reachY() + 1, radius);
	}

	private void sync() {
		if (world != null && !world.isRemote) {
			vazkii.botania.api.internal.VanillaPacketDispatcher
					.dispatchTEToNearbyPlayers(world, pos);
		}
	}
}
