package com.bandybus.infwhat.botania.block;

import com.bandybus.infwhat.botania.block.tile.ManaBeaconTileEntity;
import com.bandybus.infwhat.botania.device.DeviceKind;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import java.util.List;

/** The beacon half of the wireless mana bridge. */
public class ManaBeaconBlock extends AbstractManaEndpointBlock {

	/** Matches the block model element by element, so the lamp has no invisible volume. */
	private static final AxisAlignedBB[] SHAPE = {
			ShapeUtil.box(0, 0, 0, 16, 3, 16),
			ShapeUtil.box(2, 3, 2, 14, 5, 14),
			ShapeUtil.box(6, 5, 6, 10, 12, 10),
			ShapeUtil.box(3, 12, 3, 13, 13, 13),
			ShapeUtil.box(5, 13, 5, 11, 16, 11),
			ShapeUtil.box(5, 7, 5, 11, 8, 11),
			ShapeUtil.box(5, 10, 5, 11, 11, 11),
			ShapeUtil.box(3, 13, 3, 5, 16, 5),
			ShapeUtil.box(11, 13, 3, 13, 16, 5),
			ShapeUtil.box(3, 13, 11, 5, 16, 13),
			ShapeUtil.box(11, 13, 11, 13, 16, 13)
	};

	public ManaBeaconBlock() {
		super(Material.ROCK);
		setLightLevel(0.8F);
	}

	@Override
	public DeviceKind kind() {
		return DeviceKind.BEACON;
	}

	@Override
	public TileEntity createTileEntity(World world, IBlockState state) {
		return new ManaBeaconTileEntity();
	}

	@Override
	public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
		return ShapeUtil.union(SHAPE);
	}

	@Override
	public void addCollisionBoxToList(IBlockState state, World world, BlockPos pos,
									  AxisAlignedBB entityBox, List<AxisAlignedBB> collidingBoxes,
									  Entity entity, boolean isActualState) {
		ShapeUtil.addCollision(collidingBoxes, entityBox, SHAPE, pos);
	}
}
