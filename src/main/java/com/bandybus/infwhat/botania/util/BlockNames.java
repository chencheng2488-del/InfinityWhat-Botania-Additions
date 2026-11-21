package com.bandybus.infwhat.botania.util;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import vazkii.botania.api.subtile.SubTileEntity;
import vazkii.botania.common.block.tile.TileSpecialFlower;

/** Resolves block display names through the block item so metadata variants localize correctly. */
public final class BlockNames {

    private BlockNames() {
    }

    public static String displayName(World world, int x, int y, int z) {
        TileEntity tileEntity = world.getTileEntity(x, y, z);
        if (tileEntity instanceof TileSpecialFlower) {
            // Botania flowers all share one block; the actual name comes from the sub-tile.
            SubTileEntity subTile = ((TileSpecialFlower) tileEntity).getSubTile();
            if (subTile != null) {
                String key = "tile.botania:flower." + subTile.getUnlocalizedName() + ".name";
                String name = StatCollector.translateToLocal(key);
                if (name != null && !name.isEmpty() && !key.equals(name)) {
                    return name;
                }
            }
        }

        Block block = world.getBlock(x, y, z);
        if (block == null || block == Blocks.air) {
            return "?";
        }
        int metadata = world.getBlockMetadata(x, y, z);
        Item item = Item.getItemFromBlock(block);
        if (item != null) {
            String name = new ItemStack(item, 1, metadata).getDisplayName();
            if (name != null && !name.isEmpty()) {
                return name;
            }
        }
        return block.getLocalizedName();
    }
}
