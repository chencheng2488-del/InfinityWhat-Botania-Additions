package com.bandybus.infwhat.botania.block;

import com.bandybus.infwhat.botania.block.entity.GaiaBeaconTileEntity;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.ContainerBlock;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockReader;

/**
 * Finishes an ongoing Gaia fight once the boss bar is ready.
 *
 * <p>It never summons the guardian itself; use the Gaia Charm for that.
 */
public class GaiaBeaconBlock extends ContainerBlock {

    public GaiaBeaconBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockRenderType getRenderShape(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    public TileEntity newBlockEntity(IBlockReader world) {
        return new GaiaBeaconTileEntity();
    }
}
