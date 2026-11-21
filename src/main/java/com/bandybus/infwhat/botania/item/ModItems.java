package com.bandybus.infwhat.botania.item;

import com.bandybus.infwhat.botania.InfinityWhatBotania;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

/** Item registration for the Botania Additions. */
public final class ModItems {

    public static Item CHANNEL_LINKER;
    public static Item EVERLASTING_FERTILIZER;
    public static Item RUNE_TEMPLATE;
    public static Item GAIA_CHARM;

    public static void register() {
        CHANNEL_LINKER = register("channel_linker",
                new ChannelLinkerItem(new Item.Properties().stacksTo(1)));
        EVERLASTING_FERTILIZER = register("everlasting_fertilizer",
                new EverlastingFertilizerItem(new Item.Properties().stacksTo(1)));
        RUNE_TEMPLATE = register("rune_template",
                new RuneTemplateItem(new Item.Properties().stacksTo(1)));
        GAIA_CHARM = register("gaia_charm",
                new GaiaCharmItem(new Item.Properties().stacksTo(16)));
    }

    /** Registers a plain item; also used by {@link com.bandybus.infwhat.botania.block.ModBlocks}
     *  for the block items so both live under the same mod id. */
    public static Item register(String name, Item item) {
        return Registry.register(BuiltInRegistries.ITEM,
                new ResourceLocation(InfinityWhatBotania.MOD_ID, name), item);
    }

    private ModItems() {
    }
}
