package com.bandybus.infwhat.botania.item;

import com.bandybus.infwhat.botania.InfinityWhatBotania;
import com.bandybus.infwhat.botania.util.CreativeTabHelper;
import net.minecraft.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

/** Item registration for the Botania Additions. */
public final class ModItems {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, InfinityWhatBotania.MOD_ID);

    public static final RegistryObject<Item> CHANNEL_LINKER = ITEMS.register(
            "channel_linker",
            () -> new ChannelLinkerItem(new Item.Properties().stacksTo(1)
                    .tab(CreativeTabHelper.coreTab())));

    public static final RegistryObject<Item> EVERLASTING_FERTILIZER = ITEMS.register(
            "everlasting_fertilizer",
            () -> new EverlastingFertilizerItem(new Item.Properties().stacksTo(1)
                    .tab(CreativeTabHelper.coreTab())));

    public static final RegistryObject<Item> RUNE_TEMPLATE = ITEMS.register(
            "rune_template",
            () -> new RuneTemplateItem(new Item.Properties().stacksTo(1)
                    .tab(CreativeTabHelper.coreTab())));

    public static final RegistryObject<Item> GAIA_CHARM = ITEMS.register(
            "gaia_charm",
            () -> new GaiaCharmItem(new Item.Properties().stacksTo(16)
                    .tab(CreativeTabHelper.coreTab())));

    private ModItems() {
    }

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
