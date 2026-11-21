package com.bandybus.infwhat.botania.block.entity;

import com.bandybus.infwhat.botania.config.BotaniaAdditionConfig;
import net.minecraft.tileentity.TileEntity;
import vazkii.botania.api.subtile.SubTileEntity;
import vazkii.botania.api.subtile.SubTileGenerating;
import vazkii.botania.common.block.tile.TileSpecialFlower;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Runs the tick of nearby generating flowers extra times.
 *
 * <p>Botania 1.7.10 stores generating flowers as a {@link SubTileGenerating} inside a
 * {@link TileSpecialFlower}. {@code SubTileEntity.onUpdate()} increments {@code ticksExisted}
 * itself, so each extra call is a complete additional flower tick. Functional flowers are left
 * alone.
 */
public class FloralOverclockTileEntity extends AbstractAcceleratorTileEntity {

    private static final Map<Class<?>, Field> COOLDOWN_FIELDS = new HashMap<Class<?>, Field>();
    private static final Set<Class<?>> NO_COOLDOWN = new HashSet<Class<?>>();

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
        return TileSpecialFlower.class;
    }

    @Override
    protected void accelerate(TileEntity target) {
        if (!(target instanceof TileSpecialFlower)) {
            return;
        }
        SubTileEntity subTile = ((TileSpecialFlower) target).getSubTile();
        if (!(subTile instanceof SubTileGenerating)) {
            return;
        }
        int extraTicks = Math.max(0, effectiveRate() - 1);
        for (int index = 0; index < extraTicks; index++) {
            if (target.isInvalid()) {
                return;
            }
            // SubTileEntity.onUpdate() increments ticksExisted itself; do not increment here.
            subTile.onUpdate();
        }
        accelerateCooldown(subTile);
    }

    private void accelerateCooldown(SubTileEntity subTile) {
        int multiplier = effectiveCooldownMultiplier();
        if (multiplier <= 1) {
            return;
        }
        Field field = cooldownField(subTile.getClass());
        if (field == null) {
            return;
        }
        try {
            int cooldown = field.getInt(subTile);
            if (cooldown > 0) {
                field.setInt(subTile, Math.max(0, cooldown / multiplier));
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
