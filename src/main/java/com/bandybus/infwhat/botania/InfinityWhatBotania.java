package com.bandybus.infwhat.botania;

import com.bandybus.infwhat.botania.block.ModBlocks;
import com.bandybus.infwhat.botania.block.entity.ModBlockEntities;
import com.bandybus.infwhat.botania.config.BotaniaAdditionConfig;
import com.bandybus.infwhat.botania.crafting.ModRecipeSerializers;
import com.bandybus.infwhat.botania.item.ModItems;
import com.bandybus.infwhat.botania.menu.ModMenus;
import com.bandybus.infwhat.botania.network.ModNetwork;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Optional Botania add-on for Infinity What.
 *
 * <p>The add-on only loads when both Infinity What and Botania are installed. It contains the
 * mana well, the wireless mana endpoints, their channel linker and the server-side governance
 * config. The core mod remains completely unaware of Botania.
 */
@Mod(InfinityWhatBotania.MOD_ID)
public class InfinityWhatBotania {

    public static final String MOD_ID = "infwhat_botania";
    public static final Logger LOGGER = LoggerFactory.getLogger("InfinityWhatBotania");

    public InfinityWhatBotania(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();
        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModRecipeSerializers.register(modEventBus);
        ModMenus.register(modEventBus);
        ModNetwork.register();
        BotaniaAdditionConfig.register(context);
        LOGGER.info("Infinity What: Botania Additions loaded");
    }
}
