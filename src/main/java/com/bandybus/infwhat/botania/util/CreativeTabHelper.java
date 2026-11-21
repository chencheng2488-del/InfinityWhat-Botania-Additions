package com.bandybus.infwhat.botania.util;

import net.minecraft.item.ItemGroup;

/** Resolves the core mod's creative tab without a hard compile-time dependency. */
public final class CreativeTabHelper {

    private static final String CORE_TAB_CLASS = "com.bandybus.infwhat.creative.ModCreativeTabs";
    private static final String CORE_TAB_FIELD = "INFINITY_WHAT";

    private static boolean resolved;
    private static ItemGroup tab;

    private CreativeTabHelper() {
    }

    public static ItemGroup coreTab() {
        if (!resolved) {
            resolved = true;
            try {
                Class<?> clazz = Class.forName(CORE_TAB_CLASS);
                Object value = clazz.getField(CORE_TAB_FIELD).get(null);
                if (value instanceof ItemGroup) {
                    tab = (ItemGroup) value;
                }
            } catch (ReflectiveOperationException ignored) {
                // The core mod is optional; items stay registered but have no creative tab.
                tab = null;
            }
        }
        return tab;
    }
}
