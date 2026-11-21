package com.bandybus.infwhat.botania.util;

import com.bandybus.infwhat.botania.block.entity.GaiaBeaconTileEntity;
import net.minecraft.tileentity.TileEntity;
import vazkii.botania.api.mana.IManaPool;
import vazkii.botania.common.block.tile.TileEnchanter;
import vazkii.botania.common.block.tile.TileRuneAltar;
import vazkii.botania.common.block.tile.TileTerraPlate;

/**
 * Blocks that the Mana Well and the Mana Anchor connect to automatically.
 *
 * <p>Mana pools are the normal storage targets. The infusion devices are included so a well or
 * anchor can also feed a working altar, plate or enchanter without a manual linker click.
 */
public final class ManaAutoTargets {

    public static boolean isTarget(TileEntity tileEntity) {
        return tileEntity instanceof IManaPool
                || tileEntity instanceof TileRuneAltar
                || tileEntity instanceof TileTerraPlate
                || tileEntity instanceof TileEnchanter
                || tileEntity instanceof GaiaBeaconTileEntity;
    }

    private ManaAutoTargets() {
    }
}
