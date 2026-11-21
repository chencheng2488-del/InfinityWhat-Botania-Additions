package com.bandybus.infwhat.botania.device;

import net.minecraft.core.BlockPos;

/** Scan boxes used by the mana well and the mana anchor. */
public final class DeviceRanges {

    public static final int MAX_REACH = 8;
    public static final int MAX_OFFSET = 8;

    private DeviceRanges() {
    }

    /** Inclusive box around {@code origin + offset}, expanded by the three reach values. */
    public static Iterable<BlockPos> box(BlockPos origin, int reachX, int reachY, int reachZ,
                                         int offsetX, int offsetY, int offsetZ) {
        BlockPos center = origin.offset(offsetX, offsetY, offsetZ);
        int rx = Math.max(0, Math.min(MAX_REACH, reachX));
        int ry = Math.max(0, Math.min(MAX_REACH, reachY));
        int rz = Math.max(0, Math.min(MAX_REACH, reachZ));
        return BlockPos.betweenClosed(center.offset(-rx, -ry, -rz), center.offset(rx, ry, rz));
    }
}
