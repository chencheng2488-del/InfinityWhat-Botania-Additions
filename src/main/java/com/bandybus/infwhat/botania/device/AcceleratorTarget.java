package com.bandybus.infwhat.botania.device;

import net.minecraft.util.math.BlockPos;

/** One block currently found by an accelerator's scan. */
public final class AcceleratorTarget {

    private final BlockPos pos;
    private final String label;

    public AcceleratorTarget(BlockPos pos, String label) {
        this.pos = pos;
        this.label = label;
    }

    public BlockPos pos() {
        return pos;
    }

    public String label() {
        return label;
    }
}
