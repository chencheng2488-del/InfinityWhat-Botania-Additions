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

    @Accessor(value = "cooldown", remap = false)
    int infwhat$getCooldown();

    @Accessor(value = "cooldown", remap = false)
    void infwhat$setCooldown(int value);

    @Accessor(value = "burnTime", remap = false)
    int infwhat$getBurnTime();

    @Accessor(value = "manaPerTick", remap = false)
    int infwhat$getManaPerTick();
}
