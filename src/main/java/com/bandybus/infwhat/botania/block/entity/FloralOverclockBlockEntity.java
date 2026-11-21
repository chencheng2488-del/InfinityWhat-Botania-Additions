package com.bandybus.infwhat.botania.block.entity;

import com.bandybus.infwhat.botania.config.BotaniaAdditionConfig;
import com.bandybus.infwhat.botania.mixin.FluidGeneratorAccessor;
import com.bandybus.infwhat.botania.mixin.GourmaryllisAccessor;
import com.bandybus.infwhat.botania.mixin.MunchdewAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import vazkii.botania.api.block_entity.GeneratingFlowerBlockEntity;
import vazkii.botania.common.block.flower.generating.FluidGeneratorBlockEntity;

import java.util.function.IntConsumer;

/**
 * Runs the tick of nearby generating flowers extra times.
 *
 * <p>Only generating flowers are affected; functional flowers are left alone. Fuel, lifetime and
 * particle output scale with the multiplier, which is why the GUI and config cap it at 5.
 */
public class FloralOverclockBlockEntity extends AbstractAcceleratorBlockEntity {

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
        return GeneratingFlowerBlockEntity.class;
    }

    @Override
    protected void accelerate(ServerLevel level, BlockEntity target) {
        if (!(target instanceof GeneratingFlowerBlockEntity flower)) {
            return;
        }
        int extraTicks = Math.max(0, effectiveRate() - 1);
        if (flower instanceof FluidGeneratorBlockEntity fluidFlower) {
            accelerateFluidTicks(flower, fluidFlower, extraTicks);
        } else {
            for (int index = 0; index < extraTicks; index++) {
                if (flower.isRemoved()) {
                    return;
                }
                flower.tickFlower();
            }
        }
        accelerateCooldown(flower);
    }

    /**
     * Extra ticks shorten the burn phase, but mana is only generated on every
     * {@code generationDelay}-th real tick. Skipped generation ticks are compensated here so one
     * bucket of lava still yields the full amount.
     */
    private static void accelerateFluidTicks(GeneratingFlowerBlockEntity flower,
                                             FluidGeneratorBlockEntity fluidFlower,
                                             int extraTicks) {
        if (extraTicks <= 0) {
            return;
        }
        FluidGeneratorAccessor accessor = (FluidGeneratorAccessor) flower;
        int delay = Math.max(1, fluidFlower.getGenerationDelay());
        int manaPerTick = accessor.infwhat$getManaPerTick();
        for (int index = 1; index <= extraTicks; index++) {
            if (flower.isRemoved()) {
                return;
            }
            int before = accessor.infwhat$getBurnTime();
            flower.tickFlower();
            int after = accessor.infwhat$getBurnTime();
            if (before > 0 && after == before - 1
                    && (flower.ticksExisted + index) % delay == 0) {
                flower.addMana(manaPerTick);
            }
        }
    }

    /**
     * Shortens the cooldown of generating flowers that have one. The tick loop in those flowers
     * only removes one cooldown tick per call, so a very long cooldown barely reacts to the tick
     * multiplier.
     */
    private void accelerateCooldown(GeneratingFlowerBlockEntity flower) {
        int multiplier = effectiveCooldownMultiplier();
        if (multiplier <= 1) {
            return;
        }
        if (flower instanceof FluidGeneratorAccessor accessor) {
            reduceCooldown(accessor.infwhat$getCooldown(), accessor::infwhat$setCooldown, multiplier);
        } else if (flower instanceof GourmaryllisAccessor accessor) {
            reduceCooldown(accessor.infwhat$getCooldown(), accessor::infwhat$setCooldown, multiplier);
        } else if (flower instanceof MunchdewAccessor accessor) {
            reduceCooldown(accessor.infwhat$getCooldown(), accessor::infwhat$setCooldown, multiplier);
        }
    }

    private static void reduceCooldown(int cooldown, IntConsumer setter, int multiplier) {
        if (cooldown <= 0) {
            return;
        }
        // Exponential decay: the remaining cooldown is divided by the multiplier every tick, so
        // a multiplier of 3 turns 6000 ticks into roughly 8 game ticks and a huge value zeroes it
        // immediately.
        setter.accept(Math.max(0, cooldown / multiplier));
    }
}
