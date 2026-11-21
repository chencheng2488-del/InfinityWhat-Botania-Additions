package com.bandybus.infwhat.botania.device;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.Tag;

import java.util.HashSet;
import java.util.Set;

/**
 * Per-block GUI settings shared by the mana well, the beacon and the anchor.
 *
 * <p>{@code rate < 0} means "use the configured default"; {@code reach < 0} does the same for the
 * scan box. The range is used by the well and the anchor to auto-connect nearby mana pools and to
 * draw the range box on the client.
 */
public final class ManaDeviceSettings {

    private static final String TAG_RATE = "Rate";
    private static final String TAG_REACH_X = "ReachX";
    private static final String TAG_REACH_Y = "ReachY";
    private static final String TAG_REACH_Z = "ReachZ";
    private static final String TAG_OFFSET_X = "OffsetX";
    private static final String TAG_OFFSET_Y = "OffsetY";
    private static final String TAG_OFFSET_Z = "OffsetZ";
    private static final String TAG_SHOW_RANGE = "ShowRange";
    private static final String TAG_EXCLUDED = "Excluded";

    private static final int MAX_EXCLUDED = 512;

    private int rate = -1;
    private int reachX = -1;
    private int reachY = -1;
    private int reachZ = -1;
    private int offsetX;
    private int offsetY;
    private int offsetZ;
    private boolean showRange = true;
    private final Set<BlockPos> excluded = new HashSet<>();

    public int rate() {
        return rate;
    }

    public int effectiveRate(int fallback) {
        return rate < 0 ? fallback : rate;
    }

    public void setRate(int value) {
        this.rate = Math.max(-1, value);
    }

    public int reachX(int fallback) {
        return reachX < 0 ? fallback : reachX;
    }

    public int reachY(int fallback) {
        return reachY < 0 ? fallback : reachY;
    }

    public int reachZ(int fallback) {
        return reachZ < 0 ? fallback : reachZ;
    }

    public int offsetX() {
        return offsetX;
    }

    public int offsetY() {
        return offsetY;
    }

    public int offsetZ() {
        return offsetZ;
    }

    public void setRange(int reachX, int reachY, int reachZ,
                         int offsetX, int offsetY, int offsetZ) {
        this.reachX = clampReach(reachX);
        this.reachY = clampReach(reachY);
        this.reachZ = clampReach(reachZ);
        this.offsetX = clampOffset(offsetX);
        this.offsetY = clampOffset(offsetY);
        this.offsetZ = clampOffset(offsetZ);
    }

    public boolean showRange() {
        return showRange;
    }

    public void setShowRange(boolean value) {
        this.showRange = value;
    }

    /** Pools the player unlinked stay excluded from the automatic range scan. */
    public boolean isTargetEnabled(BlockPos pos) {
        return !excluded.contains(pos);
    }

    public void excludeTarget(BlockPos pos) {
        if (excluded.size() < MAX_EXCLUDED) {
            excluded.add(pos.immutable());
        }
    }

    public void clearExcluded() {
        excluded.clear();
    }

    private static int clampReach(int value) {
        return Math.max(0, Math.min(DeviceRanges.MAX_REACH, value));
    }

    /** Keeps the -1 "use the configured default" sentinel instead of turning it into 0. */
    private static int clampLoadedReach(int value) {
        return value < 0 ? -1 : clampReach(value);
    }

    private static int clampOffset(int value) {
        return Math.max(-DeviceRanges.MAX_OFFSET, Math.min(DeviceRanges.MAX_OFFSET, value));
    }

    public void save(CompoundTag tag) {
        tag.putInt(TAG_RATE, rate);
        tag.putInt(TAG_REACH_X, reachX);
        tag.putInt(TAG_REACH_Y, reachY);
        tag.putInt(TAG_REACH_Z, reachZ);
        tag.putInt(TAG_OFFSET_X, offsetX);
        tag.putInt(TAG_OFFSET_Y, offsetY);
        tag.putInt(TAG_OFFSET_Z, offsetZ);
        tag.putBoolean(TAG_SHOW_RANGE, showRange);
        ListTag list = new ListTag();
        for (BlockPos pos : excluded) {
            list.add(LongTag.valueOf(pos.asLong()));
        }
        tag.put(TAG_EXCLUDED, list);
    }

    public void load(CompoundTag tag) {
        rate = tag.contains(TAG_RATE) ? tag.getInt(TAG_RATE) : -1;
        reachX = tag.contains(TAG_REACH_X) ? clampLoadedReach(tag.getInt(TAG_REACH_X)) : -1;
        reachY = tag.contains(TAG_REACH_Y) ? clampLoadedReach(tag.getInt(TAG_REACH_Y)) : -1;
        reachZ = tag.contains(TAG_REACH_Z) ? clampLoadedReach(tag.getInt(TAG_REACH_Z)) : -1;
        offsetX = tag.contains(TAG_OFFSET_X) ? clampOffset(tag.getInt(TAG_OFFSET_X)) : 0;
        offsetY = tag.contains(TAG_OFFSET_Y) ? clampOffset(tag.getInt(TAG_OFFSET_Y)) : 0;
        offsetZ = tag.contains(TAG_OFFSET_Z) ? clampOffset(tag.getInt(TAG_OFFSET_Z)) : 0;
        showRange = !tag.contains(TAG_SHOW_RANGE) || tag.getBoolean(TAG_SHOW_RANGE);
        excluded.clear();
        ListTag list = tag.getList(TAG_EXCLUDED, Tag.TAG_LONG);
        for (int index = 0; index < list.size(); index++) {
            excluded.add(BlockPos.of(((LongTag) list.get(index)).getAsLong()));
        }
    }
}
