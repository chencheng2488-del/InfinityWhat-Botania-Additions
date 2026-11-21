package com.bandybus.infwhat.botania.util;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import vazkii.botania.api.mana.IManaPool;
import vazkii.botania.api.mana.IManaReceiver;
import vazkii.botania.common.block.mana.BlockPool;
import vazkii.botania.common.block.tile.mana.TilePool;

import javax.annotation.Nullable;

/** Small wrapper around Botania's 1.16.5 mana interfaces and pool helpers. */
public final class ManaReceiverHelper {

    private ManaReceiverHelper() {
    }

    @Nullable
    public static IManaReceiver find(World world, BlockPos pos) {
        if (world == null || pos == null || !world.isLoaded(pos)) {
            return null;
        }
        TileEntity tileEntity = world.getBlockEntity(pos);
        return tileEntity instanceof IManaReceiver ? (IManaReceiver) tileEntity : null;
    }

    /** Remaining room in a receiver; {@link Integer#MAX_VALUE} when the receiver has no cap. */
    public static int space(IManaReceiver receiver) {
        if (receiver == null || receiver.isFull()) {
            return 0;
        }
        if (receiver instanceof TilePool) {
            TilePool pool = (TilePool) receiver;
            return Math.max(0, pool.manaCap - pool.getCurrentMana());
        }
        // Botania 1.16.5's IManaPool has no max-mana getter; other pools are treated as bounded
        // only by their own isFull() answer.
        if (receiver instanceof IManaPool) {
            return Integer.MAX_VALUE;
        }
        return Integer.MAX_VALUE;
    }

    /** Creative pools must never be drained or filled by the network. */
    public static boolean isCreativePool(World world, BlockPos pos) {
        if (world == null || pos == null || !world.isLoaded(pos)) {
            return false;
        }
        if (!(world.getBlockState(pos).getBlock() instanceof BlockPool)) {
            return false;
        }
        return ((BlockPool) world.getBlockState(pos).getBlock()).variant == BlockPool.Variant.CREATIVE;
    }
}
