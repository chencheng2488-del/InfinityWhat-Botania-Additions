package com.bandybus.infwhat.botania.client;

import com.bandybus.infwhat.botania.InfinityWhatBotania;
import com.bandybus.infwhat.botania.block.ModBlocks;
import com.bandybus.infwhat.botania.block.entity.ModBlockEntities;
import com.bandybus.infwhat.botania.client.screen.AcceleratorScreen;
import com.bandybus.infwhat.botania.client.screen.ManaDeviceScreen;
import com.bandybus.infwhat.botania.container.ModContainers;
import net.minecraft.client.gui.ScreenManager;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderTypeLookup;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/** Client-only renderer and screen registration. */
@Mod.EventBusSubscriber(modid = InfinityWhatBotania.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {

    private ClientSetup() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ScreenManager.register(ModContainers.DEVICE.get(), ManaDeviceScreen::new);
            ScreenManager.register(ModContainers.ACCELERATOR.get(), AcceleratorScreen::new);

            ClientRegistry.bindTileEntityRenderer(ModBlockEntities.MANA_WELL.get(),
                    dispatcher -> new GlowTileEntityRenderer<>(dispatcher,
                            id("block/mana_well_pool")));
            ClientRegistry.bindTileEntityRenderer(ModBlockEntities.MANA_BEACON.get(),
                    dispatcher -> new GlowTileEntityRenderer<>(dispatcher,
                            id("block/mana_beacon_lens")));
            ClientRegistry.bindTileEntityRenderer(ModBlockEntities.MANA_ANCHOR.get(),
                    dispatcher -> new GlowTileEntityRenderer<>(dispatcher,
                            id("block/mana_anchor_core")));
            ClientRegistry.bindTileEntityRenderer(ModBlockEntities.RUNE_ACCELERATOR.get(),
                    dispatcher -> new GlowTileEntityRenderer<>(dispatcher,
                            id("block/rune_accelerator_top")));
            ClientRegistry.bindTileEntityRenderer(ModBlockEntities.TERRASTEEL_ACCELERATOR.get(),
                    dispatcher -> new GlowTileEntityRenderer<>(dispatcher,
                            id("block/terrasteel_accelerator_top")));
            ClientRegistry.bindTileEntityRenderer(ModBlockEntities.FLORAL_OVERCLOCK.get(),
                    dispatcher -> new GlowTileEntityRenderer<>(dispatcher,
                            id("block/floral_overclock_top")));

            RenderTypeLookup.setRenderLayer(ModBlocks.MANA_WELL.get(), RenderType.cutout());
            RenderTypeLookup.setRenderLayer(ModBlocks.MANA_BEACON.get(), RenderType.cutout());
            RenderTypeLookup.setRenderLayer(ModBlocks.MANA_ANCHOR.get(), RenderType.cutout());
            RenderTypeLookup.setRenderLayer(ModBlocks.RUNE_ACCELERATOR.get(), RenderType.cutout());
            RenderTypeLookup.setRenderLayer(ModBlocks.TERRASTEEL_ACCELERATOR.get(),
                    RenderType.cutout());
            RenderTypeLookup.setRenderLayer(ModBlocks.FLORAL_OVERCLOCK.get(),
                    RenderType.cutout());
            RenderTypeLookup.setRenderLayer(ModBlocks.GAIA_BEACON.get(), RenderType.cutout());
        });
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation(InfinityWhatBotania.MOD_ID, path);
    }
}
