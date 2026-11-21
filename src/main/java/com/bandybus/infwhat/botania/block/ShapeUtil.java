package com.bandybus.infwhat.botania.block;

import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

import java.util.List;

/** Converts pixel-space model boxes into 1.12 collision and selection boxes. */
public final class ShapeUtil {

	public static AxisAlignedBB box(int x1, int y1, int z1, int x2, int y2, int z2) {
		return new AxisAlignedBB(x1 / 16.0D, y1 / 16.0D, z1 / 16.0D,
				x2 / 16.0D, y2 / 16.0D, z2 / 16.0D);
	}

	/** Union of all parts, used only for the block outline. */
	public static AxisAlignedBB union(AxisAlignedBB[] boxes) {
		double minX = 1.0D;
		double minY = 1.0D;
		double minZ = 1.0D;
		double maxX = 0.0D;
		double maxY = 0.0D;
		double maxZ = 0.0D;
		for (AxisAlignedBB box : boxes) {
			minX = Math.min(minX, box.minX);
			minY = Math.min(minY, box.minY);
			minZ = Math.min(minZ, box.minZ);
			maxX = Math.max(maxX, box.maxX);
			maxY = Math.max(maxY, box.maxY);
			maxZ = Math.max(maxZ, box.maxZ);
		}
		return new AxisAlignedBB(minX, minY, minZ, maxX, maxY, maxZ);
	}

	public static void addCollision(List<AxisAlignedBB> list, AxisAlignedBB entityBox,
									AxisAlignedBB[] boxes, BlockPos pos) {
		for (AxisAlignedBB box : boxes) {
			AxisAlignedBB worldBox = box.offset(pos.getX(), pos.getY(), pos.getZ());
			// 1.12 asks the block for every part the entity could touch. Adding parts on the far
			// side of the block would create invisible collisions away from the model.
			if (entityBox.intersects(worldBox)) {
				list.add(worldBox);
			}
		}
	}

	private ShapeUtil() {
	}
}
