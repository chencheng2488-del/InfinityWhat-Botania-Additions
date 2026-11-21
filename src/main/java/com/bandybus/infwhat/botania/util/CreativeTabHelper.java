package com.bandybus.infwhat.botania.util;

import net.minecraft.world.item.CreativeModeTab;

/** Resolves the core mod's creative tab without a hard compile-time dependency. */
public final class CreativeTabHelper {

    private static final String CORE_TAB_CLASS = "com.bandybus.infwhat.creative.ModCreativeTabs";
    private static final String CORE_TAB_FIELD = "INFINITY_WHAT";

    private static boolean resolved;
    private static CreativeModeTab tab;

    private CreativeTabHelper() {
    }

    public static CreativeModeTab coreTab() {
        if (!resolved) {
            resolved = true;
            try {
                Class<?> clazz = Class.forName(CORE_TAB_CLASS);
                Object value = clazz.getField(CORE_TAB_FIELD).get(null);
                if (value instanceof CreativeModeTab) {
                    tab = (CreativeModeTab) value;
                }
            } catch (ReflectiveOperationException ignored) {
                // The core mod is optional; items stay registered but have no creative tab.
                tab = null;
            }
        }
        return tab;
    }
}
