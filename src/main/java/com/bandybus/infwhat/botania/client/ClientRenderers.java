package com.bandybus.infwhat.botania.client;

import com.bandybus.infwhat.botania.InfinityWhatBotania;
import com.bandybus.infwhat.botania.block.ModBlocks;
import com.bandybus.infwhat.botania.block.entity.ModBlockEntities;
import com.bandybus.infwhat.botania.client.screen.AcceleratorScreen;
import com.bandybus.infwhat.botania.client.screen.ManaDeviceScreen;
import com.bandybus.infwhat.botania.menu.ModMenus;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/** Client-only renderer registration. */
@Mod.EventBusSubscriber(modid = InfinityWhatBotania.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientRenderers {

    private ClientRenderers() {
    }

    @SubscribeEvent
    public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.MANA_WELL.get(),
                context -> new GlowBlockEntityRenderer<>(id("block/mana_well_pool")));
        event.registerBlockEntityRenderer(ModBlockEntities.MANA_BEACON.get(),
                context -> new GlowBlockEntityRenderer<>(id("block/mana_beacon_lens")));
        event.registerBlockEntityRenderer(ModBlockEntities.MANA_ANCHOR.get(),
                context -> new GlowBlockEntityRenderer<>(id("block/mana_anchor_core")));
        event.registerBlockEntityRenderer(ModBlockEntities.RUNE_ACCELERATOR.get(),
                context -> new GlowBlockEntityRenderer<>(id("block/rune_accelerator_top")));
        event.registerBlockEntityRenderer(ModBlockEntities.TERRASTEEL_ACCELERATOR.get(),
                context -> new GlowBlockEntityRenderer<>(id("block/terrasteel_accelerator_top")));
        event.registerBlockEntityRenderer(ModBlockEntities.FLORAL_OVERCLOCK.get(),
                context -> new GlowBlockEntityRenderer<>(id("block/floral_overclock_top")));
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(ModMenus.DEVICE.get(), ManaDeviceScreen::new);
            MenuScreens.register(ModMenus.ACCELERATOR.get(), AcceleratorScreen::new);
            // The glow textures contain alpha; without a cutout layer they render as black.
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.MANA_WELL.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.MANA_BEACON.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.MANA_ANCHOR.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.RUNE_ACCELERATOR.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.TERRASTEEL_ACCELERATOR.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.FLORAL_OVERCLOCK.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.GAIA_BEACON.get(), RenderType.cutout());
        });
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation(InfinityWhatBotania.MOD_ID, path);
    }
}
