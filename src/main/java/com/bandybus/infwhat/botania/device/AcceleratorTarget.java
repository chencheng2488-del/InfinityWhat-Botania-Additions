package com.bandybus.infwhat.botania.device;

import net.minecraft.core.BlockPos;

/** One block currently found by an accelerator's scan. */
public record AcceleratorTarget(BlockPos pos, String label) {
}
