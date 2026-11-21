package com.bandybus.infwhat.botania.block;

import com.bandybus.infwhat.botania.device.AcceleratorKind;
import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.block.Block;

/** Block registration for the 1.7.10 release. */
public final class ModBlocks {

    public static final BlockManaDevice MANA_WELL =
            new BlockManaDevice(BlockManaDevice.Kind.WELL, "mana_well");
    public static final BlockManaDevice MANA_BEACON =
            new BlockManaDevice(BlockManaDevice.Kind.BEACON, "mana_beacon");
    public static final BlockManaDevice MANA_ANCHOR =
            new BlockManaDevice(BlockManaDevice.Kind.ANCHOR, "mana_anchor");
    public static final BlockAccelerator RUNE_ACCELERATOR =
            new BlockAccelerator(AcceleratorKind.RUNE, "rune_accelerator");
    public static final BlockAccelerator TERRASTEEL_ACCELERATOR =
            new BlockAccelerator(AcceleratorKind.TERRASTEEL, "terrasteel_accelerator");
    public static final BlockAccelerator FLORAL_OVERCLOCK =
            new BlockAccelerator(AcceleratorKind.FLORAL, "floral_overclock");
    public static final BlockGaiaBeacon GAIA_BEACON = new BlockGaiaBeacon("gaia_beacon");

    private ModBlocks() {
    }

    /** Registers every block with its dedicated one-per-stack block item. */
    public static void register() {
        registerBlock(MANA_WELL, "mana_well");
        registerBlock(MANA_BEACON, "mana_beacon");
        registerBlock(MANA_ANCHOR, "mana_anchor");
        registerBlock(RUNE_ACCELERATOR, "rune_accelerator");
        registerBlock(TERRASTEEL_ACCELERATOR, "terrasteel_accelerator");
        registerBlock(FLORAL_OVERCLOCK, "floral_overclock");
        registerBlock(GAIA_BEACON, "gaia_beacon");
    }

    private static void registerBlock(Block block, String name) {
        // The game registry prefixes the active mod id, so the bare name must be passed here.
        GameRegistry.registerBlock(block, ItemBlockMachine.class, name);
    }
}
