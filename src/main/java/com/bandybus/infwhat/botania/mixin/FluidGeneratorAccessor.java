package com.bandybus.infwhat.botania.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import vazkii.botania.common.block.flower.generating.FluidGeneratorBlockEntity;

/**
 * Exposes the cooldown counter of fluid generating flowers (Thermalily, Hydroangeas) so the
 * Floral Overclock can shorten it directly.
 */
@Mixin(FluidGeneratorBlockEntity.class)
public interface FluidGeneratorAccessor {

    @Accessor("cooldown")
    int infwhat$getCooldown();

    @Accessor("cooldown")
    void infwhat$setCooldown(int value);

    @Accessor("burnTime")
    int infwhat$getBurnTime();

    @Accessor("manaPerTick")
    int infwhat$getManaPerTick();
}
