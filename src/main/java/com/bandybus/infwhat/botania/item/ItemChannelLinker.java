package com.bandybus.infwhat.botania.item;

import com.bandybus.infwhat.botania.InfinityWhatBotania;
import com.bandybus.infwhat.botania.block.entity.ManaAnchorTileEntity;
import com.bandybus.infwhat.botania.block.entity.ManaWellTileEntity;
import com.bandybus.infwhat.botania.device.ManaDeviceHost;
import com.bandybus.infwhat.botania.device.ManaNodeKind;
import com.bandybus.infwhat.botania.grid.ManaNetworkData;
import com.bandybus.infwhat.botania.util.Pos;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

/**
 * Links two mana nodes into the same implicit network.
 *
 * <p>Sneak-right-click selects the initiating block; a normal right-click on another block creates
 * the link, or removes it when the two blocks are already linked. The direction mana flows is
 * decided by the block kinds, not by the click order. Right-clicking air clears the selection.
 */
public class ItemChannelLinker extends Item {

    private static final String TAG_SOURCE_DIMENSION = "SourceDimension";
    private static final String TAG_SOURCE_POS = "SourcePos";

    public ItemChannelLinker() {
        this.setUnlocalizedName(InfinityWhatBotania.MODID + ".channel_linker");
        this.setTextureName(ModItems.name("channel_linker"));
        this.setMaxStackSize(1);
        this.setCreativeTab(com.bandybus.infwhat.botania.util.CreativeTabHelper.coreTab());
    }

    @Override
    public boolean onItemUse(ItemStack stack, EntityPlayer player, World world,
                             int x, int y, int z, int side,
                             float hitX, float hitY, float hitZ) {
        if (world.isRemote) {
            return true;
        }
        Pos pos = new Pos(x, y, z);
        ManaNodeKind kind = ManaNetworkData.kindAt(world, pos);
        if (kind == null) {
            message(player, "message.infwhat_botania.linker.invalid");
            return true;
        }
        TileEntity tileEntity = world.getTileEntity(x, y, z);
        if (tileEntity instanceof com.bandybus.infwhat.botania.block.entity
                .AbstractManaEndpointTileEntity) {
            com.bandybus.infwhat.botania.block.entity.AbstractManaEndpointTileEntity endpoint =
                    (com.bandybus.infwhat.botania.block.entity.AbstractManaEndpointTileEntity)
                            tileEntity;
            if (endpoint.getOwnerId() != null && !endpoint.isOwnedBy(player.getUniqueID())) {
                message(player, "message.infwhat_botania.linker.not_owner");
                return true;
            }
        }

        if (player.isSneaking()) {
            NBTTagCompound tag = stack.getTagCompound();
            if (tag == null) {
                tag = new NBTTagCompound();
                stack.setTagCompound(tag);
            }
            tag.setInteger(TAG_SOURCE_DIMENSION, world.provider.dimensionId);
            tag.setLong(TAG_SOURCE_POS, pos.asLong());
            message(player, "message.infwhat_botania.linker.selected", label(world, pos));
            return true;
        }

        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null || !tag.hasKey(TAG_SOURCE_POS)) {
            message(player, "message.infwhat_botania.linker.no_source");
            return true;
        }
        if (tag.getInteger(TAG_SOURCE_DIMENSION) != world.provider.dimensionId) {
            clearSource(stack);
            message(player, "message.infwhat_botania.linker.missing");
            return true;
        }
        Pos source = Pos.of(tag.getLong(TAG_SOURCE_POS));
        if (ManaNetworkData.kindAt(world, source) == null) {
            clearSource(stack);
            message(player, "message.infwhat_botania.linker.missing");
            return true;
        }

