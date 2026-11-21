package com.bandybus.infwhat.botania.block.entity;

import com.bandybus.infwhat.botania.config.BotaniaAdditionConfig;
import net.minecraft.tileentity.TileEntity;
import vazkii.botania.common.block.tile.TileRuneAltar;

/**
 * Fills nearby runic altars and performs the wand step once the mana bar is full.
 *
 * <p>The altar still requires a livingrock item. The crafting cooldown is shortened by
 * {@link RuneAcceleratorTracker}; the multiplier is stored per accelerator and can be changed in
 * the GUI.
 */
public class RuneAcceleratorTileEntity extends AbstractAcceleratorTileEntity {

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
    protected int defaultCooldownMultiplier() {
        return BotaniaAdditionConfig.runeAcceleratorCooldownMultiplier();
    }

    @Override
    protected Class<? extends TileEntity> targetType() {
        return TileRuneAltar.class;
    }

    @Override
    protected void accelerate(TileEntity target) {
        if (!(target instanceof TileRuneAltar)) {
            return;
        }
        TileRuneAltar altar = (TileRuneAltar) target;
        RuneAcceleratorTracker.boost(worldObj, altar, effectiveCooldownMultiplier());
        int targetMana = altar.getTargetMana();
        if (targetMana <= 0) {
            return;
        }
        int current = altar.getCurrentMana();
        if (current < targetMana) {
            int amount = Math.min(effectiveRate(), targetMana - current);
            if (amount > 0) {
                altar.recieveMana(amount);
            }
            return;
        }
        // 1.7.10 Botania ignores the player and the wand stack in this routine.
        altar.onWanded(null, null);
    }
}
