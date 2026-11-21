package com.bandybus.infwhat.botania.item;

import com.bandybus.infwhat.botania.config.BotaniaAdditionConfig;
import com.bandybus.infwhat.botania.util.GaiaSummonHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Summons the Gaia Guardian without building Botania's beacon and Gaia Pylon structure.
 *
 * <p>The boss is still created by Botania's own spawn routine, so health scaling, boss bar,
 * music, drops and the hard mode behaviour stay identical. Only the structure validation is
 * skipped, through {@link GaiaSummonHelper}.
 */
public class GaiaCharmItem extends Item {

    public GaiaCharmItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack charm = player.getItemInHand(hand);
        return new InteractionResultHolder<>(summon(level, player, charm), charm);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        return summon(context.getLevel(), player, player.getItemInHand(context.getHand()));
    }

    private static InteractionResult summon(Level level, Player player, ItemStack charm) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!BotaniaAdditionConfig.gaiaCharmEnabled()) {
            player.displayClientMessage(Component.translatable(
                    "message.infwhat_botania.gaia_charm.disabled"), true);
            return InteractionResult.CONSUME;
        }

        boolean hardMode = BotaniaAdditionConfig.gaiaCharmHardMode();
        if (!GaiaSummonHelper.summon(player, level, player.blockPosition(), hardMode)) {
            player.displayClientMessage(Component.translatable(
                    "message.infwhat_botania.gaia_charm.failed"), true);
            return InteractionResult.CONSUME;
        }

        if (BotaniaAdditionConfig.gaiaCharmConsumes() && !player.getAbilities().instabuild) {
            charm.shrink(1);
        }
        player.displayClientMessage(Component.translatable(hardMode
                ? "message.infwhat_botania.gaia_charm.summoned_hard"
                : "message.infwhat_botania.gaia_charm.summoned"), true);
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip,
                                TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        if (!BotaniaAdditionConfig.gaiaCharmEnabled()) {
            tooltip.add(Component.translatable("tooltip.infwhat_botania.gaia_charm.disabled")
                    .withStyle(ChatFormatting.RED));
        }
        tooltip.add(Component.translatable("tooltip.infwhat_botania.gaia_charm.usage")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(BotaniaAdditionConfig.gaiaCharmHardMode()
                        ? "tooltip.infwhat_botania.gaia_charm.hard"
                        : "tooltip.infwhat_botania.gaia_charm.normal")
                .withStyle(ChatFormatting.DARK_PURPLE));
        tooltip.add(Component.translatable(BotaniaAdditionConfig.gaiaCharmConsumes()
                        ? "tooltip.infwhat_botania.gaia_charm.consumed"
                        : "tooltip.infwhat_botania.gaia_charm.reusable")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
