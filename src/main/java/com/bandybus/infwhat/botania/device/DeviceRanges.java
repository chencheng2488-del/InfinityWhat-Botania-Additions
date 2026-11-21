package com.bandybus.infwhat.botania.device;

import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

/** Scan boxes used by the mana well and the mana anchor. */
public final class DeviceRanges {

	public static final int MAX_REACH = 8;
	public static final int MAX_OFFSET = 8;

	private DeviceRanges() {
	}

	/**
	 * Inclusive box around {@code origin + offset}, expanded by the three reach values.
	 *
	 * <p>1.12.2's mutable cursor cannot be exposed as {@code Iterable<BlockPos>}, so the box is
	 * materialised once per scan. Scans run at most once per second per device.
	 */
	public static List<BlockPos> box(BlockPos origin, int reachX, int reachY, int reachZ,
									 int offsetX, int offsetY, int offsetZ) {
		BlockPos center = origin.add(offsetX, offsetY, offsetZ);
		int rx = Math.max(0, Math.min(MAX_REACH, reachX));
		int ry = Math.max(0, Math.min(MAX_REACH, reachY));
		int rz = Math.max(0, Math.min(MAX_REACH, reachZ));
		List<BlockPos> result = new ArrayList<BlockPos>();
		for (BlockPos pos : BlockPos.getAllInBoxMutable(center.add(-rx, -ry, -rz),
				center.add(rx, ry, rz))) {
			result.add(pos.toImmutable());
		}
		return result;
	}
}
