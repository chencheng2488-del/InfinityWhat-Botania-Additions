package com.bandybus.infwhat.botania.block.entity;

import com.bandybus.infwhat.botania.config.BotaniaAdditionConfig;
import com.bandybus.infwhat.botania.device.AcceleratorState;
import com.bandybus.infwhat.botania.device.AcceleratorTarget;
import com.bandybus.infwhat.botania.device.DeviceRanges;
import com.bandybus.infwhat.botania.util.Pos;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

/**
 * Common logic for the three ranged accelerators.
 *
 * <p>The block periodically scans a box around itself for a concrete Botania block entity and
 * then acts on it once per tick. The target list is a cache; each tick re-checks that the target
 * still exists and is still the expected type, so breaking the target cannot leave a stale entry.
 */
public abstract class AbstractAcceleratorTileEntity extends TileEntity {

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
    private final List<Pos> targets = new ArrayList<Pos>();

    protected abstract boolean featureEnabled();

    protected abstract Class<? extends TileEntity> targetType();

    protected abstract void accelerate(TileEntity target);

    protected abstract int defaultRate();

    protected abstract int minRate();

    protected abstract int maxRate();

    protected abstract int defaultRadius();

    /** Cooldown multiplier used while the per-block value is unset. */
    protected int defaultCooldownMultiplier() {
        return 1;
    }

    @Override
    public void updateEntity() {
        if (worldObj == null || worldObj.isRemote) {
            return;
        }
        tickAccelerator();
    }

    public int effectiveCooldownMultiplier() {
        int fallback = defaultCooldownMultiplier();
        int value = cooldownMultiplier < 0 ? fallback : cooldownMultiplier;
        return Math.max(1, value);
    }

    public void setCooldownMultiplier(int value) {
        cooldownMultiplier = Math.max(1, value);
        markDirty();
    }

    public int effectiveRate() {
        int fallback = defaultRate();
        int value = rate < 0 ? fallback : rate;
        return Math.max(minRate(), Math.min(maxRate(), value));
    }

    public int reachX() {
        return clampReach(reachX < 0 ? defaultRadius() : reachX);
    }

    public int reachY() {
        return clampReach(reachY < 0 ? defaultRadius() : reachY);
    }

    public int reachZ() {
        return clampReach(reachZ < 0 ? defaultRadius() : reachZ);
    }

    public int offsetX() {
        return clampOffset(offsetX);
    }

    public int offsetY() {
        return clampOffset(offsetY);
    }

    public int offsetZ() {
        return clampOffset(offsetZ);
    }

    public boolean showRange() {
        return showRange;
    }

    public void setRate(int value) {
        rate = Math.max(minRate(), Math.min(maxRate(), value));
        markDirty();
    }

    public void setRange(int reachX, int reachY, int reachZ,
                         int offsetX, int offsetY, int offsetZ, boolean showRange) {
        this.reachX = clampReach(reachX);
        this.reachY = clampReach(reachY);
        this.reachZ = clampReach(reachZ);
        this.offsetX = clampOffset(offsetX);
        this.offsetY = clampOffset(offsetY);
        this.offsetZ = clampOffset(offsetZ);
        this.showRange = showRange;
        markDirty();
        markUpdated();
    }

    private int clampReach(int value) {
        return Math.max(0, Math.min(
                Math.min(DeviceRanges.MAX_REACH, BotaniaAdditionConfig.acceleratorMaxReach()),
                value));
    }

    private int clampOffset(int value) {
        int max = Math.min(DeviceRanges.MAX_OFFSET, BotaniaAdditionConfig.acceleratorMaxOffset());
        return Math.max(-max, Math.min(max, value));
    }

    protected void tickAccelerator() {
        if (!enabled || !featureEnabled()) {
            return;
        }
        if (scanCooldown-- <= 0) {
            scan();
            scanCooldown = SCAN_INTERVAL;
        }
        Class<? extends TileEntity> expected = targetType();
        for (Pos target : targets) {
            if (!worldObj.blockExists(target.x, target.y, target.z)) {
                continue;
            }
            TileEntity tileEntity = worldObj.getTileEntity(target.x, target.y, target.z);
            if (expected.isInstance(tileEntity)) {
                accelerate(tileEntity);
            }
        }
    }

