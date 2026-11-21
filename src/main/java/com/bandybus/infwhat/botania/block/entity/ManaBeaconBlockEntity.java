package com.bandybus.infwhat.botania.block.entity;

import com.bandybus.infwhat.botania.config.BotaniaAdditionConfig;
import com.bandybus.infwhat.botania.device.DeviceState;
import com.bandybus.infwhat.botania.device.LinkEntry;
import com.bandybus.infwhat.botania.device.ManaNodeKind;
import com.bandybus.infwhat.botania.grid.ManaNetworkData;
import com.bandybus.infwhat.botania.util.ManaReceiverHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import vazkii.botania.api.BotaniaForgeCapabilities;
import vazkii.botania.api.mana.IManaPool;
import vazkii.botania.api.mana.IManaReceiver;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Moves mana into and out of a network buffer.
 *
 * <p>A beacon accepts mana pushed in by linked wells (it is an {@link IManaReceiver}), may pull
 * from linked storage blocks, and deposits its buffer into the network. It never invents mana.
 */
public class ManaBeaconBlockEntity extends AbstractManaEndpointBlockEntity implements IManaReceiver {

    private static final int MANA_BUFFER = 100_000;
    private static final String TAG_BUFFER = "BufferedMana";

    private int bufferedMana;

    public ManaBeaconBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MANA_BEACON.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state,
                                  ManaBeaconBlockEntity beacon) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        if (!BotaniaAdditionConfig.wirelessEnabled()
                || !BotaniaAdditionConfig.canOperate(serverLevel)) {
            return;
        }
        ManaNetworkData network = ManaNetworkData.get(serverLevel);
        if (network == null) {
            return;
        }
        network.validateLinks(serverLevel, pos);
        if (!beacon.isEnabled() || !beacon.canOperate(serverLevel)) {
            return;
        }

        long capacity = BotaniaAdditionConfig.maxPerChannel();
        if (beacon.bufferedMana > 0) {
            int keeps = BotaniaAdditionConfig.applyLoss(beacon.bufferedMana);
            long accepted = network.deposit(serverLevel, pos, keeps, capacity);
            int drained = (int) Math.min(beacon.bufferedMana,
                    accepted + (beacon.bufferedMana - keeps));
            if (drained > 0) {
                beacon.bufferedMana -= drained;
                beacon.setChanged();
            }
        }
        if (beacon.bufferedMana >= MANA_BUFFER) {
            return;
        }

        int budget = beacon.effectiveRate();
        if (budget <= 0) {
            return;
        }
        List<BlockPos> sources = network.beaconInputs(serverLevel, pos);
        if (sources.isEmpty()) {
            return;
        }
        int remaining = sources.size();
        for (BlockPos target : sources) {
            if (budget <= 0 || beacon.bufferedMana >= MANA_BUFFER) {
                return;
            }
            int share = Math.max(1, budget / Math.max(1, remaining));
            remaining--;
            IManaReceiver receiver = ManaReceiverHelper.find(serverLevel, target);
            if (!(receiver instanceof IManaPool)
                    || ManaReceiverHelper.isCreativePool(serverLevel, target)) {
                continue;
            }
            int current = receiver.getCurrentMana();
            int space = MANA_BUFFER - beacon.bufferedMana;
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
            beacon.bufferedMana += removed;
            beacon.setChanged();
            budget -= removed;
        }
    }

    /** Per-tick input budget, clamped to the configured ceiling. */
    public int effectiveRate() {
        return Math.min(settings().effectiveRate(BotaniaAdditionConfig.manaPerTick()),
                BotaniaAdditionConfig.maxManaPerTick());
    }

    @Override
    public DeviceState createState(ServerLevel level) {
        ManaNetworkData network = ManaNetworkData.get(level);
        List<LinkEntry> links = network == null
                ? new ArrayList<>()
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
        if (level instanceof ServerLevel serverLevel) {
            ManaNetworkData data = ManaNetworkData.get(serverLevel);
            if (data != null) {
                data.register(serverLevel, worldPosition, ManaNodeKind.BEACON);
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt(TAG_BUFFER, bufferedMana);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        bufferedMana = Math.max(0, tag.getInt(TAG_BUFFER));
    }

    // ---- IManaReceiver: lets a Mana Well push mana in directly ----

    @Override
    public Level getManaReceiverLevel() {
        return level;
    }

    @Override
    public BlockPos getManaReceiverPos() {
        return worldPosition;
    }

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
        setChanged();
    }

    @Override
    public boolean canReceiveManaFromBursts() {
        return isEnabled();
    }

    /**
     * Botania 1.18.2 looks receivers up through the Forge capability, not through
     * {@code instanceof IManaReceiver}; without this the mana spreader cannot bind to the beacon.
     */
    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction side) {
        if (capability == BotaniaForgeCapabilities.MANA_RECEIVER) {
            return LazyOptional.of(() -> this).cast();
        }
        return super.getCapability(capability, side);
    }
}
