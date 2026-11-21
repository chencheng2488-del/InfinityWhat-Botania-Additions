package com.bandybus.infwhat.botania;

import com.bandybus.infwhat.botania.block.ModBlocks;
import com.bandybus.infwhat.botania.block.entity.ModTileEntities;
import com.bandybus.infwhat.botania.config.BotaniaAdditionConfig;
import com.bandybus.infwhat.botania.container.ModGuiHandler;
import com.bandybus.infwhat.botania.item.ModItems;
import com.bandybus.infwhat.botania.network.ModNetwork;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Optional Botania add-on for Infinity What, 1.7.10 Forge edition.
 *
 * <p>The add-on only loads when Botania is installed. It contains the mana well, the wireless
 * mana endpoints, the three accelerators, the Gaia charm and beacon, and their server-side
 * governance config. The core mod remains completely unaware of Botania.
 */
@Mod(modid = InfinityWhatBotania.MODID, name = InfinityWhatBotania.NAME,
        version = InfinityWhatBotania.VERSION,
        dependencies = "required-after:Botania;after:infwhat",
        acceptedMinecraftVersions = "[1.7.10]")
public class InfinityWhatBotania {

    /** Registry id shared by every port of the add-on. */
    public static final String MODID = "infwhat_botania";

    /** Human readable mod name shown by the mod list. */
    public static final String NAME = "Infinity What: Botania Additions";

    /** Mod version. */
    public static final String VERSION = "1.0.0";

    /** Shared logger; 1.7.10 already ships log4j 2. */
    public static final Logger LOGGER = LogManager.getLogger(MODID);

    /** Mod instance used as the GUI handler owner. */
    @Mod.Instance(MODID)
    public static InfinityWhatBotania instance;

    /** Side specific proxy; 1.7.10 needs it for the client-only rendering classes. */
    @SidedProxy(clientSide = "com.bandybus.infwhat.botania.client.ClientProxy",
            serverSide = "com.bandybus.infwhat.botania.CommonProxy")
    public static CommonProxy proxy;

    /** Loads config and registers blocks, items, tile entities and the network channel. */
    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        BotaniaAdditionConfig.load(event.getSuggestedConfigurationFile());
        ModBlocks.register();
        ModItems.register();
        ModTileEntities.register();
        ModNetwork.register();
        com.bandybus.infwhat.botania.network.ServerTaskQueue.register();
        // Waila is optional: the string is only resolved by Waila itself when it is installed.
        cpw.mods.fml.common.event.FMLInterModComms.sendMessage("Waila", "register",
                "com.bandybus.infwhat.botania.compat.waila.WailaIntegration.register");
        proxy.preInit(event);
        LOGGER.info("Infinity What: Botania Additions {} initialised for 1.7.10", VERSION);
    }

    /** Registers recipes and the GUI handler. */
    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        com.bandybus.infwhat.botania.recipes.ModRecipes.register();
        NetworkRegistry.INSTANCE.registerGuiHandler(this, new ModGuiHandler());
        proxy.init(event);
    }
}
