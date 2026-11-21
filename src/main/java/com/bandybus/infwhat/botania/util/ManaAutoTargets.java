package com.bandybus.infwhat.botania.util;

import com.bandybus.infwhat.botania.block.entity.GaiaBeaconBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import vazkii.botania.api.mana.IManaPool;
import vazkii.botania.common.block.tile.TileEnchanter;
import vazkii.botania.common.block.tile.TileRuneAltar;
import vazkii.botania.common.block.tile.TileTerraPlate;

import javax.annotation.Nullable;

/**
 * Blocks that the Mana Well and the Mana Anchor connect to automatically.
 *
 * <p>Mana pools are the normal storage targets. The infusion devices are included so a well or
 * anchor can also feed a working altar, plate or enchanter without a manual linker click.
 */
public final class ManaAutoTargets {

    public static boolean isTarget(@Nullable BlockEntity blockEntity) {
        return blockEntity instanceof IManaPool
                || blockEntity instanceof TileRuneAltar
                || blockEntity instanceof TileTerraPlate
                || blockEntity instanceof TileEnchanter
                || blockEntity instanceof GaiaBeaconBlockEntity;
    }

    private ManaAutoTargets() {
    }
}
