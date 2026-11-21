package com.bandybus.infwhat.botania.item;

import com.bandybus.infwhat.botania.InfinityWhatBotania;
import cpw.mods.fml.common.registry.GameRegistry;

/** Standalone item registration for the 1.7.10 release. */
public final class ModItems {

    public static final ItemChannelLinker CHANNEL_LINKER = new ItemChannelLinker();
    public static final ItemEverlastingFertilizer EVERLASTING_FERTILIZER =
            new ItemEverlastingFertilizer();
    public static final ItemRuneTemplate RUNE_TEMPLATE = new ItemRuneTemplate();
    public static final ItemGaiaCharm GAIA_CHARM = new ItemGaiaCharm();

    private ModItems() {
    }

    public static void register() {
        GameRegistry.registerItem(CHANNEL_LINKER, "channel_linker");
        GameRegistry.registerItem(EVERLASTING_FERTILIZER, "everlasting_fertilizer");
        GameRegistry.registerItem(RUNE_TEMPLATE, "rune_template");
        GameRegistry.registerItem(GAIA_CHARM, "gaia_charm");
    }

    static String name(String path) {
        return InfinityWhatBotania.MODID + ":" + path;
    }
}
