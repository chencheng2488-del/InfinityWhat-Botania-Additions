package com.bandybus.infwhat.botania.block;

import com.bandybus.infwhat.botania.InfinityWhatBotania;
import com.bandybus.infwhat.botania.item.GaiaBeaconItemBlock;
import com.bandybus.infwhat.botania.item.TooltipBlockItem;
import net.minecraft.block.Block;
import net.minecraft.item.ItemBlock;
import net.minecraftforge.registries.GameData;

/** Block and block-item registration for the Botania Additions. */
public final class ModBlocks {

	public static Block MANA_WELL;
	public static Block MANA_BEACON;
	public static Block MANA_ANCHOR;
	public static Block RUNE_ACCELERATOR;
	public static Block TERRASTEEL_ACCELERATOR;
	public static Block FLORAL_OVERCLOCK;
	public static Block GAIA_BEACON;

	public static void register() {
		MANA_WELL = register("mana_well", new ManaWellBlock());
		MANA_BEACON = register("mana_beacon", new ManaBeaconBlock());
		MANA_ANCHOR = register("mana_anchor", new ManaAnchorBlock());
		RUNE_ACCELERATOR = register("rune_accelerator", new RuneAcceleratorBlock(),
				"tooltip.infwhat_botania.rune_accelerator");
		TERRASTEEL_ACCELERATOR = register("terrasteel_accelerator",
				new TerrasteelAcceleratorBlock(),
				"tooltip.infwhat_botania.terrasteel_accelerator");
		FLORAL_OVERCLOCK = register("floral_overclock", new FloralOverclockBlock(),
				"tooltip.infwhat_botania.floral_overclock");
		GAIA_BEACON = registerGaiaBeacon("gaia_beacon", new GaiaBeaconBlock());
	}

	private static Block register(String name, Block block) {
		return register(name, block, new ItemBlock(block));
	}

	private static Block register(String name, Block block, String tooltipKey) {
		return register(name, block, new TooltipBlockItem(block, tooltipKey));
	}

	private static Block registerGaiaBeacon(String name, Block block) {
		return register(name, block, new GaiaBeaconItemBlock(block));
	}

	private static Block register(String name, Block block, ItemBlock item) {
		block.setRegistryName(InfinityWhatBotania.MOD_ID, name);
		block.setTranslationKey(InfinityWhatBotania.MOD_ID + "." + name);
		GameData.register_impl(block);

		item.setRegistryName(InfinityWhatBotania.MOD_ID, name);
		item.setMaxStackSize(1);
		item.setCreativeTab(com.bandybus.infwhat.botania.creative.ModCreativeTab.get());
		GameData.register_impl(item);
		return block;
	}

	private ModBlocks() {
	}
}
