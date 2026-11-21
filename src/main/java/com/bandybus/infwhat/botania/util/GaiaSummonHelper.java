package com.bandybus.infwhat.botania.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import vazkii.botania.common.entity.GaiaGuardianEntity;

/**
 * Summons the Gaia Guardian without Botania's beacon, pylon or arena checks.
 *
 * <p>The guardian itself is still created, scaled and announced by Botania's own spawn routine.
 * {@link com.bandybus.infwhat.botania.mixin.GaiaGuardianEntityMixin} only relaxes the structure
 * validation while {@link #isForced()} is true.
 */
public final class GaiaSummonHelper {

    private static boolean forced;

    public static boolean isForced() {
        return forced;
    }

    public static boolean summon(Player player, Level level, BlockPos pos, boolean hardMode) {
        if (level.isClientSide) {
            return false;
        }
        forced = true;
        try {
            return GaiaGuardianEntity.spawn(player, ItemStack.EMPTY, level, pos, hardMode);
        } finally {
            forced = false;
        }
    }

    private GaiaSummonHelper() {
    }
}
