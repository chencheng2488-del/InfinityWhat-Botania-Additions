package com.bandybus.infwhat.botania.block;

import com.bandybus.infwhat.botania.block.tile.RuneAcceleratorTileEntity;
import com.bandybus.infwhat.botania.device.AcceleratorKind;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

/** Speeds up nearby runic altars. */
public class RuneAcceleratorBlock extends AbstractAcceleratorBlock {

	public RuneAcceleratorBlock() {
		super(0.6F);
	}

	@Override
	public AcceleratorKind kind() {
		return AcceleratorKind.RUNE;
	}

	@Override
	public TileEntity createTileEntity(World world, IBlockState state) {
		return new RuneAcceleratorTileEntity();
	}
}
