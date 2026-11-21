package com.bandybus.infwhat.botania.block;

/** Shared render id for the custom box renderer; set by the client proxy during pre-init. */
public final class ModRenderIds {

    /** {@code -1} until the client proxy claims a free id. */
    public static int blockModel = -1;

    private ModRenderIds() {
    }
}
