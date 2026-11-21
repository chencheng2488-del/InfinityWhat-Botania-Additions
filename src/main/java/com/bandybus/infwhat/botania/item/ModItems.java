package com.bandybus.infwhat.botania.item;

import com.bandybus.infwhat.botania.InfinityWhatBotania;
import com.bandybus.infwhat.botania.creative.ModCreativeTab;
import net.minecraft.item.Item;
import net.minecraftforge.registries.GameData;

/** Item registration for the Botania Additions. */
public final class ModItems {

	public static Item CHANNEL_LINKER;
	public static Item EVERLASTING_FERTILIZER;
	public static Item RUNE_TEMPLATE;
	public static Item GAIA_CHARM;

	public static void register() {
		CHANNEL_LINKER = register("channel_linker", new ChannelLinkerItem(), 1);
		EVERLASTING_FERTILIZER = register("everlasting_fertilizer",
				new EverlastingFertilizerItem(), 1);
		RUNE_TEMPLATE = register("rune_template", new RuneTemplateItem(), 1);
		GAIA_CHARM = register("gaia_charm", new GaiaCharmItem(), 16);
	}

	private static Item register(String name, Item item, int stackSize) {
		item.setRegistryName(InfinityWhatBotania.MOD_ID, name);
		item.setTranslationKey(InfinityWhatBotania.MOD_ID + "." + name);
		item.setMaxStackSize(stackSize);
		item.setCreativeTab(ModCreativeTab.get());
		GameData.register_impl(item);
		return item;
	}

	private ModItems() {
	}
}
