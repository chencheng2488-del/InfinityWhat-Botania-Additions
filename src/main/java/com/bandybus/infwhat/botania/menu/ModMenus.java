package com.bandybus.infwhat.botania.menu;

import com.bandybus.infwhat.botania.InfinityWhatBotania;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Menu type registration. */
public final class ModMenus {

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.CONTAINERS, InfinityWhatBotania.MOD_ID);

    public static final RegistryObject<MenuType<ManaDeviceMenu>> DEVICE =
            MENUS.register("mana_device", () -> IForgeMenuType.create(ManaDeviceMenu::new));

    public static final RegistryObject<MenuType<AcceleratorMenu>> ACCELERATOR =
            MENUS.register("accelerator", () -> IForgeMenuType.create(AcceleratorMenu::new));

    private ModMenus() {
    }

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
}
