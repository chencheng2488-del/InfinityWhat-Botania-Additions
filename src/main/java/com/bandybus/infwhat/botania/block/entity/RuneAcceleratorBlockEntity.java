package com.bandybus.infwhat.botania.block.entity;

import com.bandybus.infwhat.botania.config.BotaniaAdditionConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import vazkii.botania.common.block.tile.TileRuneAltar;

/**
 * Fills nearby runic altars and performs the wand step once the mana bar is full.
 *
 * <p>The altar still requires a livingrock item. The cooldown is shortened by
 * {@link RuneAcceleratorTracker}; the multiplier is stored per accelerator and can be changed in
 * the GUI.
 */
public class RuneAcceleratorBlockEntity extends AbstractAcceleratorBlockEntity {

    public RuneAcceleratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RUNE_ACCELERATOR.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state,
                                  RuneAcceleratorBlockEntity accelerator) {
        if (level instanceof ServerLevel serverLevel) {
            accelerator.tickAccelerator(serverLevel);
        }
    }

    @Override
    protected boolean featureEnabled() {
        return BotaniaAdditionConfig.runeAcceleratorEnabled();
    }

    @Override
    protected int defaultRate() {
        return BotaniaAdditionConfig.runeAcceleratorManaPerTick();
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
        return BotaniaAdditionConfig.runeAcceleratorRadius();
    }

    @Override
    protected Class<? extends BlockEntity> targetType() {
        return TileRuneAltar.class;
    }

    @Override
    protected void accelerate(ServerLevel level, BlockEntity target) {
        if (!(target instanceof TileRuneAltar altar)) {
            return;
        }
        RuneAcceleratorTracker.boost(level, altar, effectiveCooldownMultiplier());
        int targetMana = altar.getTargetMana();
        if (targetMana <= 0) {
            return;
        }
        int current = altar.getCurrentMana();
        if (current < targetMana) {
            int amount = Math.min(effectiveRate(), targetMana - current);
            if (amount > 0) {
                altar.receiveMana(amount);
            }
            return;
        }
        altar.onUsedByWand(null, ItemStack.EMPTY, net.minecraft.core.Direction.UP);
    }

    @Override
    protected int defaultCooldownMultiplier() {
        return BotaniaAdditionConfig.runeAcceleratorCooldownMultiplier();
    }
}
