package com.bandybus.infwhat.botania.block;

import com.bandybus.infwhat.botania.block.tile.GaiaBeaconTileEntity;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.block.SoundType;
import net.minecraft.world.World;
import com.bandybus.infwhat.botania.creative.ModCreativeTab;

/**
 * Finishes an ongoing Gaia fight once the boss bar is ready.
 *
 * <p>It never summons the guardian itself; use the Gaia Charm for that.
 */
public class GaiaBeaconBlock extends Block {

	public GaiaBeaconBlock() {
		super(Material.ROCK);
		setHardness(2.0F);
		setResistance(6.0F);
		setSoundType(SoundType.STONE);
		setLightLevel(0.8F);
		setCreativeTab(ModCreativeTab.get());
		setHarvestLevel("pickaxe", 1);
	}

	@Override
	public boolean hasTileEntity(IBlockState state) {
		return true;
	}

	@Override
	public TileEntity createTileEntity(World world, IBlockState state) {
		return new GaiaBeaconTileEntity();
	}
}
