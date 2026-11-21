package com.bandybus.infwhat.botania.block.tile;

import com.bandybus.infwhat.botania.config.BotaniaAdditionConfig;
import com.bandybus.infwhat.botania.util.Reflect;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import vazkii.botania.common.block.tile.TileRuneAltar;

import java.lang.reflect.Field;

/**
 * Fills nearby runic altars and performs the wand step once the mana bar is full.
 *
 * <p>The altar still requires a livingrock item. Its cooldown is shortened through the reflected
 * {@code cooldown} field, which is the 1.12.2 equivalent of the 1.20.1 mixin. The multiplier is
 * stored per accelerator and can be changed in the GUI.
 */
public class RuneAcceleratorTileEntity extends AbstractAcceleratorTileEntity {

	private static final Field FIELD_COOLDOWN = Reflect.field(TileRuneAltar.class, "cooldown");

	private boolean trackerRegistered;

	public RuneAcceleratorTileEntity() {
	}

	@Override
	public void onLoad() {
		super.onLoad();
		ensureTracker();
	}

	/** Placement does not call {@link #onLoad()} in 1.12, so the first tick registers too. */
	private void ensureTracker() {
		if (trackerRegistered || world == null || world.isRemote) {
			return;
		}
		RuneAcceleratorTracker.add(world, pos);
		trackerRegistered = true;
	}

	@Override
	public void update() {
		ensureTracker();
		super.update();
	}

	@Override
	public void invalidate() {
		if (world != null && !world.isRemote) {
			RuneAcceleratorTracker.remove(world, pos);
			trackerRegistered = false;
		}
		super.invalidate();
	}

	@Override
	protected boolean featureEnabled() {
		return BotaniaAdditionConfig.runeAcceleratorEnabled();
	}

	@Override
	protected int defaultRate() {
		return BotaniaAdditionConfig.runeAcceleratorManaPerTick();
	}

	@Override
	protected int minRate() {
		return 0;
	}

	@Override
	protected int maxRate() {
		return Integer.MAX_VALUE;
	}

	@Override
	protected int defaultRadius() {
		return BotaniaAdditionConfig.runeAcceleratorRadius();
	}

	@Override
	protected int defaultCooldownMultiplier() {
		return BotaniaAdditionConfig.runeAcceleratorCooldownMultiplier();
	}

	@Override
	protected boolean accepts(TileEntity target) {
		return target instanceof TileRuneAltar;
	}

	@Override
	protected void accelerate(TileEntity target) {
		TileRuneAltar altar = (TileRuneAltar) target;
		int targetMana = altar.getTargetMana();
		if (targetMana > 0) {
			int current = altar.getCurrentMana();
			if (current < targetMana) {
				int amount = Math.min(effectiveRate(), targetMana - current);
				if (amount > 0) {
					altar.recieveMana(amount);
				}
			} else {
				altar.onWanded(null, ItemStack.EMPTY);
			}
		}
		applyCooldown(altar);
	}

	/**
	 * Shortens the altar's cooldown once per tick using the strongest nearby multiplier. Vanilla
	 * then removes its own single tick, so a multiplier of 3 finishes 60 ticks in 20.
	 */
	private void applyCooldown(TileRuneAltar altar) {
		if (world == null || FIELD_COOLDOWN == null
				|| !RuneAcceleratorTracker.tryApply(world, altar.getPos())) {
			return;
		}
		int multiplier = RuneAcceleratorTracker.maxCooldownMultiplier(world, altar.getPos());
		if (multiplier <= 1) {
			return;
		}
		int cooldown = Reflect.getInt(altar, FIELD_COOLDOWN, 0);
		if (cooldown > 0) {
			Reflect.setInt(altar, FIELD_COOLDOWN, Math.max(0, cooldown - (multiplier - 1)));
		}
	}
}
