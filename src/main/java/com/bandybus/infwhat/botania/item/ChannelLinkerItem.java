package com.bandybus.infwhat.botania.item;

import com.bandybus.infwhat.botania.block.entity.AbstractManaEndpointTileEntity;
import com.bandybus.infwhat.botania.block.entity.ManaAnchorTileEntity;
import com.bandybus.infwhat.botania.block.entity.ManaWellTileEntity;
import com.bandybus.infwhat.botania.device.ManaDeviceHost;
import com.bandybus.infwhat.botania.device.ManaNodeKind;
import com.bandybus.infwhat.botania.grid.ManaNetworkData;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

import javax.annotation.Nullable;
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
    public ActionResultType useOn(net.minecraft.item.ItemUseContext context) {
        World level = context.getLevel();
        net.minecraft.entity.player.PlayerEntity player = context.getPlayer();
        if (player == null) {
            return ActionResultType.PASS;
        }
        if (level.isClientSide) {
            return ActionResultType.SUCCESS;
        }
        if (!(level instanceof ServerWorld)) {
            return ActionResultType.CONSUME;
        }
        ServerWorld serverLevel = (ServerWorld) level;

        BlockPos pos = context.getClickedPos();
        ManaNodeKind kind = ManaNetworkData.kindAt(serverLevel, pos);
        if (kind == null) {
            player.displayClientMessage(new TranslationTextComponent(
                    "message.infwhat_botania.linker.invalid"), true);
            return ActionResultType.CONSUME;
        }
        TileEntity tileEntity = serverLevel.getBlockEntity(pos);
        if (tileEntity instanceof AbstractManaEndpointTileEntity) {
            AbstractManaEndpointTileEntity endpoint = (AbstractManaEndpointTileEntity) tileEntity;
            if (endpoint.getOwnerId() != null && !endpoint.isOwnedBy(player.getUUID())) {
                player.displayClientMessage(new TranslationTextComponent(
                        "message.infwhat_botania.linker.not_owner"), true);
                return ActionResultType.CONSUME;
            }
        }

        ItemStack stack = context.getItemInHand();
        if (player.isShiftKeyDown()) {
            CompoundNBT tag = stack.getOrCreateTag();
            tag.putString(TAG_SOURCE_DIMENSION, serverLevel.dimension().location().toString());
            tag.putLong(TAG_SOURCE_POS, pos.asLong());
            player.displayClientMessage(new TranslationTextComponent(
                    "message.infwhat_botania.linker.selected", label(serverLevel, pos)), true);
            return ActionResultType.CONSUME;
        }

        CompoundNBT tag = stack.getTag();
        if (tag == null || !tag.contains(TAG_SOURCE_POS)) {
            player.displayClientMessage(new TranslationTextComponent(
                    "message.infwhat_botania.linker.no_source"), true);
            return ActionResultType.CONSUME;
        }
        String dimension = tag.getString(TAG_SOURCE_DIMENSION);
        if (!dimension.equals(serverLevel.dimension().location().toString())) {
            clearSource(stack);
            player.displayClientMessage(new TranslationTextComponent(
                    "message.infwhat_botania.linker.missing"), true);
            return ActionResultType.CONSUME;
        }
        BlockPos source = BlockPos.of(tag.getLong(TAG_SOURCE_POS));
        if (ManaNetworkData.kindAt(serverLevel, source) == null) {
            clearSource(stack);
            player.displayClientMessage(new TranslationTextComponent(
                    "message.infwhat_botania.linker.missing"), true);
            return ActionResultType.CONSUME;
        }

        ManaNetworkData network = ManaNetworkData.get(serverLevel);
        if (network == null) {
            return ActionResultType.CONSUME;
        }
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
            player.displayClientMessage(new TranslationTextComponent(
                    excluded ? "message.infwhat_botania.linker.unlinked_auto"
                            : "message.infwhat_botania.linker.unlinked",
                    sourceLabel, targetLabel), true);
            return ActionResultType.CONSUME;
        }

        ManaNetworkData.ConnectResult result = network.connect(serverLevel, source, pos);
        switch (result.result()) {
            case OK:
                clearSource(stack);
                player.displayClientMessage(new TranslationTextComponent(
                        "message.infwhat_botania.linker.linked", sourceLabel, targetLabel), true);
                return ActionResultType.CONSUME;
            case CONFLICT:
                player.displayClientMessage(new TranslationTextComponent(
                        "message.infwhat_botania.network.conflict",
                        result.firstName(), result.secondName()), false);
                return ActionResultType.CONSUME;
            case INVALID_PAIR:
                player.displayClientMessage(new TranslationTextComponent(
                        "message.infwhat_botania.linker.invalid_pair"), true);
                return ActionResultType.CONSUME;
            case SAME_NODE:
                player.displayClientMessage(new TranslationTextComponent(
                        "message.infwhat_botania.linker.same"), true);
                return ActionResultType.CONSUME;
            case ALREADY:
                clearSource(stack);
                player.displayClientMessage(new TranslationTextComponent(
                        "message.infwhat_botania.linker.already"), true);
                return ActionResultType.CONSUME;
            default:
                clearSource(stack);
                player.displayClientMessage(new TranslationTextComponent(
                        "message.infwhat_botania.linker.missing"), true);
                return ActionResultType.CONSUME;
        }
    }

    @Override
    public ActionResult<ItemStack> use(World level, net.minecraft.entity.player.PlayerEntity player,
                                       Hand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            CompoundNBT tag = stack.getTag();
            boolean hadSource = tag != null && tag.contains(TAG_SOURCE_POS);
            clearSource(stack);
            if (hadSource) {
                player.displayClientMessage(new TranslationTextComponent(
                        "message.infwhat_botania.linker.cleared"), true);
            }
        }
        return new ActionResult<ItemStack>(
                ActionResultType.sidedSuccess(level.isClientSide), stack);
    }

    private static void clearSource(ItemStack stack) {
        CompoundNBT tag = stack.getTag();
        if (tag != null) {
            tag.remove(TAG_SOURCE_DIMENSION);
            tag.remove(TAG_SOURCE_POS);
        }
    }

    /** Keeps a pool unlinked from a well/anchor until the player resets the auto scan. */
    private static boolean excludeAutoTarget(ServerWorld level, BlockPos devicePos, BlockPos poolPos) {
        TileEntity tileEntity = level.getBlockEntity(devicePos);
        if (!(tileEntity instanceof ManaWellTileEntity)
                && !(tileEntity instanceof ManaAnchorTileEntity)) {
            return false;
        }
        if (tileEntity instanceof ManaDeviceHost) {
            ((ManaDeviceHost) tileEntity).settings().excludeTarget(poolPos);
            tileEntity.setChanged();
            return true;
        }
        return false;
    }

    private static String label(ServerWorld level, BlockPos pos) {
        return level.getBlockState(pos).getBlock().getName().getString();
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable World level, List<ITextComponent> tooltip,
                                ITooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        CompoundNBT tag = stack.getTag();
        if (tag == null || !tag.contains(TAG_SOURCE_POS)) {
            tooltip.add(new TranslationTextComponent(
                    "tooltip.infwhat_botania.channel_linker.empty").withStyle(TextFormatting.GRAY));
        } else {
            BlockPos source = BlockPos.of(tag.getLong(TAG_SOURCE_POS));
            tooltip.add(new TranslationTextComponent(
                    "tooltip.infwhat_botania.channel_linker.selected",
                    source.getX() + ", " + source.getY() + ", " + source.getZ())
                    .withStyle(TextFormatting.AQUA));
        }
        tooltip.add(new TranslationTextComponent(
                "tooltip.infwhat_botania.channel_linker.usage").withStyle(TextFormatting.DARK_GRAY));
    }
}
