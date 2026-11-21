package com.bandybus.infwhat.botania.item;

import com.bandybus.infwhat.botania.config.BotaniaAdditionConfig;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import vazkii.botania.api.state.BotaniaStateProps;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Reusable floral fertilizer.
 *
 * <p>Places Botania flowers in the same 7x5x7 area as Botania's own fertilizer, but never consumes
 * the item. The placement logic is reproduced directly so no server-only inner class of Botania
 * is needed.
 */
public class EverlastingFertilizerItem extends Item {

	private static final int RANGE = 3;

	public EverlastingFertilizerItem() {
	}

	@Override
	public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos,
									  EnumHand hand, EnumFacing facing,
									  float hitX, float hitY, float hitZ) {
		if (!BotaniaAdditionConfig.everlastingFertilizerEnabled()) {
			return EnumActionResult.PASS;
		}
		if (world.isRemote) {
			for (int index = 0; index < 15; index++) {
				double x = pos.getX() - RANGE + world.rand.nextInt(RANGE * 2 + 1)
						+ Math.random();
				double y = pos.getY() + 1 + Math.random();
				double z = pos.getZ() - RANGE + world.rand.nextInt(RANGE * 2 + 1)
						+ Math.random();
				world.spawnParticle(EnumParticleTypes.VILLAGER_HAPPY, x, y, z,
						0.0D, 0.05D, 0.0D);
			}
			return EnumActionResult.SUCCESS;
		}

		List<BlockPos> validPositions = new ArrayList<BlockPos>();
		for (int i = -RANGE - 1; i < RANGE; i++) {
			for (int j = -RANGE - 1; j < RANGE; j++) {
				for (int k = 2; k >= -2; k--) {
					BlockPos candidate = pos.add(i + 1, k + 1, j + 1);
					if (world.isAirBlock(candidate)
							&& (!world.provider.isNether() || candidate.getY() < 255)
							&& vazkii.botania.common.block.ModBlocks.flower
									.canPlaceBlockAt(world, candidate)) {
						validPositions.add(candidate);
					}
				}
			}
		}

		int flowerCount = Math.min(validPositions.size(),
				world.rand.nextBoolean() ? 3 : 4);
		for (int index = 0; index < flowerCount; index++) {
			BlockPos chosen = validPositions.remove(world.rand.nextInt(validPositions.size()));
			world.setBlockState(chosen,
					vazkii.botania.common.block.ModBlocks.flower.getDefaultState()
							.withProperty(BotaniaStateProps.COLOR,
									EnumDyeColor.byMetadata(world.rand.nextInt(16))),
					1 | 2);
		}
		return EnumActionResult.SUCCESS;
	}

	@SideOnly(Side.CLIENT)
	@Override
	public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip,
							   ITooltipFlag flag) {
		super.addInformation(stack, world, tooltip, flag);
		tooltip.add(TextFormatting.GRAY + I18n.format(
				"tooltip.infwhat_botania.everlasting_fertilizer"));
	}
}
