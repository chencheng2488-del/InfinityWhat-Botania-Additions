package com.bandybus.infwhat.botania.block.tile;

import com.bandybus.infwhat.botania.InfinityWhatBotania;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.GameRegistry;

/** Tile-entity registration for the Botania Additions. */
public final class ModTileEntities {

	public static void register() {
		GameRegistry.registerTileEntity(ManaWellTileEntity.class, id("mana_well"));
		GameRegistry.registerTileEntity(ManaBeaconTileEntity.class, id("mana_beacon"));
		GameRegistry.registerTileEntity(ManaAnchorTileEntity.class, id("mana_anchor"));
		GameRegistry.registerTileEntity(RuneAcceleratorTileEntity.class, id("rune_accelerator"));
		GameRegistry.registerTileEntity(TerrasteelAcceleratorTileEntity.class,
				id("terrasteel_accelerator"));
		GameRegistry.registerTileEntity(FloralOverclockTileEntity.class, id("floral_overclock"));
		GameRegistry.registerTileEntity(GaiaBeaconTileEntity.class, id("gaia_beacon"));
	}

	private static ResourceLocation id(String name) {
		return new ResourceLocation(InfinityWhatBotania.MOD_ID, name);
	}

	private ModTileEntities() {
	}
}
