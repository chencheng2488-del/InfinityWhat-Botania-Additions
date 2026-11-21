package com.bandybus.infwhat.botania.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import vazkii.botania.common.block.flower.generating.GourmaryllisBlockEntity;

/** Exposes Gourmaryllis' digestion cooldown for the Floral Overclock. */
@Mixin(GourmaryllisBlockEntity.class)
public interface GourmaryllisAccessor {

    @Accessor("cooldown")
    int infwhat$getCooldown();

    @Accessor("cooldown")
    void infwhat$setCooldown(int value);
}
