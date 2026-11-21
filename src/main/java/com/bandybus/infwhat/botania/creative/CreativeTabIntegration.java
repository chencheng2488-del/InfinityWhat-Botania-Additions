package com.bandybus.infwhat.botania.creative;

import com.bandybus.infwhat.botania.block.ModBlocks;
import com.bandybus.infwhat.botania.item.ModItems;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;

/**
 * Puts the add-on's items into the Infinity What core mod's creative tab instead of registering
 * a separate tab.
 */
public final class CreativeTabIntegration {

    private static final ResourceKey<CreativeModeTab> CORE_TAB = ResourceKey.create(
            Registries.CREATIVE_MODE_TAB, new ResourceLocation("infwhat", "infinity_what"));

    private CreativeTabIntegration() {
    }

    public static void register() {
        ItemGroupEvents.modifyEntriesEvent(CORE_TAB).register(entries -> {
            entries.accept(ModBlocks.MANA_WELL.asItem());
            entries.accept(ModBlocks.MANA_BEACON.asItem());
            entries.accept(ModBlocks.MANA_ANCHOR.asItem());
            entries.accept(ModBlocks.RUNE_ACCELERATOR.asItem());
            entries.accept(ModBlocks.TERRASTEEL_ACCELERATOR.asItem());
            entries.accept(ModBlocks.FLORAL_OVERCLOCK.asItem());
            entries.accept(ModBlocks.GAIA_BEACON.asItem());
            entries.accept(ModItems.CHANNEL_LINKER);
            entries.accept(ModItems.EVERLASTING_FERTILIZER);
            entries.accept(ModItems.RUNE_TEMPLATE);
            entries.accept(ModItems.GAIA_CHARM);
        });
    }
}