    private void scan() {
        targets.clear();
        Pos self = Pos.of(this);
        for (Pos candidate : DeviceRanges.box(self, reachX(), reachY(), reachZ(),
                offsetX(), offsetY(), offsetZ())) {
            if (candidate.equals(self)
                    || !worldObj.blockExists(candidate.x, candidate.y, candidate.z)) {
                continue;
            }
            TileEntity tileEntity = worldObj.getTileEntity(candidate.x, candidate.y, candidate.z);
            if (targetType().isInstance(tileEntity)) {
                targets.add(candidate);
            }
        }
    }

    /** Snapshot for the accelerator GUI. */
    public AcceleratorState createState(World world) {
        List<AcceleratorTarget> visible = new ArrayList<AcceleratorTarget>();
        for (Pos target : targets) {
            if (!world.blockExists(target.x, target.y, target.z)) {
                continue;
            }
            String label = com.bandybus.infwhat.botania.util.BlockNames.displayName(
                    world, target.x, target.y, target.z);
            visible.add(new AcceleratorTarget(target, label));
            if (visible.size() >= MAX_TARGETS) {
                break;
            }
        }
        return new AcceleratorState(enabled, featureEnabled(), effectiveRate(),
                effectiveCooldownMultiplier(), reachX(), reachY(), reachZ(),
                offsetX(), offsetY(), offsetZ(), showRange, visible);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isFeatureEnabled() {
        return featureEnabled();
    }

    public boolean toggleEnabled() {
        enabled = !enabled;
        markUpdated();
        return enabled;
    }

    /** Marks the tile dirty and forces a description packet so the client range box updates. */
    protected void markUpdated() {
        markDirty();
        if (worldObj != null && !worldObj.isRemote) {
            worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        saveCommon(tag);
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        loadCommon(tag);
    }

    private void saveCommon(NBTTagCompound tag) {
        tag.setBoolean(TAG_ENABLED, enabled);
        tag.setInteger(TAG_RATE, rate);
        tag.setInteger(TAG_REACH_X, reachX);
        tag.setInteger(TAG_REACH_Y, reachY);
        tag.setInteger(TAG_REACH_Z, reachZ);
        tag.setInteger(TAG_OFFSET_X, offsetX);
        tag.setInteger(TAG_OFFSET_Y, offsetY);
        tag.setInteger(TAG_OFFSET_Z, offsetZ);
        tag.setBoolean(TAG_SHOW_RANGE, showRange);
        tag.setInteger(TAG_COOLDOWN, cooldownMultiplier);
    }

    private void loadCommon(NBTTagCompound tag) {
        enabled = !tag.hasKey(TAG_ENABLED) || tag.getBoolean(TAG_ENABLED);
        int storedRate = tag.hasKey(TAG_RATE) ? tag.getInteger(TAG_RATE) : -1;
        rate = storedRate < 0 ? -1 : Math.max(minRate(), Math.min(maxRate(), storedRate));
        int legacyRadius = tag.hasKey(TAG_LEGACY_RADIUS) ? tag.getInteger(TAG_LEGACY_RADIUS) : -1;
        reachX = tag.hasKey(TAG_REACH_X) ? tag.getInteger(TAG_REACH_X) : legacyRadius;
        reachY = tag.hasKey(TAG_REACH_Y) ? tag.getInteger(TAG_REACH_Y) : legacyRadius;
        reachZ = tag.hasKey(TAG_REACH_Z) ? tag.getInteger(TAG_REACH_Z) : legacyRadius;
        offsetX = tag.hasKey(TAG_OFFSET_X) ? tag.getInteger(TAG_OFFSET_X) : 0;
        offsetY = tag.hasKey(TAG_OFFSET_Y) ? tag.getInteger(TAG_OFFSET_Y) : 0;
        offsetZ = tag.hasKey(TAG_OFFSET_Z) ? tag.getInteger(TAG_OFFSET_Z) : 0;
        showRange = !tag.hasKey(TAG_SHOW_RANGE) || tag.getBoolean(TAG_SHOW_RANGE);
        cooldownMultiplier = tag.hasKey(TAG_COOLDOWN) ? tag.getInteger(TAG_COOLDOWN) : -1;
    }

    @Override
    public Packet getDescriptionPacket() {
        NBTTagCompound tag = new NBTTagCompound();
        saveCommon(tag);
        return new S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, 1, tag);
    }

    @Override
    public void onDataPacket(NetworkManager network, S35PacketUpdateTileEntity packet) {
        loadCommon(packet.getNbtCompound());
    }
}
