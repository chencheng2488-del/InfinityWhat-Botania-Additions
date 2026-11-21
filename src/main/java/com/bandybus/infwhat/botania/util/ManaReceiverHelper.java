package com.bandybus.infwhat.botania.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import vazkii.botania.api.mana.IManaPool;
import vazkii.botania.api.mana.IManaReceiver;
import vazkii.botania.common.block.mana.BlockPool;
import vazkii.botania.common.block.tile.mana.TilePool;

import javax.annotation.Nullable;

/** Small wrapper around Botania 1.18.2's direct mana interfaces. */
public final class ManaReceiverHelper {

    private ManaReceiverHelper() {
    }

    @Nullable
    public static IManaReceiver find(Level level, BlockPos pos) {
        if (level == null || pos == null || !level.isLoaded(pos)) {
            return null;
        }
        BlockEntity blockEntity = level.getBlockEntity(pos);
        return blockEntity instanceof IManaReceiver ? (IManaReceiver) blockEntity : null;
    }

    /** Remaining room in a receiver; {@link Integer#MAX_VALUE} when the receiver has no cap. */
    public static int space(IManaReceiver receiver) {
        if (receiver == null || receiver.isFull()) {
            return 0;
        }
        if (receiver instanceof TilePool) {
            return Math.max(0, ((TilePool) receiver).getAvailableSpaceForMana());
        }
        if (receiver instanceof IManaPool) {
            return Integer.MAX_VALUE;
        }
        return Integer.MAX_VALUE;
    }

    /** Creative pools must never be drained or filled by the network. */
    public static boolean isCreativePool(Level level, BlockPos pos) {
        if (level == null || pos == null || !level.isLoaded(pos)) {
            return false;
        }
        if (!(level.getBlockState(pos).getBlock() instanceof BlockPool)) {
            return false;
        }
        return ((BlockPool) level.getBlockState(pos).getBlock()).variant == BlockPool.Variant.CREATIVE;
    }
}
