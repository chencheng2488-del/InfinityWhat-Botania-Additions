package com.bandybus.infwhat.botania.item;

import com.bandybus.infwhat.botania.config.BotaniaAdditionConfig;
import com.bandybus.infwhat.botania.util.GaiaSummonHelper;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
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
 * Summons the Gaia Guardian without building Botania's beacon and Gaia Pylon structure.
 *
 * <p>The boss is still created by Botania's own entity class, so health scaling, boss bar, music,
 * drops and the hard mode behaviour stay identical. Only the structure validation is skipped,
 * through {@link GaiaSummonHelper}.
 */
public class GaiaCharmItem extends Item {

	public GaiaCharmItem() {
	}

	@Override
	public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
		ItemStack charm = player.getHeldItem(hand);
		return new ActionResult<ItemStack>(summon(world, player, charm), charm);
	}

	@Override
	public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos,
									  EnumHand hand, EnumFacing facing,
									  float hitX, float hitY, float hitZ) {
		return summon(world, player, player.getHeldItem(hand));
	}

	private static EnumActionResult summon(World world, EntityPlayer player, ItemStack charm) {
		if (world.isRemote) {
			return EnumActionResult.SUCCESS;
		}
		if (!BotaniaAdditionConfig.gaiaCharmEnabled()) {
			player.sendStatusMessage(new TextComponentTranslation(
					"message.infwhat_botania.gaia_charm.disabled"), true);
			return EnumActionResult.SUCCESS;
		}

		boolean hardMode = BotaniaAdditionConfig.gaiaCharmHardMode();
		if (!GaiaSummonHelper.summon(player, world, player.getPosition(), hardMode)) {
			player.sendStatusMessage(new TextComponentTranslation(
					"message.infwhat_botania.gaia_charm.failed"), true);
			return EnumActionResult.SUCCESS;
		}

		if (BotaniaAdditionConfig.gaiaCharmConsumes()
				&& !player.capabilities.isCreativeMode) {
			charm.shrink(1);
		}
		player.sendStatusMessage(new TextComponentTranslation(hardMode
				? "message.infwhat_botania.gaia_charm.summoned_hard"
				: "message.infwhat_botania.gaia_charm.summoned"), true);
		return EnumActionResult.SUCCESS;
	}

	@SideOnly(Side.CLIENT)
	@Override
	public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip,
							   ITooltipFlag flag) {
		super.addInformation(stack, world, tooltip, flag);
		if (!BotaniaAdditionConfig.gaiaCharmEnabled()) {
			tooltip.add(TextFormatting.RED + I18n.format(
					"tooltip.infwhat_botania.gaia_charm.disabled"));
		}
		tooltip.add(TextFormatting.GRAY + I18n.format(
				"tooltip.infwhat_botania.gaia_charm.usage"));
		tooltip.add(TextFormatting.DARK_PURPLE + I18n.format(
				BotaniaAdditionConfig.gaiaCharmHardMode()
						? "tooltip.infwhat_botania.gaia_charm.hard"
						: "tooltip.infwhat_botania.gaia_charm.normal"));
		tooltip.add(TextFormatting.DARK_GRAY + I18n.format(
				BotaniaAdditionConfig.gaiaCharmConsumes()
						? "tooltip.infwhat_botania.gaia_charm.consumed"
						: "tooltip.infwhat_botania.gaia_charm.reusable"));
	}
}
