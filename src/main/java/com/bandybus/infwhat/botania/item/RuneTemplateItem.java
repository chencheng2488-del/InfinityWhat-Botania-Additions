package com.bandybus.infwhat.botania.item;

import com.bandybus.infwhat.botania.config.BotaniaAdditionConfig;
import com.bandybus.infwhat.botania.util.ManaReceiverHelper;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import vazkii.botania.common.block.tile.mana.TilePool;

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

	private static final String TAG_RUNE_META = "RuneMeta";
	private static final int SEARCH_RADIUS = 8;

	public RuneTemplateItem() {
	}

	@Override
	public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
		ItemStack template = player.getHeldItem(hand);
		return new ActionResult<ItemStack>(handleUse(world, player, template), template);
	}

	@Override
	public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos,
									  EnumHand hand, EnumFacing facing,
									  float hitX, float hitY, float hitZ) {
		return handleUse(world, player, player.getHeldItem(hand));
	}

	private static EnumActionResult handleUse(World world, EntityPlayer player, ItemStack template) {
		if (world.isRemote) {
			return EnumActionResult.SUCCESS;
		}
		if (!BotaniaAdditionConfig.runeTemplateEnabled()) {
			player.sendStatusMessage(new TextComponentTranslation(
					"message.infwhat_botania.rune_template.disabled"), true);
			return EnumActionResult.SUCCESS;
		}
		if (player.isSneaking()) {
			return bind(player, template);
		}
		return produce(world, player, template);
	}

	private static EnumActionResult bind(EntityPlayer player, ItemStack template) {
		ItemStack rune = findRune(player);
		if (rune.isEmpty()) {
			player.sendStatusMessage(new TextComponentTranslation(
					"message.infwhat_botania.rune_template.no_rune"), true);
			return EnumActionResult.SUCCESS;
		}
		String runeName = rune.getDisplayName();
		NBTTagCompound tag = template.getTagCompound();
		if (tag == null) {
			tag = new NBTTagCompound();
			template.setTagCompound(tag);
		}
		tag.setInteger(TAG_RUNE_META, rune.getMetadata());
		rune.shrink(1);
		player.inventory.markDirty();
		player.sendStatusMessage(new TextComponentTranslation(
				"message.infwhat_botania.rune_template.bound", runeName), true);
		return EnumActionResult.SUCCESS;
	}

	private static EnumActionResult produce(World world, EntityPlayer player, ItemStack template) {
		NBTTagCompound tag = template.getTagCompound();
		int runeMeta = tag == null || !tag.hasKey(TAG_RUNE_META)
				? -1 : tag.getInteger(TAG_RUNE_META);
		if (!isBotaniaRune(runeMeta)) {
			player.sendStatusMessage(new TextComponentTranslation(
					"message.infwhat_botania.rune_template.empty"), true);
			return EnumActionResult.SUCCESS;
		}

		int livingrockSlot = findLivingrockSlot(player);
		if (livingrockSlot < 0) {
			player.sendStatusMessage(new TextComponentTranslation(
					"message.infwhat_botania.rune_template.no_stone"), true);
			return EnumActionResult.SUCCESS;
		}

		int cost = BotaniaAdditionConfig.runeTemplateManaCost();
		TilePool pool = findPool(world, player.getPosition(), cost);
		if (pool == null) {
			player.sendStatusMessage(new TextComponentTranslation(
					"message.infwhat_botania.rune_template.no_pool"), true);
			return EnumActionResult.SUCCESS;
		}

		player.inventory.getStackInSlot(livingrockSlot).shrink(1);
		player.inventory.markDirty();
		pool.recieveMana(-cost);

		ItemStack result = new ItemStack(vazkii.botania.common.item.ModItems.rune, 1, runeMeta);
		// InventoryPlayer.addItemStackToInventory shrinks the stack it is given, so the display
		// name must be captured before insertion or the chat feedback would say "Air".
		String resultName = result.getDisplayName();
		ItemStack toInsert = result.copy();
		if (!player.inventory.addItemStackToInventory(toInsert)) {
			player.dropItem(toInsert, false);
		}
		world.playSound(null, player.getPosition(), net.minecraft.init.SoundEvents
				.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.PLAYERS, 0.8F, 1.0F);
		player.sendStatusMessage(new TextComponentTranslation(
				"message.infwhat_botania.rune_template.crafted", resultName), true);
		return EnumActionResult.SUCCESS;
	}

	private static ItemStack findRune(EntityPlayer player) {
		for (int slot = 0; slot < player.inventory.getSizeInventory(); slot++) {
			ItemStack stack = player.inventory.getStackInSlot(slot);
			if (!stack.isEmpty() && stack.getItem()
					== vazkii.botania.common.item.ModItems.rune) {
				return stack;
			}
		}
		return ItemStack.EMPTY;
	}

	private static int findLivingrockSlot(EntityPlayer player) {
		Item livingrock = Item.getItemFromBlock(
				vazkii.botania.common.block.ModBlocks.livingrock);
		for (int slot = 0; slot < player.inventory.getSizeInventory(); slot++) {
			ItemStack stack = player.inventory.getStackInSlot(slot);
			if (!stack.isEmpty() && stack.getItem() == livingrock) {
				return slot;
			}
		}
		return -1;
	}

	private static TilePool findPool(World world, BlockPos center, int cost) {
		BlockPos min = center.add(-SEARCH_RADIUS, -SEARCH_RADIUS, -SEARCH_RADIUS);
		BlockPos max = center.add(SEARCH_RADIUS, SEARCH_RADIUS, SEARCH_RADIUS);
		for (BlockPos pos : BlockPos.getAllInBoxMutable(min, max)) {
			if (!world.isBlockLoaded(pos)) {
				continue;
			}
			TilePool pool = ManaReceiverHelper.find(world, pos) instanceof TilePool
					? (TilePool) ManaReceiverHelper.find(world, pos) : null;
			if (pool != null && pool.getCurrentMana() >= cost) {
				return pool;
			}
		}
		return null;
	}

	private static boolean isBotaniaRune(int meta) {
		return meta >= 0 && meta < 16;
	}

	@SideOnly(Side.CLIENT)
	@Override
	public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip,
							   ITooltipFlag flag) {
		super.addInformation(stack, world, tooltip, flag);
		if (!BotaniaAdditionConfig.runeTemplateEnabled()) {
			tooltip.add(TextFormatting.RED + I18n.format(
					"tooltip.infwhat_botania.rune_template.disabled"));
		}
		NBTTagCompound tag = stack.getTagCompound();
		int meta = tag == null || !tag.hasKey(TAG_RUNE_META)
				? -1 : tag.getInteger(TAG_RUNE_META);
		if (isBotaniaRune(meta)) {
			tooltip.add(TextFormatting.AQUA + I18n.format(
					"tooltip.infwhat_botania.rune_template.bound",
					new ItemStack(vazkii.botania.common.item.ModItems.rune, 1, meta)
							.getDisplayName()));
		} else {
			tooltip.add(TextFormatting.GRAY + I18n.format(
					"tooltip.infwhat_botania.rune_template.empty"));
		}
		tooltip.add(TextFormatting.GRAY + I18n.format(
				"tooltip.infwhat_botania.rune_template.usage",
				BotaniaAdditionConfig.runeTemplateManaCost()));
	}
}
