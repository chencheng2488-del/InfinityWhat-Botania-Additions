package com.bandybus.infwhat.botania.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;
import vazkii.botania.api.BotaniaForgeCapabilities;
import vazkii.botania.api.mana.ManaReceiver;

/** Small wrapper around Forge's block-entity capability lookup. */
public final class ManaReceiverHelper {

    @Nullable
    public static ManaReceiver find(Level level, BlockPos pos, @Nullable Direction side) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity == null) {
            return null;
        }
        return blockEntity.getCapability(BotaniaForgeCapabilities.MANA_RECEIVER, side)
                .resolve().orElse(null);
    }

    private ManaReceiverHelper() {
    }
}
