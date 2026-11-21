package com.bandybus.infwhat.botania;

import com.bandybus.infwhat.botania.block.ModBlocks;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

/** Small startup checks that must run after all registries are frozen. */
public final class BotaniaAddonEvents {

    private BotaniaAddonEvents() {
    }

    public static void onCommonSetup(FMLCommonSetupEvent event) {
        // Touch the blocks so a broken block registration is reported during startup, not on the
        // first placement. The call is intentionally side-effect free.
        ModBlocks.MANA_WELL.get().getRegistryName();
    }
}
