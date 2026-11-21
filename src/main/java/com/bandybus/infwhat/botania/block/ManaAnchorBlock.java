package com.bandybus.infwhat.botania.block;

import com.bandybus.infwhat.botania.block.tile.ManaAnchorTileEntity;
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

/** The receiving half of the wireless mana bridge. */
public class ManaAnchorBlock extends AbstractManaEndpointBlock {

	/** Matches the block model element by element, so the rune plate has no invisible volume. */
	private static final AxisAlignedBB[] SHAPE = {
			ShapeUtil.box(0, 0, 0, 16, 3, 16),
			ShapeUtil.box(1, 3, 1, 15, 4, 15),
			ShapeUtil.box(7, 4, 7, 9, 7, 9),
			ShapeUtil.box(1, 4, 1, 3, 9, 3),
			ShapeUtil.box(13, 4, 1, 15, 9, 3),
			ShapeUtil.box(1, 4, 13, 3, 9, 15),
			ShapeUtil.box(13, 4, 13, 15, 9, 15),
			ShapeUtil.box(5, 4, 4, 11, 5, 6),
			ShapeUtil.box(5, 4, 10, 11, 5, 12),
			ShapeUtil.box(4, 4, 6, 6, 5, 10),
			ShapeUtil.box(10, 4, 6, 12, 5, 10)
	};

	public ManaAnchorBlock() {
		super(Material.ROCK);
		setLightLevel(0.55F);
	}

	@Override
	public DeviceKind kind() {
		return DeviceKind.ANCHOR;
	}

	@Override
	public TileEntity createTileEntity(World world, IBlockState state) {
		return new ManaAnchorTileEntity();
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
