package com.bandybus.infwhat.botania.creative;

import com.bandybus.infwhat.botania.InfinityWhatBotania;
import com.bandybus.infwhat.botania.block.ModBlocks;
import com.bandybus.infwhat.botania.item.ModItems;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Puts the add-on's items into the Infinity What core mod's creative tab instead of registering
 * a separate tab.
 */
@Mod.EventBusSubscriber(modid = InfinityWhatBotania.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class CreativeTabIntegration {

    private static final String CORE_MOD_ID = "infwhat";

    private CreativeTabIntegration() {
    }

    @SubscribeEvent
    public static void onBuildTab(BuildCreativeModeTabContentsEvent event) {
        if (!CORE_MOD_ID.equals(event.getTabKey().location().getNamespace())) {
            return;
        }
        add(event, ModBlocks.MANA_WELL.get().asItem());
        add(event, ModBlocks.MANA_BEACON.get().asItem());
        add(event, ModBlocks.MANA_ANCHOR.get().asItem());
        add(event, ModBlocks.RUNE_ACCELERATOR.get().asItem());
        add(event, ModBlocks.TERRASTEEL_ACCELERATOR.get().asItem());
        add(event, ModBlocks.FLORAL_OVERCLOCK.get().asItem());
        add(event, ModBlocks.GAIA_BEACON.get().asItem());
        add(event, ModItems.CHANNEL_LINKER.get());
        add(event, ModItems.EVERLASTING_FERTILIZER.get());
        add(event, ModItems.RUNE_TEMPLATE.get());
        add(event, ModItems.GAIA_CHARM.get());
    }

    private static void add(BuildCreativeModeTabContentsEvent event,
                            net.minecraft.world.level.ItemLike item) {
        event.accept(new ItemStack(item), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
    }
}
