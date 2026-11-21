package com.bandybus.infwhat.botania.device;

import com.bandybus.infwhat.botania.util.Pos;

/**
 * One row of the device GUI's connection list.
 *
 * <p>{@code direction} is relative to the device that owns the list: it either pushes mana towards
 * the neighbour ({@link #OUT}), pulls from it ({@link #IN}) or the link works both ways
 * ({@link #PEER}, used by beacon-to-beacon links). {@code auto} marks a link that was created by
 * the range scan rather than by the linker.
 */
public final class LinkEntry {

    public static final int OUT = 1;
    public static final int IN = -1;
    public static final int PEER = 0;

    private final Pos pos;
    private final String label;
    private final int direction;
    private final boolean auto;

    public LinkEntry(Pos pos, String label, int direction, boolean auto) {
        this.pos = pos;
        this.label = label;
        this.direction = direction;
        this.auto = auto;
    }

    public Pos pos() {
        return pos;
    }

    public String label() {
        return label;
    }

    public int direction() {
        return direction;
    }

    public boolean auto() {
        return auto;
    }
}
