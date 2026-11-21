package com.bandybus.infwhat.botania.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import vazkii.botania.common.block.flower.generating.MunchdewBlockEntity;

/** Exposes Munchdew's cooldown between leaf meals for the Floral Overclock. */
@Mixin(MunchdewBlockEntity.class)
public interface MunchdewAccessor {

    @Accessor(value = "cooldown", remap = false)
    int infwhat$getCooldown();

    @Accessor(value = "cooldown", remap = false)
    void infwhat$setCooldown(int value);
}
