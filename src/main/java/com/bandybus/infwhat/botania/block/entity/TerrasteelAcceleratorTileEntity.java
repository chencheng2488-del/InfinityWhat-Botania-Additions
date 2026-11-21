package com.bandybus.infwhat.botania.block.entity;

import com.bandybus.infwhat.botania.config.BotaniaAdditionConfig;
import net.minecraft.tileentity.TileEntity;
import vazkii.botania.common.block.tile.TileTerraPlate;

/** Feeds nearby Terrestrial Agglomeration Plates. */
public class TerrasteelAcceleratorTileEntity extends AbstractAcceleratorTileEntity {

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
    protected Class<? extends TileEntity> targetType() {
        return TileTerraPlate.class;
    }

    @Override
    protected void accelerate(TileEntity target) {
        if (!(target instanceof TileTerraPlate)) {
            return;
        }
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
