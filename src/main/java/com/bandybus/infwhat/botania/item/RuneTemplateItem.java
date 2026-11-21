package com.bandybus.infwhat.botania.item;

import com.bandybus.infwhat.botania.config.BotaniaAdditionConfig;
import com.bandybus.infwhat.botania.util.ManaReceiverHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import vazkii.botania.api.mana.ManaPool;

import java.util.List;

/**
 * Reusable Rune Template.
 *
 * <p>Sneak-right-click with a Botania rune in the inventory to bind the template to that rune.
 * Normal right-click consumes one livingrock and mana from a nearby pool, then produces the bound
 * rune. The template itself is never consumed; it is a QoL shortcut for players who have already
 * made the rune once.
 */
public class RuneTemplateItem extends Item {

    public static final String TAG_RUNE = "Rune";
    private static final int SEARCH_RADIUS = 8;
    private static final ResourceLocation LIVINGROCK =
            new ResourceLocation("botania", "livingrock");

    public RuneTemplateItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack template = player.getItemInHand(hand);
        return new InteractionResultHolder<>(handleUse(level, player, template), template);
    }

    private static InteractionResult handleUse(Level level, Player player, ItemStack template) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!BotaniaAdditionConfig.runeTemplateEnabled()) {
            player.displayClientMessage(Component.translatable(
                    "message.infwhat_botania.rune_template.disabled"), true);
            return InteractionResult.CONSUME;
        }
        if (player.isShiftKeyDown()) {
            return bind(level, player, template);
        }
        return produce(level, player, template);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        return handleUse(context.getLevel(), player,
                player.getItemInHand(context.getHand()));
    }

    private static InteractionResult bind(Level level, Player player, ItemStack template) {
        ItemStack rune = findRune(player);
        if (rune.isEmpty()) {
            player.displayClientMessage(Component.translatable(
                    "message.infwhat_botania.rune_template.no_rune"), true);
            return InteractionResult.CONSUME;
        }
        ResourceLocation runeId = BuiltInRegistries.ITEM.getKey(rune.getItem());
        Component runeName = rune.getHoverName();
        template.getOrCreateTag().putString(TAG_RUNE, runeId.toString());
        rune.shrink(1);
        player.getInventory().setChanged();
        player.displayClientMessage(Component.translatable(
                "message.infwhat_botania.rune_template.bound", runeName), true);
        return InteractionResult.CONSUME;
    }

    private static InteractionResult produce(Level level, Player player, ItemStack template) {
        String runeId = boundRune(template);
        ResourceLocation id = ResourceLocation.tryParse(runeId);
        Item runeItem = id == null ? Items.AIR : BuiltInRegistries.ITEM.get(id);
        if (!isBotaniaRune(runeItem)) {
            player.displayClientMessage(Component.translatable(
                    "message.infwhat_botania.rune_template.empty"), true);
            return InteractionResult.CONSUME;
        }

        int livingrockSlot = findLivingrockSlot(player);
        if (livingrockSlot < 0) {
            player.displayClientMessage(Component.translatable(
                    "message.infwhat_botania.rune_template.no_stone"), true);
            return InteractionResult.CONSUME;
        }

        int cost = BotaniaAdditionConfig.runeTemplateManaCost();
        ManaPool pool = findPool(level, player.blockPosition(), cost);
        if (pool == null) {
            player.displayClientMessage(Component.translatable(
                    "message.infwhat_botania.rune_template.no_pool"), true);
            return InteractionResult.CONSUME;
        }

        player.getInventory().getItem(livingrockSlot).shrink(1);
        player.getInventory().setChanged();
        pool.receiveMana(-cost);

        ItemStack result = new ItemStack(runeItem);
        if (!player.getInventory().add(result)) {
            player.drop(result, false);
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.8F, 1.0F);
        player.displayClientMessage(Component.translatable(
                "message.infwhat_botania.rune_template.crafted",
                new ItemStack(runeItem).getHoverName()), true);
        return InteractionResult.CONSUME;
    }

    private static String boundRune(ItemStack template) {
        return template.getTag() == null ? "" : template.getTag().getString(TAG_RUNE);
    }

    private static ItemStack findRune(Player player) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (isBotaniaRune(stack.getItem()) && !stack.isEmpty()) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private static int findLivingrockSlot(Player player) {
        Item livingrock = BuiltInRegistries.ITEM.get(LIVINGROCK);
        if (livingrock == Items.AIR) {
            return -1;
        }
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            if (player.getInventory().getItem(slot).is(livingrock)) {
                return slot;
            }
        }
        return -1;
    }

    private static ManaPool findPool(Level level, BlockPos center, int cost) {
        BlockPos min = center.offset(-SEARCH_RADIUS, -SEARCH_RADIUS, -SEARCH_RADIUS);
        BlockPos max = center.offset(SEARCH_RADIUS, SEARCH_RADIUS, SEARCH_RADIUS);
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            if (!level.hasChunkAt(pos)) {
                continue;
            }
            if (ManaReceiverHelper.find(level, pos, null) instanceof ManaPool pool
                    && pool.getCurrentMana() >= cost) {
                return pool;
            }
        }
        return null;
    }

    private static boolean isBotaniaRune(Item item) {
        if (item == Items.AIR) {
            return false;
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        return "botania".equals(id.getNamespace()) && id.getPath().startsWith("rune_");
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip,
                                TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        if (!BotaniaAdditionConfig.runeTemplateEnabled()) {
            tooltip.add(Component.translatable("tooltip.infwhat_botania.rune_template.disabled")
                    .withStyle(ChatFormatting.RED));
        }
        String runeId = boundRune(stack);
        if (runeId.isEmpty()) {
            tooltip.add(Component.translatable("tooltip.infwhat_botania.rune_template.empty")
                    .withStyle(ChatFormatting.GRAY));
        } else {
            ResourceLocation id = ResourceLocation.tryParse(runeId);
            Item rune = id == null ? Items.AIR : BuiltInRegistries.ITEM.get(id);
            if (isBotaniaRune(rune)) {
                tooltip.add(Component.translatable("tooltip.infwhat_botania.rune_template.bound",
                        new ItemStack(rune).getHoverName()).withStyle(ChatFormatting.AQUA));
            }
        }
        tooltip.add(Component.translatable("tooltip.infwhat_botania.rune_template.usage",
                BotaniaAdditionConfig.runeTemplateManaCost()).withStyle(ChatFormatting.GRAY));
    }
}
