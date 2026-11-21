package com.bandybus.infwhat.botania.block.entity;

import com.bandybus.infwhat.botania.config.BotaniaAdditionConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
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
public class FloralOverclockBlockEntity extends AbstractAcceleratorBlockEntity {

    private static final Map<Class<?>, Field> COOLDOWN_FIELDS = new HashMap<>();
    private static final Set<Class<?>> NO_COOLDOWN = new HashSet<>();

    public FloralOverclockBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FLORAL_OVERCLOCK.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state,
                                  FloralOverclockBlockEntity accelerator) {
        if (level instanceof ServerLevel serverLevel) {
            accelerator.tickAccelerator(serverLevel);
        }
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
    protected Class<? extends BlockEntity> targetType() {
        return TileEntityGeneratingFlower.class;
    }

    @Override
    protected void accelerate(ServerLevel level, BlockEntity target) {
        if (!(target instanceof TileEntityGeneratingFlower flower)) {
            return;
        }
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
