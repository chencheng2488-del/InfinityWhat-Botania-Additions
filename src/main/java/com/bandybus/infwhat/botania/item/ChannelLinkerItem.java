package com.bandybus.infwhat.botania.item;

import com.bandybus.infwhat.botania.block.tile.AbstractManaEndpointTileEntity;
import com.bandybus.infwhat.botania.block.tile.ManaAnchorTileEntity;
import com.bandybus.infwhat.botania.block.tile.ManaWellTileEntity;
import com.bandybus.infwhat.botania.device.ManaNodeKind;
import com.bandybus.infwhat.botania.grid.ManaNetworkData;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

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

	public ChannelLinkerItem() {
	}

	@Override
	public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos,
									  EnumHand hand, EnumFacing facing,
									  float hitX, float hitY, float hitZ) {
		ItemStack stack = player.getHeldItem(hand);
		if (world.isRemote) {
			return EnumActionResult.SUCCESS;
		}

		ManaNodeKind kind = ManaNetworkData.kindAt(world, pos);
		if (kind == null) {
			player.sendStatusMessage(new TextComponentTranslation(
					"message.infwhat_botania.linker.invalid"), true);
			return EnumActionResult.SUCCESS;
		}
		net.minecraft.tileentity.TileEntity tileEntity = world.getTileEntity(pos);
		if (tileEntity instanceof AbstractManaEndpointTileEntity) {
			AbstractManaEndpointTileEntity endpoint =
					(AbstractManaEndpointTileEntity) tileEntity;
			if (endpoint.getOwnerId() != null
					&& !endpoint.isOwnedBy(player.getUniqueID())) {
				player.sendStatusMessage(new TextComponentTranslation(
						"message.infwhat_botania.linker.not_owner"), true);
				return EnumActionResult.SUCCESS;
			}
		}

		if (player.isSneaking()) {
			NBTTagCompound tag = stack.getTagCompound();
			if (tag == null) {
				tag = new NBTTagCompound();
				stack.setTagCompound(tag);
			}
			tag.setInteger(TAG_SOURCE_DIMENSION, world.provider.getDimension());
			tag.setLong(TAG_SOURCE_POS, pos.toLong());
			player.sendStatusMessage(new TextComponentTranslation(
					"message.infwhat_botania.linker.selected", label(world, pos)), true);
			return EnumActionResult.SUCCESS;
		}

		NBTTagCompound tag = stack.getTagCompound();
		if (tag == null || !tag.hasKey(TAG_SOURCE_POS)) {
			player.sendStatusMessage(new TextComponentTranslation(
					"message.infwhat_botania.linker.no_source"), true);
			return EnumActionResult.SUCCESS;
		}
		if (tag.getInteger(TAG_SOURCE_DIMENSION) != world.provider.getDimension()) {
			clearSource(stack);
			player.sendStatusMessage(new TextComponentTranslation(
					"message.infwhat_botania.linker.missing"), true);
			return EnumActionResult.SUCCESS;
		}
		BlockPos source = BlockPos.fromLong(tag.getLong(TAG_SOURCE_POS));
		if (ManaNetworkData.kindAt(world, source) == null) {
			clearSource(stack);
			player.sendStatusMessage(new TextComponentTranslation(
					"message.infwhat_botania.linker.missing"), true);
			return EnumActionResult.SUCCESS;
		}

		ManaNetworkData network = ManaNetworkData.get(world);
		if (network == null) {
			return EnumActionResult.SUCCESS;
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
			player.sendStatusMessage(new TextComponentTranslation(
					excluded ? "message.infwhat_botania.linker.unlinked_auto"
							: "message.infwhat_botania.linker.unlinked",
					sourceLabel, targetLabel), true);
			return EnumActionResult.SUCCESS;
		}

		ManaNetworkData.ConnectResult result = network.connect(world, source, pos);
		switch (result.result()) {
			case OK:
				clearSource(stack);
				player.sendStatusMessage(new TextComponentTranslation(
						"message.infwhat_botania.linker.linked", sourceLabel, targetLabel), true);
				break;
			case CONFLICT:
				player.sendStatusMessage(new TextComponentTranslation(
						"message.infwhat_botania.network.conflict",
						result.firstName(), result.secondName()), false);
				break;
			case INVALID_PAIR:
				player.sendStatusMessage(new TextComponentTranslation(
						"message.infwhat_botania.linker.invalid_pair"), true);
				break;
			case SAME_NODE:
				player.sendStatusMessage(new TextComponentTranslation(
						"message.infwhat_botania.linker.same"), true);
				break;
			case ALREADY:
				clearSource(stack);
				player.sendStatusMessage(new TextComponentTranslation(
						"message.infwhat_botania.linker.already"), true);
				break;
			default:
				clearSource(stack);
				player.sendStatusMessage(new TextComponentTranslation(
						"message.infwhat_botania.linker.missing"), true);
				break;
		}
		return EnumActionResult.SUCCESS;
	}

	@Override
	public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
		ItemStack stack = player.getHeldItem(hand);
		if (!world.isRemote) {
			NBTTagCompound tag = stack.getTagCompound();
			boolean hadSource = tag != null && tag.hasKey(TAG_SOURCE_POS);
			clearSource(stack);
			if (hadSource) {
				player.sendStatusMessage(new TextComponentTranslation(
						"message.infwhat_botania.linker.cleared"), true);
			}
		}
		return new ActionResult<ItemStack>(EnumActionResult.SUCCESS, stack);
	}

	private static void clearSource(ItemStack stack) {
		NBTTagCompound tag = stack.getTagCompound();
		if (tag != null) {
			tag.removeTag(TAG_SOURCE_DIMENSION);
			tag.removeTag(TAG_SOURCE_POS);
		}
	}

	/** Keeps a pool unlinked from a well/anchor until the player resets the auto scan. */
	private static boolean excludeAutoTarget(World world, BlockPos devicePos, BlockPos poolPos) {
		net.minecraft.tileentity.TileEntity tileEntity = world.getTileEntity(devicePos);
		if (!(tileEntity instanceof ManaWellTileEntity)
				&& !(tileEntity instanceof ManaAnchorTileEntity)) {
			return false;
		}
		if (tileEntity instanceof com.bandybus.infwhat.botania.device.ManaDeviceHost) {
			((com.bandybus.infwhat.botania.device.ManaDeviceHost) tileEntity).settings()
					.excludeTarget(poolPos);
			tileEntity.markDirty();
			return true;
		}
		return false;
	}

	private static String label(World world, BlockPos pos) {
		return com.bandybus.infwhat.botania.util.BlockLabels.of(world, pos);
	}

	@SideOnly(Side.CLIENT)
	@Override
	public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip,
							   ITooltipFlag flag) {
		super.addInformation(stack, world, tooltip, flag);
		NBTTagCompound tag = stack.getTagCompound();
		if (tag == null || !tag.hasKey(TAG_SOURCE_POS)) {
			tooltip.add(TextFormatting.GRAY + net.minecraft.client.resources.I18n.format(
					"tooltip.infwhat_botania.channel_linker.empty"));
		} else {
			BlockPos source = BlockPos.fromLong(tag.getLong(TAG_SOURCE_POS));
			tooltip.add(TextFormatting.AQUA + net.minecraft.client.resources.I18n.format(
					"tooltip.infwhat_botania.channel_linker.selected",
					source.getX() + ", " + source.getY() + ", " + source.getZ()));
		}
		tooltip.add(TextFormatting.DARK_GRAY + net.minecraft.client.resources.I18n.format(
				"tooltip.infwhat_botania.channel_linker.usage"));
	}
}
