package com.bandybus.infwhat.botania.item;

import com.bandybus.infwhat.botania.block.entity.AbstractManaEndpointBlockEntity;
import com.bandybus.infwhat.botania.block.entity.ManaAnchorBlockEntity;
import com.bandybus.infwhat.botania.block.entity.ManaWellBlockEntity;
import com.bandybus.infwhat.botania.device.ManaDeviceHost;
import com.bandybus.infwhat.botania.device.ManaNodeKind;
import com.bandybus.infwhat.botania.grid.ManaNetworkData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Links two mana nodes into the same implicit network.
 *
 * <p>Sneak-right-click selects the initiating block; a normal right-click on another block creates
 * the link, or removes it when the two blocks are already linked. The direction mana flows is
 * decided by the block kinds, not by the click order. Right-clicking air clears the selection.
 */
public class ChannelLinkerItem extends Item {

    private static final String TAG_SOURCE_DIMENSION = "SourceDimension";
    private static final String TAG_SOURCE_POS = "SourcePos";

    public ChannelLinkerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.CONSUME;
        }

        BlockPos pos = context.getClickedPos();
        ManaNodeKind kind = ManaNetworkData.kindAt(serverLevel, pos);
        if (kind == null) {
            player.displayClientMessage(new net.minecraft.network.chat.TranslatableComponent(
                    "message.infwhat_botania.linker.invalid"), true);
            return InteractionResult.CONSUME;
        }
        BlockEntity blockEntity = serverLevel.getBlockEntity(pos);
        if (blockEntity instanceof AbstractManaEndpointBlockEntity endpoint
                && endpoint.getOwnerId() != null && !endpoint.isOwnedBy(player.getUUID())) {
            player.displayClientMessage(new net.minecraft.network.chat.TranslatableComponent(
                    "message.infwhat_botania.linker.not_owner"), true);
            return InteractionResult.CONSUME;
        }

        ItemStack stack = context.getItemInHand();
        if (player.isShiftKeyDown()) {
            CompoundTag tag = stack.getOrCreateTag();
            tag.putString(TAG_SOURCE_DIMENSION, serverLevel.dimension().location().toString());
            tag.putLong(TAG_SOURCE_POS, pos.asLong());
            player.displayClientMessage(new net.minecraft.network.chat.TranslatableComponent(
                    "message.infwhat_botania.linker.selected", label(serverLevel, pos)), true);
            return InteractionResult.CONSUME;
        }

        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TAG_SOURCE_POS)) {
            player.displayClientMessage(new net.minecraft.network.chat.TranslatableComponent(
                    "message.infwhat_botania.linker.no_source"), true);
            return InteractionResult.CONSUME;
        }
        String dimension = tag.getString(TAG_SOURCE_DIMENSION);
        if (!dimension.equals(serverLevel.dimension().location().toString())) {
            clearSource(stack);
            player.displayClientMessage(new net.minecraft.network.chat.TranslatableComponent(
                    "message.infwhat_botania.linker.missing"), true);
            return InteractionResult.CONSUME;
        }
        BlockPos source = BlockPos.of(tag.getLong(TAG_SOURCE_POS));
        if (ManaNetworkData.kindAt(serverLevel, source) == null) {
            clearSource(stack);
            player.displayClientMessage(new net.minecraft.network.chat.TranslatableComponent(
                    "message.infwhat_botania.linker.missing"), true);
            return InteractionResult.CONSUME;
        }

        ManaNetworkData network = ManaNetworkData.get(serverLevel);
        String sourceLabel = label(serverLevel, source);
        String targetLabel = label(serverLevel, pos);
        if (network.disconnect(serverLevel, source, pos)) {
            ManaNodeKind sourceKind = ManaNetworkData.kindAt(serverLevel, source);
            boolean excluded = false;
            if (sourceKind == ManaNodeKind.STORAGE
                    && (kind == ManaNodeKind.WELL || kind == ManaNodeKind.ANCHOR)) {
                excluded = excludeAutoTarget(serverLevel, pos, source);
            } else if (kind == ManaNodeKind.STORAGE
                    && (sourceKind == ManaNodeKind.WELL || sourceKind == ManaNodeKind.ANCHOR)) {
                excluded = excludeAutoTarget(serverLevel, source, pos);
            }
            clearSource(stack);
            player.displayClientMessage(new net.minecraft.network.chat.TranslatableComponent(
                    excluded ? "message.infwhat_botania.linker.unlinked_auto"
                            : "message.infwhat_botania.linker.unlinked",
                    sourceLabel, targetLabel), true);
            return InteractionResult.CONSUME;
        }

        ManaNetworkData.ConnectResult result = network.connect(serverLevel, source, pos);
        switch (result.result()) {
            case OK -> {
                clearSource(stack);
                player.displayClientMessage(new net.minecraft.network.chat.TranslatableComponent(
                        "message.infwhat_botania.linker.linked", sourceLabel, targetLabel), true);
                return InteractionResult.CONSUME;
            }
            case CONFLICT -> {
                player.displayClientMessage(new net.minecraft.network.chat.TranslatableComponent(
                        "message.infwhat_botania.network.conflict",
                        result.firstName(), result.secondName()), false);
                return InteractionResult.CONSUME;
            }
            case INVALID_PAIR -> {
                player.displayClientMessage(new net.minecraft.network.chat.TranslatableComponent(
                        "message.infwhat_botania.linker.invalid_pair"), true);
                return InteractionResult.CONSUME;
            }
            case SAME_NODE -> {
                player.displayClientMessage(new net.minecraft.network.chat.TranslatableComponent(
                        "message.infwhat_botania.linker.same"), true);
                return InteractionResult.CONSUME;
            }
            case ALREADY -> {
                clearSource(stack);
                player.displayClientMessage(new net.minecraft.network.chat.TranslatableComponent(
                        "message.infwhat_botania.linker.already"), true);
                return InteractionResult.CONSUME;
            }
            default -> {
                clearSource(stack);
                player.displayClientMessage(new net.minecraft.network.chat.TranslatableComponent(
                        "message.infwhat_botania.linker.missing"), true);
                return InteractionResult.CONSUME;
            }
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            CompoundTag tag = stack.getTag();
            boolean hadSource = tag != null && tag.contains(TAG_SOURCE_POS);
            clearSource(stack);
            if (hadSource) {
                player.displayClientMessage(new net.minecraft.network.chat.TranslatableComponent(
                        "message.infwhat_botania.linker.cleared"), true);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    private static void clearSource(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null) {
            tag.remove(TAG_SOURCE_DIMENSION);
            tag.remove(TAG_SOURCE_POS);
        }
    }

    /** Keeps a pool unlinked from a well/anchor until the player resets the auto scan. */
    private static boolean excludeAutoTarget(ServerLevel level, BlockPos devicePos, BlockPos poolPos) {
        BlockEntity blockEntity = level.getBlockEntity(devicePos);
        if (!(blockEntity instanceof ManaWellBlockEntity)
                && !(blockEntity instanceof ManaAnchorBlockEntity)) {
            return false;
        }
        if (blockEntity instanceof ManaDeviceHost host) {
            host.settings().excludeTarget(poolPos);
            blockEntity.setChanged();
            return true;
        }
        return false;
    }

    private static String label(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos).getBlock().getName().getString();
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip,
                                TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TAG_SOURCE_POS)) {
            tooltip.add(new net.minecraft.network.chat.TranslatableComponent("tooltip.infwhat_botania.channel_linker.empty")
                    .withStyle(ChatFormatting.GRAY));
        } else {
            BlockPos source = BlockPos.of(tag.getLong(TAG_SOURCE_POS));
            tooltip.add(new net.minecraft.network.chat.TranslatableComponent("tooltip.infwhat_botania.channel_linker.selected",
                            source.getX() + ", " + source.getY() + ", " + source.getZ())
                    .withStyle(ChatFormatting.AQUA));
        }
        tooltip.add(new net.minecraft.network.chat.TranslatableComponent("tooltip.infwhat_botania.channel_linker.usage")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
