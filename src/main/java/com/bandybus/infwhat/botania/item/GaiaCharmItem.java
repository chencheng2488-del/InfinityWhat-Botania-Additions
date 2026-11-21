package com.bandybus.infwhat.botania.item;

import com.bandybus.infwhat.botania.config.BotaniaAdditionConfig;
import com.bandybus.infwhat.botania.util.GaiaSummonHelper;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Summons the Gaia Guardian without building Botania's beacon and Gaia Pylon structure.
 *
 * <p>The boss is still created and scaled by Botania's own setup code, so health scaling, boss
 * bar, music, drops and the hard mode behaviour stay identical. Only the structure validation is
 * skipped, through {@link GaiaSummonHelper}.
 */
public class GaiaCharmItem extends Item {

    public GaiaCharmItem(Properties properties) {
        super(properties);
    }

    @Override
    public ActionResult<ItemStack> use(World level, PlayerEntity player, Hand hand) {
        ItemStack charm = player.getItemInHand(hand);
        return new ActionResult<ItemStack>(summon(level, player, charm), charm);
    }

    @Override
    public ActionResultType useOn(ItemUseContext context) {
        PlayerEntity player = context.getPlayer();
        if (player == null) {
            return ActionResultType.PASS;
        }
        return summon(context.getLevel(), player, player.getItemInHand(context.getHand()));
    }

    private static ActionResultType summon(World level, PlayerEntity player, ItemStack charm) {
        if (level.isClientSide) {
            return ActionResultType.SUCCESS;
        }
        if (!BotaniaAdditionConfig.gaiaCharmEnabled()) {
            player.displayClientMessage(new TranslationTextComponent(
                    "message.infwhat_botania.gaia_charm.disabled"), true);
            return ActionResultType.CONSUME;
        }

        boolean hardMode = BotaniaAdditionConfig.gaiaCharmHardMode();
        if (!GaiaSummonHelper.summon(player, level, player.blockPosition(), hardMode)) {
            player.displayClientMessage(new TranslationTextComponent(
                    "message.infwhat_botania.gaia_charm.failed"), true);
            return ActionResultType.CONSUME;
        }

        if (BotaniaAdditionConfig.gaiaCharmConsumes() && !player.isCreative()) {
            charm.shrink(1);
        }
        player.displayClientMessage(new TranslationTextComponent(hardMode
                ? "message.infwhat_botania.gaia_charm.summoned_hard"
                : "message.infwhat_botania.gaia_charm.summoned"), true);
        return ActionResultType.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable World level, List<ITextComponent> tooltip,
                                ITooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        if (!BotaniaAdditionConfig.gaiaCharmEnabled()) {
            tooltip.add(new TranslationTextComponent("tooltip.infwhat_botania.gaia_charm.disabled")
                    .withStyle(TextFormatting.RED));
        }
        tooltip.add(new TranslationTextComponent("tooltip.infwhat_botania.gaia_charm.usage")
                .withStyle(TextFormatting.GRAY));
        tooltip.add(new TranslationTextComponent(BotaniaAdditionConfig.gaiaCharmHardMode()
                        ? "tooltip.infwhat_botania.gaia_charm.hard"
                        : "tooltip.infwhat_botania.gaia_charm.normal")
                .withStyle(TextFormatting.DARK_PURPLE));
        tooltip.add(new TranslationTextComponent(BotaniaAdditionConfig.gaiaCharmConsumes()
                        ? "tooltip.infwhat_botania.gaia_charm.consumed"
                        : "tooltip.infwhat_botania.gaia_charm.reusable")
                .withStyle(TextFormatting.DARK_GRAY));
    }
}
