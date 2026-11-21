package com.bandybus.infwhat.botania.util;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Resolves a block's display name through its pick item.
 *
 * <p>Botania names metadata blocks (mana pools, flowers, altars) through their ItemBlock:
 * {@code Block.getLocalizedName()} returns a raw key such as {@code tile.pool.name} because the
 * block's translation key has no variant suffix. Going through the ItemStack with the block's
 * metadata gives the real name, e.g. {@code 神话魔力池} for a mythic pool.
 */
public final class BlockLabels {

	public static String of(World world, BlockPos pos) {
		if (world == null || pos == null || !world.isBlockLoaded(pos)) {
			return "?";
		}
		IBlockState state = world.getBlockState(pos);
		Block block = state.getBlock();
		Item item = Item.getItemFromBlock(block);
		if (item != null) {
			ItemStack stack = new ItemStack(item, 1, block.getMetaFromState(state));
			String name = stack.getDisplayName();
			if (name != null && !name.isEmpty()) {
				return name;
			}
		}
		return block.getLocalizedName();
	}

	private BlockLabels() {
	}
}
