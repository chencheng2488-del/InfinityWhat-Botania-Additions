package com.bandybus.infwhat.botania.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Server-side positions of loaded rune accelerators.
 *
 * <p>The mixin that shortens the runic altar cooldown runs inside Botania's block entity, so it
 * cannot use the accelerator's own scan cache. This tracker stores loaded positions and reads the
 * enabled state and per-block settings from the live block entity when the altar asks.
 */
public final class RuneAcceleratorTracker {

    private static final Map<ServerLevel, Set<BlockPos>> POSITIONS = new WeakHashMap<>();

    public static void add(ServerLevel level, BlockPos pos) {
        POSITIONS.computeIfAbsent(level, key -> new HashSet<>()).add(pos.immutable());
    }

    public static void remove(ServerLevel level, BlockPos pos) {
        Set<BlockPos> positions = POSITIONS.get(level);
        if (positions == null) {
            return;
        }
        positions.remove(pos);
        if (positions.isEmpty()) {
            POSITIONS.remove(level);
        }
    }

    /**
     * Returns the strongest cooldown multiplier of an enabled accelerator whose own radius covers
     * the altar, or 0 when none applies.
     */
    public static int maxCooldownMultiplier(ServerLevel level, BlockPos center) {
        Set<BlockPos> positions = POSITIONS.get(level);
        if (positions == null || positions.isEmpty()) {
            return 0;
        }
        int best = 0;
        for (BlockPos pos : positions) {
            if (!level.hasChunkAt(pos)) {
                continue;
            }
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (!(blockEntity instanceof RuneAcceleratorBlockEntity accelerator)
                    || !accelerator.isEnabled() || !accelerator.isFeatureEnabled()) {
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

    private RuneAcceleratorTracker() {
    }
}
