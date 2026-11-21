package com.bandybus.infwhat.botania.util;

import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import vazkii.botania.api.mana.IManaReceiver;
import vazkii.botania.api.state.BotaniaStateProps;
import vazkii.botania.api.state.enums.PoolVariant;
import vazkii.botania.common.block.tile.mana.TilePool;

/** Small wrapper around the 1.12 tile-entity lookup for Botania's mana interfaces. */
public final class ManaReceiverHelper {

	public static IManaReceiver find(World world, BlockPos pos) {
		if (world == null || pos == null || !world.isBlockLoaded(pos)) {
			return null;
		}
		TileEntity tileEntity = world.getTileEntity(pos);
		return tileEntity instanceof IManaReceiver ? (IManaReceiver) tileEntity : null;
	}

	/**
	 * Creative mana pools are infinite sources and must never be filled or drained by the add-on.
	 */
	public static boolean isCreativePool(World world, BlockPos pos) {
		IBlockState state = world.getBlockState(pos);
		return state.getBlock() == vazkii.botania.common.block.ModBlocks.pool
				&& state.getValue(BotaniaStateProps.POOL_VARIANT) == PoolVariant.CREATIVE;
	}

	/** Pool capacity, preferring the live cap that Botania computes for diluted pools. */
	public static int poolCapacity(TilePool pool) {
		return pool.manaCap > 0 ? pool.manaCap : TilePool.MAX_MANA;
	}

	public static int poolSpace(TilePool pool) {
		return Math.max(0, poolCapacity(pool) - pool.getCurrentMana());
	}

	private ManaReceiverHelper() {
	}
}
