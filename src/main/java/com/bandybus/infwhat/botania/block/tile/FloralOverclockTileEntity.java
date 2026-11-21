package com.bandybus.infwhat.botania.block.tile;

import com.bandybus.infwhat.botania.config.BotaniaAdditionConfig;
import com.bandybus.infwhat.botania.util.Reflect;
import net.minecraft.tileentity.TileEntity;
import vazkii.botania.api.subtile.SubTileGenerating;
import vazkii.botania.common.block.subtile.generating.SubTileGourmaryllis;
import vazkii.botania.common.block.subtile.generating.SubTileHydroangeas;
import vazkii.botania.common.block.subtile.generating.SubTileMunchdew;
import vazkii.botania.common.block.tile.TileSpecialFlower;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * Runs the tick of nearby generating flowers extra times.
 *
 * <p>Only generating flowers are affected; functional flowers are left alone. Fuel, lifetime and
 * particle output scale with the multiplier, which is why the GUI and config cap it at 16.
 */
public class FloralOverclockTileEntity extends AbstractAcceleratorTileEntity {

	private static final Map<Class<?>, Field> COOLDOWN_FIELDS =
			new HashMap<Class<?>, Field>();

	public FloralOverclockTileEntity() {
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
	protected boolean accepts(TileEntity target) {
		if (!(target instanceof TileSpecialFlower)) {
			return false;
		}
		return ((TileSpecialFlower) target).getSubTile() instanceof SubTileGenerating;
	}

	@Override
	protected void accelerate(TileEntity target) {
		TileSpecialFlower special = (TileSpecialFlower) target;
		if (!(special.getSubTile() instanceof SubTileGenerating)) {
			return;
		}
		SubTileGenerating flower = (SubTileGenerating) special.getSubTile();
		int extraTicks = Math.max(0, effectiveRate() - 1);
		for (int index = 0; index < extraTicks; index++) {
			if (special.isInvalid()) {
				return;
			}
			flower.onUpdate();
		}
		accelerateCooldown(flower);
	}

	/**
	 * Shortens the cooldown of generating flowers that have one. The extra tick loop only removes
	 * one cooldown tick per call, so a very long cooldown barely reacts to the multiplier without
	 * this additional division.
	 */
	private void accelerateCooldown(SubTileGenerating flower) {
		int multiplier = effectiveCooldownMultiplier();
		if (multiplier <= 1) {
			return;
		}
		Field field = cooldownField(flower.getClass());
		if (field == null) {
			return;
		}
		int cooldown = Reflect.getInt(flower, field, -1);
		if (cooldown <= 0) {
			return;
		}
		// Exponential decay: the remaining cooldown is divided by the multiplier every tick.
		Reflect.setInt(flower, field, Math.max(0, cooldown / multiplier));
	}

	private static Field cooldownField(Class<?> flowerClass) {
		if (!hasCooldown(flowerClass)) {
			return null;
		}
		Field cached = COOLDOWN_FIELDS.get(flowerClass);
		if (cached != null) {
			return cached;
		}
		Class<?> current = flowerClass;
		while (current != null && current != Object.class) {
			try {
				Field field = current.getDeclaredField("cooldown");
				field.setAccessible(true);
				COOLDOWN_FIELDS.put(flowerClass, field);
				return field;
			} catch (NoSuchFieldException exception) {
				current = current.getSuperclass();
			} catch (SecurityException exception) {
				return null;
			}
		}
		return null;
	}

	private static boolean hasCooldown(Class<?> flowerClass) {
		return SubTileHydroangeas.class.isAssignableFrom(flowerClass)
				|| SubTileGourmaryllis.class.isAssignableFrom(flowerClass)
				|| SubTileMunchdew.class.isAssignableFrom(flowerClass);
	}
}
