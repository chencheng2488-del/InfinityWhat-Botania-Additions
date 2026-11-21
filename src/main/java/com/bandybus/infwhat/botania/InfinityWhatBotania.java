package com.bandybus.infwhat.botania;

import com.bandybus.infwhat.botania.block.ModBlocks;
import com.bandybus.infwhat.botania.block.entity.ModBlockEntities;
import com.bandybus.infwhat.botania.config.BotaniaAdditionConfig;
import com.bandybus.infwhat.botania.container.ModContainers;
import com.bandybus.infwhat.botania.crafting.ModRecipeSerializers;
import com.bandybus.infwhat.botania.item.ModItems;
import com.bandybus.infwhat.botania.network.ModNetwork;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Optional Botania add-on for Infinity What, 1.16.5 Forge edition.
 *
 * <p>The add-on only loads when Botania is installed. It contains the mana well, the wireless
 * mana endpoints, the accelerators, the Gaia helper items and the server-side governance config.
 * The core Infinity What mod remains completely unaware of Botania and of this add-on.
 */
@Mod(InfinityWhatBotania.MOD_ID)
public class InfinityWhatBotania {

    public static final String MOD_ID = "infwhat_botania";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public InfinityWhatBotania() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModBlocks.register(modBus);
        ModItems.register(modBus);
        ModBlockEntities.register(modBus);
        ModRecipeSerializers.register(modBus);
        ModContainers.register(modBus);
        ModNetwork.register();
        modBus.addListener(BotaniaAddonEvents::onCommonSetup);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON,
                BotaniaAdditionConfig.SPEC, "infwhat_botania.toml");
        LOGGER.info("Infinity What: Botania Additions loaded");
    }
}
