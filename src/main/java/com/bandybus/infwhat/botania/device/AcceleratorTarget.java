package com.bandybus.infwhat.botania.device;

import com.bandybus.infwhat.botania.util.Pos;

/** One block currently found by an accelerator's scan. */
public final class AcceleratorTarget {

    private final Pos pos;
    private final String label;

    public AcceleratorTarget(Pos pos, String label) {
        this.pos = pos;
        this.label = label;
    }

    public Pos pos() {
        return pos;
    }

    public String label() {
        return label;
    }
}