        ManaNetworkData network = ManaNetworkData.get(world);
        if (network == null) {
            return true;
        }
        String sourceLabel = label(world, source);
        String targetLabel = label(world, pos);
        if (network.disconnect(world, source, pos)) {
            ManaNodeKind sourceKind = ManaNetworkData.kindAt(world, source);
            boolean excluded = false;
            if (sourceKind == ManaNodeKind.STORAGE
                    && (kind == ManaNodeKind.WELL || kind == ManaNodeKind.ANCHOR)) {
                excluded = excludeAutoTarget(world, pos, source);
            } else if (kind == ManaNodeKind.STORAGE
                    && (sourceKind == ManaNodeKind.WELL || sourceKind == ManaNodeKind.ANCHOR)) {
                excluded = excludeAutoTarget(world, source, pos);
            }
            clearSource(stack);
            message(player, excluded
                    ? "message.infwhat_botania.linker.unlinked_auto"
                    : "message.infwhat_botania.linker.unlinked", sourceLabel, targetLabel);
            return true;
        }

        ManaNetworkData.ConnectResult result = network.connect(world, source, pos);
        switch (result.result()) {
            case OK:
                clearSource(stack);
                message(player, "message.infwhat_botania.linker.linked",
                        sourceLabel, targetLabel);
                return true;
            case CONFLICT:
                send(player, new ChatComponentTranslation(
                        "message.infwhat_botania.network.conflict",
                        result.firstName(), result.secondName()));
                return true;
            case INVALID_PAIR:
                message(player, "message.infwhat_botania.linker.invalid_pair");
                return true;
            case SAME_NODE:
                message(player, "message.infwhat_botania.linker.same");
                return true;
            case ALREADY:
                clearSource(stack);
                message(player, "message.infwhat_botania.linker.already");
                return true;
            default:
                clearSource(stack);
                message(player, "message.infwhat_botania.linker.missing");
                return true;
        }
    }

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        if (!world.isRemote) {
            NBTTagCompound tag = stack.getTagCompound();
            boolean hadSource = tag != null && tag.hasKey(TAG_SOURCE_POS);
            clearSource(stack);
            if (hadSource) {
                message(player, "message.infwhat_botania.linker.cleared");
            }
        }
        return stack;
    }

    private static void clearSource(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag != null) {
            tag.removeTag(TAG_SOURCE_DIMENSION);
            tag.removeTag(TAG_SOURCE_POS);
        }
    }

    /** Keeps a pool unlinked from a well/anchor until the player resets the auto scan. */
    private static boolean excludeAutoTarget(World world, Pos devicePos, Pos poolPos) {
        TileEntity tileEntity = world.getTileEntity(devicePos.x, devicePos.y, devicePos.z);
        if (!(tileEntity instanceof ManaWellTileEntity)
                && !(tileEntity instanceof ManaAnchorTileEntity)) {
            return false;
        }
        if (tileEntity instanceof ManaDeviceHost) {
            ((ManaDeviceHost) tileEntity).settings().excludeTarget(poolPos);
            tileEntity.markDirty();
            return true;
        }
        return false;
    }

    private static String label(World world, Pos pos) {
        return com.bandybus.infwhat.botania.util.BlockNames.displayName(world, pos.x, pos.y, pos.z);
    }

    private static void message(EntityPlayer player, String key, Object... args) {
        send(player, new ChatComponentTranslation(key, args));
    }

    private static void send(EntityPlayer player, ChatComponentTranslation message) {
        player.addChatMessage(message);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void addInformation(ItemStack stack, EntityPlayer player, java.util.List tooltip,
                               boolean advanced) {
        super.addInformation(stack, player, tooltip, advanced);
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null || !tag.hasKey(TAG_SOURCE_POS)) {
            tooltip.add(StatCollector.translateToLocal(
                    "tooltip.infwhat_botania.channel_linker.empty"));
        } else {
            Pos source = Pos.of(tag.getLong(TAG_SOURCE_POS));
            tooltip.add(StatCollector.translateToLocalFormatted(
                    "tooltip.infwhat_botania.channel_linker.selected", source.toString()));
        }
        tooltip.add(StatCollector.translateToLocal(
                "tooltip.infwhat_botania.channel_linker.usage"));
    }
}

