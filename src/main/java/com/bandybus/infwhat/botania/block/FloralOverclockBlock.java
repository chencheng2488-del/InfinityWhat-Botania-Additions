package com.bandybus.infwhat.botania.block;

import com.bandybus.infwhat.botania.block.tile.FloralOverclockTileEntity;
import com.bandybus.infwhat.botania.device.AcceleratorKind;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

/** Ticks nearby generating flowers multiple times per game tick. */
public class FloralOverclockBlock extends AbstractAcceleratorBlock {

	public FloralOverclockBlock() {
		super(0.45F);
	}

	@Override
	public AcceleratorKind kind() {
		return AcceleratorKind.FLORAL;
	}

	@Override
	public TileEntity createTileEntity(World world, IBlockState state) {
		return new FloralOverclockTileEntity();
	}
}
