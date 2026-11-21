package com.bandybus.infwhat.botania.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import vazkii.botania.api.BotaniaFabricCapabilities;
import vazkii.botania.api.mana.ManaReceiver;

/** Small wrapper around Botania's Fabric block-entity capability lookup. */
public final class ManaReceiverHelper {

    @Nullable
    public static ManaReceiver find(Level level, BlockPos pos, @Nullable Direction side) {
        return BotaniaFabricCapabilities.MANA_RECEIVER.find(level, pos,
                level.getBlockState(pos), level.getBlockEntity(pos), side);
    }

    private ManaReceiverHelper() {
    }
}
