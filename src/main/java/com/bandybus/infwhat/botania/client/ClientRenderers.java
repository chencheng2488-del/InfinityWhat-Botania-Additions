package com.bandybus.infwhat.botania.client;

import com.bandybus.infwhat.botania.InfinityWhatBotania;
import com.bandybus.infwhat.botania.block.entity.ModBlockEntities;
import com.bandybus.infwhat.botania.client.screen.AcceleratorScreen;
import com.bandybus.infwhat.botania.client.screen.ManaDeviceScreen;
import com.bandybus.infwhat.botania.menu.ModMenus;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.resources.ResourceLocation;

/** Client-only renderer and screen registration. */
public final class ClientRenderers {

    private ClientRenderers() {
    }

    public static void register() {
        BlockEntityRendererRegistry.register(ModBlockEntities.MANA_WELL,
                context -> new GlowBlockEntityRenderer<>(id("block/mana_well_pool")));
        BlockEntityRendererRegistry.register(ModBlockEntities.MANA_BEACON,
                context -> new GlowBlockEntityRenderer<>(id("block/mana_beacon_lens")));
        BlockEntityRendererRegistry.register(ModBlockEntities.MANA_ANCHOR,
                context -> new GlowBlockEntityRenderer<>(id("block/mana_anchor_core")));
        BlockEntityRendererRegistry.register(ModBlockEntities.RUNE_ACCELERATOR,
                context -> new GlowBlockEntityRenderer<>(id("block/rune_accelerator_top")));
        BlockEntityRendererRegistry.register(ModBlockEntities.TERRASTEEL_ACCELERATOR,
                context -> new GlowBlockEntityRenderer<>(id("block/terrasteel_accelerator_top")));
        BlockEntityRendererRegistry.register(ModBlockEntities.FLORAL_OVERCLOCK,
                context -> new GlowBlockEntityRenderer<>(id("block/floral_overclock_top")));

        MenuScreens.register(ModMenus.DEVICE, ManaDeviceScreen::new);
        MenuScreens.register(ModMenus.ACCELERATOR, AcceleratorScreen::new);

        // A lambda keeps DeviceLinkRenderer (and its custom RenderType) unloaded until the first
        // world frame, after the render system is ready.
        WorldRenderEvents.AFTER_TRANSLUCENT.register(context -> DeviceLinkRenderer.onRender(context));
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation(InfinityWhatBotania.MOD_ID, path);
    }
}
