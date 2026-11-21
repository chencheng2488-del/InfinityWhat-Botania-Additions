package com.bandybus.infwhat.botania.block.entity;

import net.minecraft.world.World;
import vazkii.botania.common.block.tile.TileRuneAltar;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * Shortens the runic altar cooldown when an enabled Rune Accelerator is nearby.
 *
 * <p>1.7.10 Botania has no hook for this, so the package-private {@code cooldown} field is reduced
 * directly. Only one accelerator applies its multiplier per game tick; the strongest multiplier
 * among all accelerators targeting the same altar wins.
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
            throw new IllegalStateException("Botania 1.7.10 TileRuneAltar.cooldown is missing",
                    exception);
        }
    }

    public static void boost(World world, TileRuneAltar altar, int multiplier) {
        if (multiplier <= 1 || world == null || world.isRemote) {
            return;
        }
        long now = world.getTotalWorldTime();
        if (now != appliedTick) {
            APPLIED.clear();
            appliedTick = now;
        }
        String key = world.provider.dimensionId + "|" + altar.xCoord + "|"
                + altar.yCoord + "|" + altar.zCoord;
        Integer previousBoxed = APPLIED.get(key);
        int previous = previousBoxed == null ? 0 : previousBoxed;
        int effective = Math.max(previous, multiplier);
        if (effective <= previous) {
            return;
        }
        APPLIED.put(key, effective);
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
