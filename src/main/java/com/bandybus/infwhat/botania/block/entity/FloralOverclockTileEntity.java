package com.bandybus.infwhat.botania.block.entity;

import com.bandybus.infwhat.botania.config.BotaniaAdditionConfig;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.server.ServerWorld;
import vazkii.botania.api.subtile.TileEntityGeneratingFlower;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Runs the tick of nearby generating flowers extra times.
 *
 * <p>Only generating flowers are affected; functional flowers are left alone. Fuel, lifetime and
 * particle output scale with the multiplier, which is why the GUI and config cap it at 16.
 *
 * <p>Botania's {@code tickFlower()} advances {@code ticksExisted} itself, so each extra call is a
 * complete additional flower tick. This keeps passive generation, decay and burn timers
 * consistent with a genuinely faster game clock.
 */
public class FloralOverclockTileEntity extends AbstractAcceleratorTileEntity {

    private static final Map<Class<?>, Field> COOLDOWN_FIELDS = new HashMap<Class<?>, Field>();
    private static final Set<Class<?>> NO_COOLDOWN = new HashSet<Class<?>>();

    public FloralOverclockTileEntity() {
        super(ModBlockEntities.FLORAL_OVERCLOCK.get());
    }

    @Override
    protected boolean featureEnabled() {
        return BotaniaAdditionConfig.floralOverclockEnabled();
    }

    @Override
    protected int defaultRate() {
        return BotaniaAdditionConfig.floralOverclockMultiplier();
    }

    @Override
    protected int minRate() {
        return 1;
    }

    @Override
    protected int maxRate() {
        return 16;
    }

    @Override
    protected int defaultRadius() {
        return BotaniaAdditionConfig.floralOverclockRadius();
    }

    @Override
    protected int defaultCooldownMultiplier() {
        return BotaniaAdditionConfig.floralOverclockCooldownMultiplier();
    }

    @Override
    protected Class<? extends TileEntity> targetType() {
        return TileEntityGeneratingFlower.class;
    }

    @Override
    protected void accelerate(ServerWorld level, TileEntity target) {
        if (!(target instanceof TileEntityGeneratingFlower)) {
            return;
        }
        TileEntityGeneratingFlower flower = (TileEntityGeneratingFlower) target;
        int extraTicks = Math.max(0, effectiveRate() - 1);
        for (int index = 0; index < extraTicks; index++) {
            if (flower.isRemoved()) {
                return;
            }
            // TileEntitySpecialFlower.tickFlower() increments ticksExisted itself, so an extra
            // call is a complete additional flower tick. Do not increment the counter here.
            flower.tickFlower();
        }
        accelerateCooldown(flower);
    }

    /**
     * Shortens the cooldown of generating flowers that have one. The tick loop in those flowers
     * only removes one cooldown tick per call, so a very long cooldown barely reacts to the tick
     * multiplier.
     */
    private void accelerateCooldown(TileEntityGeneratingFlower flower) {
        int multiplier = effectiveCooldownMultiplier();
        if (multiplier <= 1) {
            return;
        }
        Field field = cooldownField(flower.getClass());
        if (field == null) {
            return;
        }
        try {
            int cooldown = field.getInt(flower);
            if (cooldown > 0) {
                field.setInt(flower, Math.max(0, cooldown / multiplier));
            }
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("Cannot shorten the flower cooldown", exception);
        }
    }

    private static Field cooldownField(Class<?> type) {
        Field cached = COOLDOWN_FIELDS.get(type);
        if (cached != null) {
            return cached;
        }
        if (NO_COOLDOWN.contains(type)) {
            return null;
        }
        for (Class<?> current = type; current != null && current != Object.class;
             current = current.getSuperclass()) {
            try {
                Field field = current.getDeclaredField("cooldown");
                if (field.getType() == int.class) {
                    field.setAccessible(true);
                    COOLDOWN_FIELDS.put(type, field);
                    return field;
                }
            } catch (NoSuchFieldException ignored) {
                // Keep walking up the hierarchy.
            }
        }
        NO_COOLDOWN.add(type);
        return null;
    }
}
