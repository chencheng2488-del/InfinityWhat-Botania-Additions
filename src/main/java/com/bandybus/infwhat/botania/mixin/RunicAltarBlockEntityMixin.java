package com.bandybus.infwhat.botania.mixin;

import com.bandybus.infwhat.botania.block.entity.RuneAcceleratorTracker;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vazkii.botania.common.block.block_entity.RunicAltarBlockEntity;

/**
 * Shortens the runic altar cooldown when an enabled Rune Accelerator is nearby.
 *
 * <p>The vanilla method decrements {@code cooldown} once per tick. This mixin runs first and
 * removes the extra ticks, leaving the original decrement in place, so a multiplier of 3 makes
 * the 60 tick cooldown finish in 20 ticks.
 */
@Mixin(RunicAltarBlockEntity.class)
public abstract class RunicAltarBlockEntityMixin {

    // Botania's own members keep their source names in the Fabric jar, so they must not be
    // looked up in the Minecraft mapping tree.
    @Shadow(remap = false)
    private int cooldown;

    @Inject(method = "tickCooldown", at = @At("HEAD"), remap = false, require = 0)
    private void infwhat$accelerateCooldown(CallbackInfo ci) {
        if (cooldown <= 0) {
            return;
        }
        BlockEntity self = (BlockEntity) (Object) this;
        if (!(self.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }
        int multiplier = RuneAcceleratorTracker.maxCooldownMultiplier(serverLevel,
                self.getBlockPos());
        if (multiplier <= 1) {
            return;
        }
        cooldown = Math.max(0, cooldown - (multiplier - 1));
    }
}
