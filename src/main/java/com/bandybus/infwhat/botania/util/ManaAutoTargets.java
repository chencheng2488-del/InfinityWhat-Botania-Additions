package com.bandybus.infwhat.botania.util;

import com.bandybus.infwhat.botania.block.entity.GaiaBeaconBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;
import vazkii.botania.api.mana.ManaPool;
import vazkii.botania.common.block.block_entity.ManaEnchanterBlockEntity;
import vazkii.botania.common.block.block_entity.RunicAltarBlockEntity;
import vazkii.botania.common.block.block_entity.TerrestrialAgglomerationPlateBlockEntity;

/**
 * Blocks that the Mana Well and the Mana Anchor connect to automatically.
 *
 * <p>Mana pools are the normal storage targets. The infusion devices are included so a well or
 * anchor can also feed a working altar, plate or enchanter without a manual linker click.
 */
public final class ManaAutoTargets {

    public static boolean isTarget(@Nullable BlockEntity blockEntity) {
        return blockEntity instanceof ManaPool
                || blockEntity instanceof RunicAltarBlockEntity
                || blockEntity instanceof TerrestrialAgglomerationPlateBlockEntity
                || blockEntity instanceof ManaEnchanterBlockEntity
                || blockEntity instanceof GaiaBeaconBlockEntity;
    }

    private ManaAutoTargets() {
    }
}
