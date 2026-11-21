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

    // Botania's own members keep their source names, but the Minecraft call inside its method is
    // remapped to the intermediary name by Fabric. The annotation is therefore remap = false and
    // the INVOKE target below is written in intermediary form for 1.20.1:
    //   Level.getBlockEntity(BlockPos) -> method_8321, BlockPos -> class_2338,
    //   BlockEntity -> class_2586.
    @Redirect(method = "spawn", remap = false, require = 0,
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/class_1937;method_8321(Lnet/minecraft/class_2338;)Lnet/minecraft/class_2586;"))
    private static BlockEntity infwhat$fakeBeacon(Level level, BlockPos pos) {
        if (GaiaSummonHelper.isForced()) {
            return new BeaconBlockEntity(pos, Blocks.BEACON.defaultBlockState());
        }
        return level.getBlockEntity(pos);
    }

    @Inject(method = "checkPylons", at = @At("HEAD"), cancellable = true, remap = false,
            require = 0)
    private static void infwhat$skipPylonCheck(Level level, BlockPos pos,
                                               CallbackInfoReturnable<List<BlockPos>> cir) {
        if (GaiaSummonHelper.isForced()) {
            cir.setReturnValue(new ArrayList<>());
        }
    }

    @Inject(method = "checkArena", at = @At("HEAD"), cancellable = true, remap = false,
            require = 0)
    private static void infwhat$skipArenaCheck(Level level, BlockPos pos,
                                               CallbackInfoReturnable<List<BlockPos>> cir) {
        if (GaiaSummonHelper.isForced()) {
            cir.setReturnValue(new ArrayList<>());
        }
    }
}
