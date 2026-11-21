package com.bandybus.infwhat.botania;

import com.bandybus.infwhat.botania.block.ModBlocks;
import com.bandybus.infwhat.botania.block.entity.ModBlockEntities;
import com.bandybus.infwhat.botania.config.BotaniaAdditionConfig;
import com.bandybus.infwhat.botania.crafting.ModRecipeSerializers;
import com.bandybus.infwhat.botania.creative.CreativeTabIntegration;
import com.bandybus.infwhat.botania.event.ModCapabilities;
import com.bandybus.infwhat.botania.item.ModItems;
import com.bandybus.infwhat.botania.menu.ModMenus;
import com.bandybus.infwhat.botania.network.ModNetwork;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Optional Botania add-on for Infinity What.
 *
 * <p>The add-on only loads when both Infinity What and Botania are installed. It contains the
 * mana well, the wireless mana endpoints, their channel linker and the server-side governance
 * config. The core mod remains completely unaware of Botania.
 */
public class InfinityWhatBotania implements ModInitializer {

    public static final String MOD_ID = "infwhat_botania";
    public static final Logger LOGGER = LoggerFactory.getLogger("InfinityWhatBotania");

    @Override
    public void onInitialize() {
        BotaniaAdditionConfig.load();

        ModItems.register();
        ModBlocks.register();
        ModBlockEntities.register();
        ModRecipeSerializers.register();
        ModMenus.register();
        ModCapabilities.register();
        CreativeTabIntegration.register();
        ModNetwork.registerServer();

        LOGGER.info("Infinity What: Botania Additions loaded");
    }
}
