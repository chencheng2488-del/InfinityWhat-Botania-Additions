package com.bandybus.infwhat.botania.block.entity;

import com.bandybus.infwhat.botania.config.BotaniaAdditionConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import vazkii.botania.common.block.block_entity.TerrestrialAgglomerationPlateBlockEntity;

/** Feeds nearby Terrestrial Agglomeration Plates. */
public class TerrasteelAcceleratorBlockEntity extends AbstractAcceleratorBlockEntity {

    public TerrasteelAcceleratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TERRASTEEL_ACCELERATOR, pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state,
                                  TerrasteelAcceleratorBlockEntity accelerator) {
        if (level instanceof ServerLevel serverLevel) {
            accelerator.tickAccelerator(serverLevel);
        }
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
    protected Class<? extends BlockEntity> targetType() {
        return TerrestrialAgglomerationPlateBlockEntity.class;
    }

    @Override
    protected void accelerate(ServerLevel level, BlockEntity target) {
        if (!(target instanceof TerrestrialAgglomerationPlateBlockEntity plate)) {
            return;
        }
        if (!plate.canReceiveManaFromBursts()) {
            return;
        }
        int space = plate.getAvailableSpaceForMana();
        if (space <= 0) {
            return;
        }
        int amount = Math.min(effectiveRate(), space);
        if (amount > 0) {
            plate.receiveMana(amount);
        }
    }
}
