package com.bandybus.infwhat.botania.block.entity;

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
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import vazkii.botania.api.mana.ManaPool;
import vazkii.botania.api.mana.ManaReceiver;
import vazkii.botania.common.block.mana.ManaPoolBlock;

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
public class ManaWellBlockEntity extends BlockEntity implements ManaDeviceHost {

    private static final String TAG_ENABLED = "Enabled";
    private static final String TAG_AUTO_TARGETS = "AutoTargets";
    private static final int SCAN_INTERVAL = 20;

    private final ManaDeviceSettings settings = new ManaDeviceSettings();
    private final List<BlockPos> clientLinks = new ArrayList<>();
    private final List<BlockPos> autoTargets = new ArrayList<>();
    private boolean enabled = true;
    private int scanCooldown;

    public ManaWellBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MANA_WELL, pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state,
                                  ManaWellBlockEntity well) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        if (!BotaniaAdditionConfig.manaWellEnabled()
                || !BotaniaAdditionConfig.canOperate(serverLevel)) {
            return;
        }
        ManaNetworkData.get(serverLevel).validateLinks(serverLevel, pos);
        if (well.scanCooldown-- <= 0) {
            well.scan(serverLevel);
            well.scanCooldown = SCAN_INTERVAL;
        }
        if (!well.enabled) {
            return;
        }
        well.pushMana(serverLevel);
    }

    private void scan(ServerLevel level) {
        List<BlockPos> candidates = new ArrayList<>();
        for (BlockPos candidate : DeviceRanges.box(worldPosition,
                settings.reachX(BotaniaAdditionConfig.radius()),
                settings.reachY(BotaniaAdditionConfig.radius()),
                settings.reachZ(BotaniaAdditionConfig.radius()),
                settings.offsetX(), settings.offsetY(), settings.offsetZ())) {
            if (candidate.equals(worldPosition) || !level.hasChunkAt(candidate)
                    || isCreativePool(level, candidate)
                    || !settings.isTargetEnabled(candidate)) {
                continue;
            }
            ManaReceiver receiver = ManaReceiverHelper.find(level, candidate, null);
            if (receiver != null && ManaAutoTargets.isTarget(level.getBlockEntity(candidate))) {
                candidates.add(candidate.immutable());
            }
        }

        ManaNetworkData network = ManaNetworkData.get(level);
        for (BlockPos existing : new ArrayList<>(autoTargets)) {
            if (!candidates.contains(existing)) {
                network.autoDisconnect(level, worldPosition, existing);
            }
        }

        List<BlockPos> linked = new ArrayList<>();
        for (BlockPos candidate : candidates) {
            if (network.hasDirectEdge(level, worldPosition, candidate)) {
                if (network.isAutoEdge(level, worldPosition, candidate)) {
                    linked.add(candidate);
                }
                continue;
            }
            ManaNetworkData.ConnectResult result =
                    network.autoConnect(level, worldPosition, candidate);
            if ((result.result() == ManaNetworkData.Result.OK
                    || result.result() == ManaNetworkData.Result.ALREADY)
                    && network.isAutoEdge(level, worldPosition, candidate)) {
                linked.add(candidate);
            }
        }

        if (!linked.equals(autoTargets)) {
            autoTargets.clear();
            autoTargets.addAll(linked);
            setChanged();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    private void pushMana(ServerLevel level) {
        int budget = effectiveRate();
        if (budget <= 0) {
            return;
        }
        List<BlockPos> targets = ManaNetworkData.get(level).wellOutputs(level, worldPosition);
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
            ManaReceiver receiver = ManaReceiverHelper.find(level, target, null);
            if (receiver == null || receiver.isFull() || !receiver.canReceiveManaFromBursts()) {
                continue;
            }
            int space = Integer.MAX_VALUE;
            if (receiver instanceof ManaPool pool) {
                space = Math.max(0, pool.getMaxMana() - pool.getCurrentMana());
            }
            int amount = Math.min(Math.min(share, space), budget);
            if (amount > 0) {
                receiver.receiveMana(amount);
                budget -= amount;
            }
        }
    }

    private static boolean isCreativePool(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos).getBlock() instanceof ManaPoolBlock pool
                && pool.variant == ManaPoolBlock.Variant.CREATIVE;
    }

    public boolean toggleEnabled() {
        enabled = !enabled;
        setChanged();
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
            setChanged();
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
    public DeviceState createState(ServerLevel level) {
        ManaNetworkData network = ManaNetworkData.get(level);
        List<LinkEntry> links = network.links(level, worldPosition);
        return new DeviceState(settings.rate(), effectiveRate(), enabled, 0L, 0L,
                network.networkName(level, worldPosition),
                reachX(), reachY(), reachZ(),
                offsetX(), offsetY(), offsetZ(), showRange(), links);
    }

    @Override
    public void setLevel(Level level) {
        super.setLevel(level);
        if (level instanceof ServerLevel serverLevel) {
            ManaNetworkData.get(serverLevel).register(serverLevel, worldPosition, ManaNodeKind.WELL);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putBoolean(TAG_ENABLED, enabled);
        settings.save(tag);
        if (level instanceof ServerLevel serverLevel) {
            ManaNetworkData.writeLinks(tag, serverLevel, worldPosition);
        }
        ManaNetworkData.writePositions(tag, TAG_AUTO_TARGETS, autoTargets);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        enabled = !tag.contains(TAG_ENABLED) || tag.getBoolean(TAG_ENABLED);
        settings.load(tag);
        clientLinks.clear();
        clientLinks.addAll(ManaNetworkData.readLinks(tag));
        autoTargets.clear();
        autoTargets.addAll(ManaNetworkData.readPositions(tag, TAG_AUTO_TARGETS));
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Override
    public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }
}
