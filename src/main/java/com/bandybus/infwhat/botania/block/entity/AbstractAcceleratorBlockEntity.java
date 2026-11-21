package com.bandybus.infwhat.botania.block.entity;

import com.bandybus.infwhat.botania.config.BotaniaAdditionConfig;
import com.bandybus.infwhat.botania.device.AcceleratorState;
import com.bandybus.infwhat.botania.device.AcceleratorTarget;
import com.bandybus.infwhat.botania.device.DeviceRanges;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * Common logic for the three ranged accelerators.
 *
 * <p>The block periodically scans a box around itself for a concrete Botania block entity and
 * then acts on it once per tick. The target list is a cache; each tick re-checks that the target
 * still exists and is still the expected type, so breaking the target cannot leave a stale entry.
 *
 * <p>Rate, reach, offset and the range-visibility flag are stored per block. A reach of
 * {@code -1} means "use the config default on that axis"; the GUI always writes concrete values
 * after the player applies them.
 */
public abstract class AbstractAcceleratorBlockEntity extends BlockEntity {

    private static final String TAG_ENABLED = "Enabled";
    private static final String TAG_RATE = "Rate";
    private static final String TAG_REACH_X = "ReachX";
    private static final String TAG_REACH_Y = "ReachY";
    private static final String TAG_REACH_Z = "ReachZ";
    private static final String TAG_OFFSET_X = "OffsetX";
    private static final String TAG_OFFSET_Y = "OffsetY";
    private static final String TAG_OFFSET_Z = "OffsetZ";
    private static final String TAG_SHOW_RANGE = "ShowRange";
    private static final String TAG_COOLDOWN = "CooldownMultiplier";
    private static final String TAG_LEGACY_RADIUS = "Radius";
    private static final int SCAN_INTERVAL = 20;
    private static final int MAX_TARGETS = 64;

    private boolean enabled = true;
    private int rate = -1;
    private int cooldownMultiplier = -1;
    private int reachX = -1;
    private int reachY = -1;
    private int reachZ = -1;
    private int offsetX;
    private int offsetY;
    private int offsetZ;
    private boolean showRange = true;
    private int scanCooldown;
    private final List<BlockPos> targets = new ArrayList<>();

    protected AbstractAcceleratorBlockEntity(BlockEntityType<?> type, BlockPos pos,
                                             BlockState state) {
        super(type, pos, state);
    }

    protected abstract boolean featureEnabled();

    protected abstract Class<? extends BlockEntity> targetType();

    protected abstract void accelerate(ServerLevel level, BlockEntity target);

    protected abstract int defaultRate();

    protected abstract int minRate();

    protected abstract int maxRate();

    protected abstract int defaultRadius();

    /** Cooldown multiplier used while the per-block value is unset. */
    protected int defaultCooldownMultiplier() {
        return 1;
    }

    public int effectiveCooldownMultiplier() {
        int fallback = defaultCooldownMultiplier();
        int value = cooldownMultiplier < 0 ? fallback : cooldownMultiplier;
        return Mth.clamp(value, 1, Integer.MAX_VALUE);
    }

    public void setCooldownMultiplier(int value) {
        cooldownMultiplier = Mth.clamp(value, 1, Integer.MAX_VALUE);
        setChanged();
    }

    public int effectiveRate() {
        int fallback = defaultRate();
        int value = rate < 0 ? fallback : rate;
        return Mth.clamp(value, minRate(), maxRate());
    }

    public int reachX() {
        return Mth.clamp(reachX < 0 ? defaultRadius() : reachX, 0, maxReach());
    }

    public int reachY() {
        return Mth.clamp(reachY < 0 ? defaultRadius() : reachY, 0, maxReach());
    }

    public int reachZ() {
        return Mth.clamp(reachZ < 0 ? defaultRadius() : reachZ, 0, maxReach());
    }

    public int offsetX() {
        return Mth.clamp(offsetX, -maxOffset(), maxOffset());
    }

    public int offsetY() {
        return Mth.clamp(offsetY, -maxOffset(), maxOffset());
    }

    public int offsetZ() {
        return Mth.clamp(offsetZ, -maxOffset(), maxOffset());
    }

    public boolean showRange() {
        return showRange;
    }

    public void setRate(int value) {
        rate = Mth.clamp(value, minRate(), maxRate());
        setChanged();
    }

    public void setRange(int reachX, int reachY, int reachZ,
                         int offsetX, int offsetY, int offsetZ, boolean showRange) {
        this.reachX = Mth.clamp(reachX, 0, maxReach());
        this.reachY = Mth.clamp(reachY, 0, maxReach());
        this.reachZ = Mth.clamp(reachZ, 0, maxReach());
        this.offsetX = Mth.clamp(offsetX, -maxOffset(), maxOffset());
        this.offsetY = Mth.clamp(offsetY, -maxOffset(), maxOffset());
        this.offsetZ = Mth.clamp(offsetZ, -maxOffset(), maxOffset());
        this.showRange = showRange;
        setChanged();
    }

