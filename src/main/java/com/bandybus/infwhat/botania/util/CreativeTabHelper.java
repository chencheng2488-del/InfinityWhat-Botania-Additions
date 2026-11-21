package com.bandybus.infwhat.botania.util;

import net.minecraft.creativetab.CreativeTabs;

/**
 * Resolves the core mod's creative tab without a hard compile-time dependency.
 *
 * <p>When Infinity What is installed the add-on's blocks and items are listed in the core mod's
 * own tab instead of a separate "Botania Additions" tab. If the core mod is absent the items stay
 * registered but are not attached to any tab.
 */
public final class CreativeTabHelper {

    private static final String CORE_TAB_CLASS = "com.bandybus.infwhat.creative.ModCreativeTab";
    private static final String CORE_TAB_FIELD = "INSTANCE";

    private static boolean resolved;
    private static CreativeTabs tab;

    private CreativeTabHelper() {
    }

    public static CreativeTabs coreTab() {
        if (!resolved) {
            resolved = true;
            try {
                Class<?> clazz = Class.forName(CORE_TAB_CLASS);
                Object value = clazz.getField(CORE_TAB_FIELD).get(null);
                if (value instanceof CreativeTabs) {
                    tab = (CreativeTabs) value;
                }
            } catch (ReflectiveOperationException ignored) {
                // The core mod is optional; items stay registered but have no creative tab.
                tab = null;
            }
        }
        return tab;
    }
}
