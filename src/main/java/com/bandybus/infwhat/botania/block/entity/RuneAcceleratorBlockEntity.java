package com.bandybus.infwhat.botania.block.entity;

import com.bandybus.infwhat.botania.config.BotaniaAdditionConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import vazkii.botania.common.block.block_entity.RunicAltarBlockEntity;

/**
 * Fills nearby runic altars and performs the wand step once the mana bar is full.
 *
 * <p>The altar still requires a livingrock item. Its cooldown is shortened by the mixin in
 * {@link com.bandybus.infwhat.botania.mixin.RunicAltarBlockEntityMixin}; the multiplier is stored
 * per accelerator and can be changed in the GUI.
 */
public class RuneAcceleratorBlockEntity extends AbstractAcceleratorBlockEntity {

    public RuneAcceleratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RUNE_ACCELERATOR.get(), pos, state);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel serverLevel) {
            RuneAcceleratorTracker.add(serverLevel, worldPosition);
        }
    }

    @Override
    public void setRemoved() {
        if (level instanceof ServerLevel serverLevel) {
            RuneAcceleratorTracker.remove(serverLevel, worldPosition);
        }
        super.setRemoved();
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
        return RunicAltarBlockEntity.class;
    }

    @Override
    protected void accelerate(ServerLevel level, BlockEntity target) {
        if (!(target instanceof RunicAltarBlockEntity altar)) {
            return;
        }
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
        altar.onUsedByWand(null, ItemStack.EMPTY, Direction.UP);
    }

    @Override
    protected int defaultCooldownMultiplier() {
        return BotaniaAdditionConfig.runeAcceleratorCooldownMultiplier();
    }
}
