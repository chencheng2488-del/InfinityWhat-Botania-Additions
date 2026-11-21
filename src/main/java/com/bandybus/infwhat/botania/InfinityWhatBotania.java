package com.bandybus.infwhat.botania;

import com.bandybus.infwhat.botania.block.ModBlocks;
import com.bandybus.infwhat.botania.block.tile.ModTileEntities;
import com.bandybus.infwhat.botania.config.BotaniaAdditionConfig;
import com.bandybus.infwhat.botania.container.ModGuiHandler;
import com.bandybus.infwhat.botania.crafting.ModRecipes;
import com.bandybus.infwhat.botania.item.ModItems;
import com.bandybus.infwhat.botania.network.ModNetwork;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import vazkii.botania.api.BotaniaAPI;

/**
 * Optional Botania add-on for Infinity What, 1.12.2 Forge edition.
 *
 * <p>The add-on only loads when Botania is installed. It contains the mana well, the wireless
 * mana endpoints, the accelerators, the Gaia helper items and the server-side governance config.
 * The core Infinity What mod remains completely unaware of Botania and of this add-on.
 */
@Mod(modid = InfinityWhatBotania.MOD_ID, name = "Infinity What:Botania Additions", version = "1.0.0",
		acceptedMinecraftVersions = "[1.12,1.13)",
		dependencies = "required-after:botania;after:infwhat;after:jei")
public class InfinityWhatBotania {

	public static final String MOD_ID = "infwhat_botania";
	public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

	@Mod.Instance(MOD_ID)
	public static InfinityWhatBotania instance;

	@SidedProxy(clientSide = "com.bandybus.infwhat.botania.client.ClientProxy",
			serverSide = "com.bandybus.infwhat.botania.CommonProxy")
	public static CommonProxy proxy;

	@Mod.EventHandler
	public void preInit(FMLPreInitializationEvent event) {
		BotaniaAdditionConfig.load(event.getSuggestedConfigurationFile());

		ModBlocks.register();
		ModItems.register();
		ModTileEntities.register();
		ModRecipes.register();
		ModNetwork.register();
		proxy.preInit(event);
	}

	@Mod.EventHandler
	public void init(FMLInitializationEvent event) {
		NetworkRegistry.INSTANCE.registerGuiHandler(this, new ModGuiHandler());
		// Botania's guardian checks this set while it scans the arena for blocks it may break.
		BotaniaAPI.gaiaBreakBlacklist.add(ModBlocks.GAIA_BEACON);
		proxy.init(event);
	}

	@Mod.EventHandler
	public void postInit(FMLPostInitializationEvent event) {
		proxy.postInit(event);
		LOGGER.info("Infinity What: Botania Additions loaded");
	}
}
