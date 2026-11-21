package com.bandybus.infwhat.botania.item;

import com.bandybus.infwhat.botania.config.BotaniaAdditionConfig;
import com.bandybus.infwhat.botania.util.ManaReceiverHelper;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.registries.ForgeRegistries;
import vazkii.botania.api.mana.IManaPool;
import vazkii.botania.api.mana.IManaReceiver;

import javax.annotation.Nullable;
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
    public ActionResult<ItemStack> use(World level, PlayerEntity player, Hand hand) {
        ItemStack template = player.getItemInHand(hand);
        return new ActionResult<ItemStack>(handleUse(level, player, template), template);
    }

    private static ActionResultType handleUse(World level, PlayerEntity player,
                                              ItemStack template) {
        if (level.isClientSide) {
            return ActionResultType.SUCCESS;
        }
        if (!BotaniaAdditionConfig.runeTemplateEnabled()) {
            player.displayClientMessage(new TranslationTextComponent(
                    "message.infwhat_botania.rune_template.disabled"), true);
            return ActionResultType.CONSUME;
        }
        if (player.isShiftKeyDown()) {
            return bind(player, template);
        }
        return produce(level, player, template);
    }

    @Override
    public ActionResultType useOn(ItemUseContext context) {
        PlayerEntity player = context.getPlayer();
        if (player == null) {
            return ActionResultType.PASS;
        }
        return handleUse(context.getLevel(), player, player.getItemInHand(context.getHand()));
    }

    private static ActionResultType bind(PlayerEntity player, ItemStack template) {
        ItemStack rune = findRune(player);
        if (rune.isEmpty()) {
            player.displayClientMessage(new TranslationTextComponent(
                    "message.infwhat_botania.rune_template.no_rune"), true);
            return ActionResultType.CONSUME;
        }
        ResourceLocation runeId = ForgeRegistries.ITEMS.getKey(rune.getItem());
        ITextComponent runeName = rune.getHoverName();
        template.getOrCreateTag().putString(TAG_RUNE, runeId.toString());
        rune.shrink(1);
        player.inventory.setChanged();
        player.displayClientMessage(new TranslationTextComponent(
                "message.infwhat_botania.rune_template.bound", runeName), true);
        return ActionResultType.CONSUME;
    }

    private static ActionResultType produce(World level, PlayerEntity player,
                                            ItemStack template) {
        String runeId = boundRune(template);
        ResourceLocation id = ResourceLocation.tryParse(runeId);
        Item runeItem = id == null ? Items.AIR : ForgeRegistries.ITEMS.getValue(id);
        if (runeItem == null || !isBotaniaRune(runeItem)) {
            player.displayClientMessage(new TranslationTextComponent(
                    "message.infwhat_botania.rune_template.empty"), true);
            return ActionResultType.CONSUME;
        }

        int livingrockSlot = findLivingrockSlot(player);
        if (livingrockSlot < 0) {
            player.displayClientMessage(new TranslationTextComponent(
                    "message.infwhat_botania.rune_template.no_stone"), true);
            return ActionResultType.CONSUME;
        }

        int cost = BotaniaAdditionConfig.runeTemplateManaCost();
        IManaPool pool = findPool(level, player.blockPosition(), cost);
        if (pool == null) {
            player.displayClientMessage(new TranslationTextComponent(
                    "message.infwhat_botania.rune_template.no_pool"), true);
            return ActionResultType.CONSUME;
        }

        player.inventory.getItem(livingrockSlot).shrink(1);
        player.inventory.setChanged();
        pool.receiveMana(-cost);

        ItemStack result = new ItemStack(runeItem);
        if (!player.inventory.add(result)) {
            player.drop(result, false);
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENCHANTMENT_TABLE_USE, SoundCategory.PLAYERS, 0.8F, 1.0F);
        player.displayClientMessage(new TranslationTextComponent(
                "message.infwhat_botania.rune_template.crafted",
                new ItemStack(runeItem).getHoverName()), true);
        return ActionResultType.CONSUME;
    }

    private static String boundRune(ItemStack template) {
        CompoundNBT tag = template.getTag();
        return tag == null ? "" : tag.getString(TAG_RUNE);
    }

    private static ItemStack findRune(PlayerEntity player) {
        for (int slot = 0; slot < player.inventory.getContainerSize(); slot++) {
            ItemStack stack = player.inventory.getItem(slot);
            if (!stack.isEmpty() && isBotaniaRune(stack.getItem())) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private static int findLivingrockSlot(PlayerEntity player) {
        Item livingrock = ForgeRegistries.ITEMS.getValue(LIVINGROCK);
        if (livingrock == null || livingrock == Items.AIR) {
            return -1;
        }
        for (int slot = 0; slot < player.inventory.getContainerSize(); slot++) {
            if (player.inventory.getItem(slot).getItem() == livingrock) {
                return slot;
            }
        }
        return -1;
    }

    @Nullable
    private static IManaPool findPool(World level, BlockPos center, int cost) {
        BlockPos min = center.offset(-SEARCH_RADIUS, -SEARCH_RADIUS, -SEARCH_RADIUS);
        BlockPos max = center.offset(SEARCH_RADIUS, SEARCH_RADIUS, SEARCH_RADIUS);
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            if (!level.isLoaded(pos)) {
                continue;
            }
            IManaReceiver receiver = ManaReceiverHelper.find(level, pos);
            if (receiver instanceof IManaPool) {
                IManaPool pool = (IManaPool) receiver;
                if (pool.getCurrentMana() >= cost) {
                    return pool;
                }
            }
        }
        return null;
    }

    private static boolean isBotaniaRune(Item item) {
        if (item == null || item == Items.AIR) {
            return false;
        }
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
        return id != null && "botania".equals(id.getNamespace())
                && id.getPath().startsWith("rune_");
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable World level, List<ITextComponent> tooltip,
                                ITooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        if (!BotaniaAdditionConfig.runeTemplateEnabled()) {
            tooltip.add(new TranslationTextComponent("tooltip.infwhat_botania.rune_template.disabled")
                    .withStyle(TextFormatting.RED));
        }
        String runeId = boundRune(stack);
        if (runeId.isEmpty()) {
            tooltip.add(new TranslationTextComponent("tooltip.infwhat_botania.rune_template.empty")
                    .withStyle(TextFormatting.GRAY));
        } else {
            ResourceLocation id = ResourceLocation.tryParse(runeId);
            Item rune = id == null ? Items.AIR : ForgeRegistries.ITEMS.getValue(id);
            if (isBotaniaRune(rune)) {
                tooltip.add(new TranslationTextComponent(
                        "tooltip.infwhat_botania.rune_template.bound",
                        new ItemStack(rune).getHoverName()).withStyle(TextFormatting.AQUA));
            }
        }
        tooltip.add(new TranslationTextComponent("tooltip.infwhat_botania.rune_template.usage",
                Integer.valueOf(BotaniaAdditionConfig.runeTemplateManaCost()))
                .withStyle(TextFormatting.GRAY));
    }
}
