package com.bandybus.infwhat.botania.mixin;

import com.bandybus.infwhat.botania.util.GaiaSummonHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import vazkii.botania.common.entity.GaiaGuardianEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * Relaxes Botania's structure validation for the Gaia Charm.
 *
 * <p>Botania's spawn method insists on a beacon below, four Gaia Pylons around it and a clear
 * arena. The charm only sets {@link GaiaSummonHelper#isForced()} while it calls that method, so
 * these hooks are inert for the normal ritual.
 */
@Mixin(GaiaGuardianEntity.class)
public abstract class GaiaGuardianEntityMixin {

    // This add-on does not ship a mixin refmap, so Minecraft members must be referenced by their
    // SRG name (Level#m_7702_ is Level#getBlockEntity in 1.20.1). Botania's own members keep
    // their source names in production jars.
    @Redirect(method = "spawn",
            at = @At(value = "INVOKE", remap = false,
                    target = "Lnet/minecraft/world/level/Level;m_7702_(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/entity/BlockEntity;"))
    private static BlockEntity infwhat$fakeBeacon(Level level, BlockPos pos) {
        if (GaiaSummonHelper.isForced()) {
            return new BeaconBlockEntity(pos, Blocks.BEACON.defaultBlockState());
        }
        return level.getBlockEntity(pos);
    }

    @Inject(method = "checkPylons", at = @At("HEAD"), cancellable = true)
    private static void infwhat$skipPylonCheck(Level level, BlockPos pos,
                                               CallbackInfoReturnable<List<BlockPos>> cir) {
        if (GaiaSummonHelper.isForced()) {
            cir.setReturnValue(new ArrayList<>());
        }
    }

    @Inject(method = "checkArena", at = @At("HEAD"), cancellable = true)
    private static void infwhat$skipArenaCheck(Level level, BlockPos pos,
                                               CallbackInfoReturnable<List<BlockPos>> cir) {
        if (GaiaSummonHelper.isForced()) {
            cir.setReturnValue(new ArrayList<>());
        }
    }
}
