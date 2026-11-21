package com.bandybus.infwhat.botania.device;

import com.bandybus.infwhat.botania.util.Pos;

/** Inclusive scan box used by the mana well and the mana anchor. */
public final class DeviceRanges {

    public static final int MAX_REACH = 8;
    public static final int MAX_OFFSET = 8;

    private DeviceRanges() {
    }

    /** All positions in the inclusive box around {@code origin + offset}. */
    public static Iterable<Pos> box(Pos origin, int reachX, int reachY, int reachZ,
                                    int offsetX, int offsetY, int offsetZ) {
        int rx = Math.max(0, Math.min(MAX_REACH, reachX));
        int ry = Math.max(0, Math.min(MAX_REACH, reachY));
        int rz = Math.max(0, Math.min(MAX_REACH, reachZ));
        int cx = origin.x + offsetX;
        int cy = origin.y + offsetY;
        int cz = origin.z + offsetZ;
        java.util.List<Pos> result = new java.util.ArrayList<Pos>();
        for (int x = cx - rx; x <= cx + rx; x++) {
            for (int y = cy - ry; y <= cy + ry; y++) {
                for (int z = cz - rz; z <= cz + rz; z++) {
                    result.add(new Pos(x, y, z));
                }
            }
        }
        return result;
    }
}
