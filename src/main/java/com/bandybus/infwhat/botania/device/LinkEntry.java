package com.bandybus.infwhat.botania.device;

import net.minecraft.core.BlockPos;

/**
 * One row of the device GUI's connection list.
 *
 * <p>{@code direction} is relative to the device that owns the list: it either pushes mana towards
 * the neighbour ({@link #OUT}), pulls from it ({@link #IN}) or the link works both ways
 * ({@link #PEER}, used by beacon-to-beacon links). {@code auto} marks a link that was created by
 * the range scan rather than by the linker.
 */
public record LinkEntry(BlockPos pos, String label, int direction, boolean auto) {

    public static final int OUT = 1;
    public static final int IN = -1;
    public static final int PEER = 0;
}
