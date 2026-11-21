package com.bandybus.infwhat.botania.block.entity;

import com.bandybus.infwhat.botania.config.BotaniaAdditionConfig;
import com.bandybus.infwhat.botania.device.DeviceState;
import com.bandybus.infwhat.botania.device.LinkEntry;
import com.bandybus.infwhat.botania.device.ManaNodeKind;
import com.bandybus.infwhat.botania.grid.ManaNetworkData;
import com.bandybus.infwhat.botania.util.ManaReceiverHelper;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.server.ServerWorld;
import vazkii.botania.api.mana.IManaPool;
import vazkii.botania.api.mana.IManaReceiver;

import java.util.ArrayList;
import java.util.List;

/**
 * Moves mana into and out of a network buffer.
 *
 * <p>A beacon accepts mana pushed in by linked wells (it is an {@link IManaReceiver}), may pull
 * from linked storage blocks, and deposits its buffer into the network. It never invents mana.
 */
public class ManaBeaconTileEntity extends AbstractManaEndpointTileEntity
        implements ITickableTileEntity, IManaReceiver {

    private static final int MANA_BUFFER = 100_000;
    private static final String TAG_BUFFER = "BufferedMana";

    private int bufferedMana;

    public ManaBeaconTileEntity() {
        super(ModBlockEntities.MANA_BEACON.get());
    }

    @Override
    public void tick() {
        if (level == null || level.isClientSide) {
            return;
        }
        if (!BotaniaAdditionConfig.wirelessEnabled()
                || !BotaniaAdditionConfig.canOperate(level)) {
            return;
        }
        ManaNetworkData network = ManaNetworkData.get(level);
        if (network == null) {
            return;
        }
        network.validateLinks((ServerWorld) level, worldPosition);
        if (!isEnabled() || !canOperate((ServerWorld) level)) {
            return;
        }

        long capacity = BotaniaAdditionConfig.maxPerChannel();

        // Mana pushed directly by a well (or any other IManaReceiver) is deposited first.
        if (bufferedMana > 0) {
            int keeps = BotaniaAdditionConfig.applyLoss(bufferedMana);
            long accepted = network.deposit((ServerWorld) level, worldPosition, keeps, capacity);
            int drained = (int) Math.min(bufferedMana, accepted + (bufferedMana - keeps));
            if (drained > 0) {
                bufferedMana -= drained;
                markUpdated();
            }
        }
        if (bufferedMana >= MANA_BUFFER) {
            return;
        }

        int budget = effectiveRate();
        if (budget <= 0) {
            return;
        }
        List<BlockPos> sources = network.beaconInputs((ServerWorld) level, worldPosition);
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
            IManaReceiver receiver = ManaReceiverHelper.find(level, target);
            if (!(receiver instanceof IManaPool)
                    || ManaReceiverHelper.isCreativePool(level, target)) {
                continue;
            }
            int current = receiver.getCurrentMana();
            int space = MANA_BUFFER - bufferedMana;
            int extracted = Math.min(Math.min(share, space), Math.min(current, budget));
            if (extracted <= 0) {
                continue;
            }
            receiver.receiveMana(-extracted);
            int after = receiver.getCurrentMana();
            int removed = Math.min(extracted, Math.max(0, current - after));
            if (removed <= 0) {
                continue;
            }
            bufferedMana += removed;
            markUpdated();
            budget -= removed;
        }
    }

    private static boolean isCreativePool(ServerWorld level, BlockPos pos) {
        return ManaReceiverHelper.isCreativePool(level, pos);
    }

    /** Per-tick input budget, clamped to the configured ceiling. */
    public int effectiveRate() {
        return Math.min(settings().effectiveRate(BotaniaAdditionConfig.manaPerTick()),
                BotaniaAdditionConfig.maxManaPerTick());
    }

    @Override
    public DeviceState createState(ServerWorld level) {
        ManaNetworkData network = ManaNetworkData.get(level);
        List<LinkEntry> links = network == null
                ? new ArrayList<LinkEntry>()
                : network.links(level, worldPosition);
        return new DeviceState(settings().rate(), effectiveRate(), isEnabled(),
                network == null ? 0L : network.buffer(level, worldPosition),
                BotaniaAdditionConfig.maxPerChannel(),
                network == null ? "" : network.networkName(level, worldPosition),
                reachX(), reachY(), reachZ(),
                offsetX(), offsetY(), offsetZ(), showRange(), links);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level instanceof ServerWorld) {
            ManaNetworkData data = ManaNetworkData.get(level);
            if (data != null) {
                data.register((ServerWorld) level, worldPosition, ManaNodeKind.BEACON);
            }
        }
    }

    @Override
    public CompoundNBT save(CompoundNBT tag) {
        super.save(tag);
        tag.putInt(TAG_BUFFER, bufferedMana);
        return tag;
    }

    @Override
    public void load(net.minecraft.block.BlockState state, CompoundNBT tag) {
        super.load(state, tag);
        bufferedMana = Math.max(0, tag.getInt(TAG_BUFFER));
    }

    private void markUpdated() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    // ---- IManaReceiver: lets a Mana Well push mana in directly ----

    @Override
    public int getCurrentMana() {
        return bufferedMana;
    }

    @Override
    public boolean isFull() {
        return bufferedMana >= MANA_BUFFER;
    }

    @Override
    public void receiveMana(int mana) {
        if (mana <= 0 || bufferedMana >= MANA_BUFFER) {
            return;
        }
        bufferedMana = Math.min(MANA_BUFFER, bufferedMana + mana);
        markUpdated();
    }

    @Override
    public boolean canReceiveManaFromBursts() {
        return isEnabled();
    }
}
