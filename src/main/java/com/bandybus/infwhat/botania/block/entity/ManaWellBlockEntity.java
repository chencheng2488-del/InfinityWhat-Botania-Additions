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
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import vazkii.botania.api.mana.IManaReceiver;

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
    private boolean nodeRegistered;

    public ManaWellBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MANA_WELL.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state,
                                  ManaWellBlockEntity well) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        well.ensureRegistered(serverLevel);
        if (!BotaniaAdditionConfig.manaWellEnabled()
                || !BotaniaAdditionConfig.canOperate(serverLevel)) {
            return;
        }
        ManaNetworkData network = ManaNetworkData.get(serverLevel);
        if (network == null) {
            return;
        }
        network.validateLinks(serverLevel, pos);
        if (well.scanCooldown-- <= 0) {
            well.scan(serverLevel, network);
            well.scanCooldown = SCAN_INTERVAL;
        }
        if (!well.enabled) {
            return;
        }
        well.pushMana(serverLevel, network);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel serverLevel) {
            ensureRegistered(serverLevel);
        }
    }

    private void ensureRegistered(ServerLevel serverLevel) {
        if (nodeRegistered) {
            return;
        }
        ManaNetworkData data = ManaNetworkData.get(serverLevel);
        if (data != null) {
            data.register(serverLevel, worldPosition, ManaNodeKind.WELL);
            nodeRegistered = true;
        }
    }

    private void scan(ServerLevel serverLevel, ManaNetworkData network) {
        List<BlockPos> candidates = new ArrayList<>();
        for (BlockPos candidate : DeviceRanges.box(worldPosition,
                settings.reachX(BotaniaAdditionConfig.radius()),
                settings.reachY(BotaniaAdditionConfig.radius()),
                settings.reachZ(BotaniaAdditionConfig.radius()),
                settings.offsetX(), settings.offsetY(), settings.offsetZ())) {
            if (candidate.equals(worldPosition) || !level.isLoaded(candidate)
                    || ManaReceiverHelper.isCreativePool(level, candidate)
                    || !settings.isTargetEnabled(candidate)) {
                continue;
            }
            IManaReceiver receiver = ManaReceiverHelper.find(level, candidate);
            if (receiver != null && ManaAutoTargets.isTarget(level.getBlockEntity(candidate))) {
                candidates.add(candidate.immutable());
            }
        }

        for (BlockPos existing : new ArrayList<>(autoTargets)) {
            if (!candidates.contains(existing)) {
                network.autoDisconnect(serverLevel, worldPosition, existing);
            }
        }

        List<BlockPos> linked = new ArrayList<>();
        for (BlockPos candidate : candidates) {
            if (network.hasDirectEdge(serverLevel, worldPosition, candidate)) {
                if (network.isAutoEdge(serverLevel, worldPosition, candidate)) {
                    linked.add(candidate);
                }
                continue;
            }
            ManaNetworkData.ConnectResult result =
                    network.autoConnect(serverLevel, worldPosition, candidate);
            if ((result.result() == ManaNetworkData.Result.OK
                    || result.result() == ManaNetworkData.Result.ALREADY)
                    && network.isAutoEdge(serverLevel, worldPosition, candidate)) {
                linked.add(candidate);
            }
        }

        if (!linked.equals(autoTargets)) {
            autoTargets.clear();
            autoTargets.addAll(linked);
            markUpdated();
        }
    }

    private void pushMana(ServerLevel serverLevel, ManaNetworkData network) {
        int budget = effectiveRate();
        if (budget <= 0) {
            return;
        }
        List<BlockPos> targets = network.wellOutputs(serverLevel, worldPosition);
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
            IManaReceiver receiver = ManaReceiverHelper.find(level, target);
            if (receiver == null || receiver.isFull() || !receiver.canReceiveManaFromBursts()) {
                continue;
            }
            int space = ManaReceiverHelper.space(receiver);
            int amount = Math.min(Math.min(share, space), budget);
            if (amount > 0) {
                receiver.receiveMana(amount);
                budget -= amount;
            }
        }
    }

    public boolean toggleEnabled() {
        enabled = !enabled;
        markUpdated();
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
            markUpdated();
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
        List<LinkEntry> links = network == null
                ? new ArrayList<>()
                : network.links(level, worldPosition);
        return new DeviceState(settings.rate(), effectiveRate(), enabled, 0L, 0L,
                network == null ? "" : network.networkName(level, worldPosition),
                reachX(), reachY(), reachZ(),
                offsetX(), offsetY(), offsetZ(), showRange(), links);
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
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public AABB getRenderBoundingBox() {
        int radius = Math.max(1, Math.max(reachX(), reachZ()) + 1);
        return new AABB(worldPosition).inflate(radius, reachY() + 1, radius);
    }

    private void markUpdated() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }
}
