package com.bandybus.infwhat.botania.block.tile;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Server-side bookkeeping for the runic altar accelerator.
 *
 * <p>Several accelerators may cover the same altar. The cooldown shortening is applied once per
 * altar per tick using the strongest multiplier, which is the same rule the 1.20.1 mixin used.
 */
public final class RuneAcceleratorTracker {

	private static final Map<World, Set<BlockPos>> POSITIONS = new WeakHashMap<World, Set<BlockPos>>();
	private static final Map<World, Map<BlockPos, Long>> APPLIED =
			new WeakHashMap<World, Map<BlockPos, Long>>();

	public static void add(World world, BlockPos pos) {
		Set<BlockPos> positions = POSITIONS.get(world);
		if (positions == null) {
			positions = new HashSet<BlockPos>();
			POSITIONS.put(world, positions);
		}
		positions.add(pos.toImmutable());
	}

	public static void remove(World world, BlockPos pos) {
		Set<BlockPos> positions = POSITIONS.get(world);
		if (positions == null) {
			return;
		}
		positions.remove(pos);
		if (positions.isEmpty()) {
			POSITIONS.remove(world);
		}
	}

	/**
	 * Returns the strongest cooldown multiplier of an enabled accelerator whose own radius covers
	 * the altar, or 0 when none applies.
	 */
	public static int maxCooldownMultiplier(World world, BlockPos center) {
		Set<BlockPos> positions = POSITIONS.get(world);
		if (positions == null || positions.isEmpty()) {
			return 0;
		}
		int best = 0;
		for (BlockPos pos : positions) {
			if (!world.isBlockLoaded(pos)) {
				continue;
			}
			if (!(world.getTileEntity(pos) instanceof RuneAcceleratorTileEntity)) {
				continue;
			}
			RuneAcceleratorTileEntity accelerator =
					(RuneAcceleratorTileEntity) world.getTileEntity(pos);
			if (!accelerator.isEnabled() || !accelerator.isFeatureEnabled()) {
				continue;
			}
			int minX = pos.getX() + accelerator.offsetX() - accelerator.reachX();
			int minY = pos.getY() + accelerator.offsetY() - accelerator.reachY();
			int minZ = pos.getZ() + accelerator.offsetZ() - accelerator.reachZ();
			int maxX = pos.getX() + accelerator.offsetX() + accelerator.reachX();
			int maxY = pos.getY() + accelerator.offsetY() + accelerator.reachY();
			int maxZ = pos.getZ() + accelerator.offsetZ() + accelerator.reachZ();
			if (center.getX() < minX || center.getX() > maxX
					|| center.getY() < minY || center.getY() > maxY
					|| center.getZ() < minZ || center.getZ() > maxZ) {
				continue;
			}
			best = Math.max(best, accelerator.effectiveCooldownMultiplier());
		}
		return best;
	}

	/** True at most once per altar per world tick. */
	public static boolean tryApply(World world, BlockPos altar) {
		long now = world.getTotalWorldTime();
		Map<BlockPos, Long> applied = APPLIED.get(world);
		if (applied == null) {
			applied = new HashMap<BlockPos, Long>();
			APPLIED.put(world, applied);
		}
		Long last = applied.get(altar);
		if (last != null && last.longValue() == now) {
			return false;
		}
		if (applied.size() > 512) {
			applied.clear();
		}
		applied.put(altar.toImmutable(), now);
		return true;
	}

	private RuneAcceleratorTracker() {
	}
}
