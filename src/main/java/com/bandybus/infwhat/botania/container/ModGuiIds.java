package com.bandybus.infwhat.botania.container;

import com.bandybus.infwhat.botania.block.BlockManaDevice;
import com.bandybus.infwhat.botania.device.AcceleratorKind;

/** Stable GUI ids used by {@code player.openGui} and the GUI handler. */
public final class ModGuiIds {

    public static final int MANA_WELL = 0;
    public static final int MANA_BEACON = 1;
    public static final int MANA_ANCHOR = 2;
    public static final int RUNE_ACCELERATOR = 3;
    public static final int TERRASTEEL_ACCELERATOR = 4;
    public static final int FLORAL_OVERCLOCK = 5;

    private ModGuiIds() {
    }

    public static int idFor(BlockManaDevice.Kind kind) {
        if (kind == BlockManaDevice.Kind.WELL) {
            return MANA_WELL;
        }
        return kind == BlockManaDevice.Kind.BEACON ? MANA_BEACON : MANA_ANCHOR;
    }

    public static int idFor(AcceleratorKind kind) {
        if (kind == AcceleratorKind.RUNE) {
            return RUNE_ACCELERATOR;
        }
        return kind == AcceleratorKind.TERRASTEEL ? TERRASTEEL_ACCELERATOR
                : FLORAL_OVERCLOCK;
    }
}