    private static int maxReach() {
        return Math.min(DeviceRanges.MAX_REACH, BotaniaAdditionConfig.acceleratorMaxReach());
    }

    private static int maxOffset() {
        return Math.min(DeviceRanges.MAX_OFFSET, BotaniaAdditionConfig.acceleratorMaxOffset());
    }

    protected void tickAccelerator(ServerLevel level) {
        if (!enabled || !featureEnabled()) {
            return;
        }
        if (scanCooldown-- <= 0) {
            scan(level);
            scanCooldown = SCAN_INTERVAL;
        }
        Class<? extends BlockEntity> expected = targetType();
        for (BlockPos target : targets) {
            if (!level.hasChunkAt(target)) {
                continue;
            }
            BlockEntity blockEntity = level.getBlockEntity(target);
            if (expected.isInstance(blockEntity)) {
                accelerate(level, blockEntity);
            }
        }
    }

    private void scan(ServerLevel level) {
        targets.clear();
        for (BlockPos candidate : DeviceRanges.box(worldPosition, reachX(), reachY(), reachZ(),
                offsetX(), offsetY(), offsetZ())) {
            if (candidate.equals(worldPosition) || !level.hasChunkAt(candidate)) {
                continue;
            }
            if (targetType().isInstance(level.getBlockEntity(candidate))) {
                targets.add(candidate.immutable());
            }
        }
    }

    /** Snapshot for the accelerator GUI. */
    public AcceleratorState createState(ServerLevel level) {
        List<AcceleratorTarget> visibleTargets = new ArrayList<>();
        for (BlockPos target : targets) {
            if (!level.hasChunkAt(target)) {
                continue;
            }
            visibleTargets.add(new AcceleratorTarget(target,
                    level.getBlockState(target).getBlock().getName().getString()));
            if (visibleTargets.size() >= MAX_TARGETS) {
                break;
            }
        }
        int cooldown = effectiveCooldownMultiplier();
        return new AcceleratorState(enabled, featureEnabled(), effectiveRate(), cooldown,
                reachX(), reachY(), reachZ(), offsetX(), offsetY(), offsetZ(), showRange,
                visibleTargets);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isFeatureEnabled() {
        return featureEnabled();
    }

    public boolean toggleEnabled() {
        enabled = !enabled;
        setChanged();
        return enabled;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putBoolean(TAG_ENABLED, enabled);
        tag.putInt(TAG_RATE, rate);
        tag.putInt(TAG_REACH_X, reachX);
        tag.putInt(TAG_REACH_Y, reachY);
        tag.putInt(TAG_REACH_Z, reachZ);
        tag.putInt(TAG_OFFSET_X, offsetX);
        tag.putInt(TAG_OFFSET_Y, offsetY);
        tag.putInt(TAG_OFFSET_Z, offsetZ);
        tag.putBoolean(TAG_SHOW_RANGE, showRange);
        tag.putInt(TAG_COOLDOWN, cooldownMultiplier);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        enabled = !tag.contains(TAG_ENABLED) || tag.getBoolean(TAG_ENABLED);
        int storedRate = tag.contains(TAG_RATE) ? tag.getInt(TAG_RATE) : -1;
        rate = storedRate < 0 ? -1 : Mth.clamp(storedRate, minRate(), maxRate());
        int legacyRadius = tag.contains(TAG_LEGACY_RADIUS) ? tag.getInt(TAG_LEGACY_RADIUS) : -1;
        reachX = tag.contains(TAG_REACH_X) ? tag.getInt(TAG_REACH_X) : legacyRadius;
        reachY = tag.contains(TAG_REACH_Y) ? tag.getInt(TAG_REACH_Y) : legacyRadius;
        reachZ = tag.contains(TAG_REACH_Z) ? tag.getInt(TAG_REACH_Z) : legacyRadius;
        offsetX = tag.contains(TAG_OFFSET_X) ? tag.getInt(TAG_OFFSET_X) : 0;
        offsetY = tag.contains(TAG_OFFSET_Y) ? tag.getInt(TAG_OFFSET_Y) : 0;
        offsetZ = tag.contains(TAG_OFFSET_Z) ? tag.getInt(TAG_OFFSET_Z) : 0;
        showRange = !tag.contains(TAG_SHOW_RANGE) || tag.getBoolean(TAG_SHOW_RANGE);
        cooldownMultiplier = tag.contains(TAG_COOLDOWN) ? tag.getInt(TAG_COOLDOWN) : -1;
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
