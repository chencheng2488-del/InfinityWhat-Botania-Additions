package com.bandybus.infwhat.botania.block.entity;

import com.bandybus.infwhat.botania.config.BotaniaAdditionConfig;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.server.ServerWorld;
import vazkii.botania.common.block.tile.TileRuneAltar;

/**
 * Fills nearby runic altars and performs the wand step once the mana bar is full.
 *
 * <p>The altar still requires a livingrock item. The cooldown is shortened by
 * {@link RuneAcceleratorTracker}; the multiplier is stored per accelerator and can be changed in
 * the GUI.
 */
public class RuneAcceleratorTileEntity extends AbstractAcceleratorTileEntity {

    public RuneAcceleratorTileEntity() {
        super(ModBlockEntities.RUNE_ACCELERATOR.get());
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
    protected Class<? extends TileEntity> targetType() {
        return TileRuneAltar.class;
    }

    @Override
    protected void accelerate(ServerWorld level, TileEntity target) {
        if (!(target instanceof TileRuneAltar)) {
            return;
        }
        TileRuneAltar altar = (TileRuneAltar) target;
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
        altar.onWanded(null, ItemStack.EMPTY);
    }

    @Override
    protected int defaultCooldownMultiplier() {
        return BotaniaAdditionConfig.runeAcceleratorCooldownMultiplier();
    }
}
