package com.bandybus.infwhat.botania.block.tile;

import com.bandybus.infwhat.botania.config.BotaniaAdditionConfig;
import com.bandybus.infwhat.botania.device.DeviceState;
import com.bandybus.infwhat.botania.device.LinkEntry;
import com.bandybus.infwhat.botania.device.ManaNodeKind;
import com.bandybus.infwhat.botania.grid.ManaNetworkData;
import com.bandybus.infwhat.botania.util.ManaReceiverHelper;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import vazkii.botania.api.mana.IManaReceiver;
import vazkii.botania.common.block.tile.mana.TilePool;

import java.util.ArrayList;
import java.util.List;

/**
 * Moves mana into and out of a network buffer.
 *
 * <p>A beacon accepts mana pushed in by linked wells (it is an {@link IManaReceiver}), may pull
 * from linked storage blocks, and deposits its buffer into the network. It never invents mana.
 */
public class ManaBeaconTileEntity extends AbstractManaEndpointTileEntity
		implements IManaReceiver, ITickable {

	private static final int MANA_BUFFER = 100_000;
	private static final String TAG_BUFFER = "BufferedMana";

	private int bufferedMana;
	private boolean nodeRegistered;

	public ManaBeaconTileEntity() {
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
			data.register(world, pos, ManaNodeKind.BEACON);
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
		if (!isEnabled() || !canOperate(world)) {
			return;
		}

		long capacity = BotaniaAdditionConfig.maxPerChannel();

		// Mana pushed directly by a well (or any other IManaReceiver) is deposited first.
		if (bufferedMana > 0) {
			int keeps = BotaniaAdditionConfig.applyLoss(bufferedMana);
			long accepted = network.deposit(world, pos, keeps, capacity);
			int drained = (int) Math.min(bufferedMana,
					accepted + (bufferedMana - keeps));
			if (drained > 0) {
				bufferedMana -= drained;
				markDirty();
			}
		}
		if (bufferedMana >= MANA_BUFFER) {
			return;
		}

		int budget = effectiveRate();
		if (budget <= 0) {
			return;
		}
		List<BlockPos> sources = network.beaconInputs(world, pos);
		if (sources.isEmpty()) {
			return;
		}
		int remaining = sources.size();
		for (BlockPos target : sources) {
			if (budget <= 0 || bufferedMana >= MANA_BUFFER) {
				return;
			}
			int share = Math.max(1, budget / Math.max(1, remaining));
			remaining--;
			IManaReceiver receiver = ManaReceiverHelper.find(world, target);
			if (!(receiver instanceof TilePool)
					|| ManaReceiverHelper.isCreativePool(world, target)) {
				continue;
			}
			TilePool pool = (TilePool) receiver;
			int current = pool.getCurrentMana();
			int space = MANA_BUFFER - bufferedMana;
			int extracted = Math.min(Math.min(share, space), Math.min(current, budget));
			if (extracted <= 0) {
				continue;
			}
			pool.recieveMana(-extracted);
			int after = pool.getCurrentMana();
			int removed = Math.min(extracted, Math.max(0, current - after));
			if (removed <= 0) {
				continue;
			}
			bufferedMana += removed;
			markDirty();
			budget -= removed;
		}
	}

	/** Per-tick input budget, clamped to the configured ceiling. */
	public int effectiveRate() {
		return Math.min(settings().effectiveRate(BotaniaAdditionConfig.manaPerTick()),
				BotaniaAdditionConfig.maxManaPerTick());
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
	public NBTTagCompound writeToNBT(NBTTagCompound tag) {
		super.writeToNBT(tag);
		tag.setInteger(TAG_BUFFER, bufferedMana);
		return tag;
	}

	@Override
	public void readFromNBT(NBTTagCompound tag) {
		super.readFromNBT(tag);
		bufferedMana = Math.max(0, tag.getInteger(TAG_BUFFER));
	}

	// ---- IManaReceiver: lets a Mana Well push mana in directly ----

	@Override
	public boolean isFull() {
		return bufferedMana >= MANA_BUFFER;
	}

	@Override
	public void recieveMana(int mana) {
		if (mana <= 0 || bufferedMana >= MANA_BUFFER) {
			return;
		}
		bufferedMana = Math.min(MANA_BUFFER, bufferedMana + mana);
		markDirty();
	}

	@Override
	public boolean canRecieveManaFromBursts() {
		return isEnabled();
	}

	@Override
	public int getCurrentMana() {
		return bufferedMana;
	}
}
