package com.bandybus.infwhat.botania.block.entity;

import com.bandybus.infwhat.botania.InfinityWhatBotania;
import cpw.mods.fml.common.registry.GameRegistry;

/** Tile entity registration for the 1.7.10 release. */
public final class ModTileEntities {

    private ModTileEntities() {
    }

    public static void register() {
        GameRegistry.registerTileEntity(ManaWellTileEntity.class,
                InfinityWhatBotania.MODID + ":mana_well");
        GameRegistry.registerTileEntity(ManaBeaconTileEntity.class,
                InfinityWhatBotania.MODID + ":mana_beacon");
        GameRegistry.registerTileEntity(ManaAnchorTileEntity.class,
                InfinityWhatBotania.MODID + ":mana_anchor");
        GameRegistry.registerTileEntity(RuneAcceleratorTileEntity.class,
                InfinityWhatBotania.MODID + ":rune_accelerator");
        GameRegistry.registerTileEntity(TerrasteelAcceleratorTileEntity.class,
                InfinityWhatBotania.MODID + ":terrasteel_accelerator");
        GameRegistry.registerTileEntity(FloralOverclockTileEntity.class,
                InfinityWhatBotania.MODID + ":floral_overclock");
        GameRegistry.registerTileEntity(GaiaBeaconTileEntity.class,
                InfinityWhatBotania.MODID + ":gaia_beacon");
    }
}
