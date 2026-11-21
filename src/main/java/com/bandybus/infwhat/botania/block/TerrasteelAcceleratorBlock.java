package com.bandybus.infwhat.botania.block;

import com.bandybus.infwhat.botania.block.tile.TerrasteelAcceleratorTileEntity;
import com.bandybus.infwhat.botania.device.AcceleratorKind;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

/** Feeds nearby Terrestrial Agglomeration Plates. */
public class TerrasteelAcceleratorBlock extends AbstractAcceleratorBlock {

	public TerrasteelAcceleratorBlock() {
		super(0.6F);
	}

	@Override
	public AcceleratorKind kind() {
		return AcceleratorKind.TERRASTEEL;
	}

	@Override
	public TileEntity createTileEntity(World world, IBlockState state) {
		return new TerrasteelAcceleratorTileEntity();
	}
}
