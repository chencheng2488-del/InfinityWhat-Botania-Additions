package com.bandybus.infwhat.botania.block.tile;

import com.bandybus.infwhat.botania.config.BotaniaAdditionConfig;
import net.minecraft.tileentity.TileEntity;
import vazkii.botania.common.block.tile.TileTerraPlate;

/** Feeds nearby Terrestrial Agglomeration Plates. */
public class TerrasteelAcceleratorTileEntity extends AbstractAcceleratorTileEntity {

	public TerrasteelAcceleratorTileEntity() {
	}

	@Override
	protected boolean featureEnabled() {
		return BotaniaAdditionConfig.terrasteelAcceleratorEnabled();
	}

	@Override
	protected int defaultRate() {
		return BotaniaAdditionConfig.terrasteelAcceleratorManaPerTick();
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
		return BotaniaAdditionConfig.terrasteelAcceleratorRadius();
	}

	@Override
	protected boolean accepts(TileEntity target) {
		return target instanceof TileTerraPlate;
	}

	@Override
	protected void accelerate(TileEntity target) {
		TileTerraPlate plate = (TileTerraPlate) target;
		if (!plate.canRecieveManaFromBursts()) {
			return;
		}
		int space = plate.getAvailableSpaceForMana();
		if (space <= 0) {
			return;
		}
		int amount = Math.min(effectiveRate(), space);
		if (amount > 0) {
			plate.recieveMana(amount);
		}
	}
}
