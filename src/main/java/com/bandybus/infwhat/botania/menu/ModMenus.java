package com.bandybus.infwhat.botania.menu;

import com.bandybus.infwhat.botania.InfinityWhatBotania;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;

/** Menu type registration. */
public final class ModMenus {

    public static MenuType<ManaDeviceMenu> DEVICE;
    public static MenuType<AcceleratorMenu> ACCELERATOR;

    public static void register() {
        DEVICE = Registry.register(BuiltInRegistries.MENU,
                new ResourceLocation(InfinityWhatBotania.MOD_ID, "mana_device"),
                new ExtendedScreenHandlerType<>(
                        (windowId, inventory, data) -> new ManaDeviceMenu(windowId, inventory, data)));
        ACCELERATOR = Registry.register(BuiltInRegistries.MENU,
                new ResourceLocation(InfinityWhatBotania.MOD_ID, "accelerator"),
                new ExtendedScreenHandlerType<>(
                        (windowId, inventory, data) -> new AcceleratorMenu(windowId, inventory, data)));
    }

    private ModMenus() {
    }
}
