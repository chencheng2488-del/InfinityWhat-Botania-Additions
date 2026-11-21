package com.bandybus.infwhat.botania.block.entity;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.server.ServerWorld;
import vazkii.botania.common.block.tile.TileRuneAltar;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Shortens the runic altar cooldown when an enabled Rune Accelerator is nearby.
 *
 * <p>Botania 1.16.5 has no hook for this, so the private {@code cooldown} field is reduced by the
 * extra ticks directly. Only one accelerator applies its multiplier per game tick; the strongest
 * multiplier among all accelerators targeting the same altar wins, matching the 1.20.1 behaviour.
 */
public final class RuneAcceleratorTracker {

    private static final Map<String, Integer> APPLIED = new HashMap<String, Integer>();
    private static long appliedTick = Long.MIN_VALUE;

    private static final Field COOLDOWN = findCooldown();

    private RuneAcceleratorTracker() {
    }

    private static Field findCooldown() {
        try {
            Field field = TileRuneAltar.class.getDeclaredField("cooldown");
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Botania 1.16.5 TileRuneAltar.cooldown is missing", exception);
        }
    }

    public static void boost(ServerWorld level, TileRuneAltar altar, int multiplier) {
        if (multiplier <= 1) {
            return;
        }
        long now = level.getGameTime();
        if (now != appliedTick) {
            APPLIED.clear();
            appliedTick = now;
        }
        BlockPos pos = altar.getBlockPos();
        String key = level.dimension().location() + "|" + pos.asLong();
        Integer previousBoxed = APPLIED.get(key);
        int previous = previousBoxed == null ? 0 : previousBoxed.intValue();
        int effective = Math.max(previous, multiplier);
        if (effective <= previous) {
            return;
        }
        APPLIED.put(key, Integer.valueOf(effective));
        int alreadyExtra = previous <= 0 ? 0 : previous - 1;
        int extra = (effective - 1) - alreadyExtra;
        if (extra <= 0) {
            return;
        }
        try {
            int cooldown = COOLDOWN.getInt(altar);
            if (cooldown <= 0) {
                return;
            }
            COOLDOWN.setInt(altar, Math.max(0, cooldown - extra));
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("Cannot shorten the runic altar cooldown", exception);
        }
    }
}
