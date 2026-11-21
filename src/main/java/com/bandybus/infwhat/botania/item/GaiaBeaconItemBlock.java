package com.bandybus.infwhat.botania.item;

import com.bandybus.infwhat.botania.config.BotaniaAdditionConfig;
import net.minecraft.block.Block;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

/** Block item for the Gaia Beacon with a config-aware tooltip. */
public class GaiaBeaconItemBlock extends ItemBlock {

	public GaiaBeaconItemBlock(Block block) {
		super(block);
	}

	@SideOnly(Side.CLIENT)
	@Override
	public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip,
							   ITooltipFlag flag) {
		super.addInformation(stack, world, tooltip, flag);
		if (!BotaniaAdditionConfig.gaiaBeaconEnabled()) {
			tooltip.add(TextFormatting.RED + I18n.format(
					"tooltip.infwhat_botania.gaia_beacon.disabled"));
		}
		tooltip.add(TextFormatting.GRAY + I18n.format(
				"tooltip.infwhat_botania.gaia_beacon.usage",
				BotaniaAdditionConfig.gaiaBeaconManaCost()));
	}
}
