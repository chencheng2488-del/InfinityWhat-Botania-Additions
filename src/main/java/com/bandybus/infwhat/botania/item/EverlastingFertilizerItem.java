package com.bandybus.infwhat.botania.item;

import com.bandybus.infwhat.botania.config.BotaniaAdditionConfig;
import net.minecraft.block.BlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.DyeColor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import vazkii.botania.common.block.ModBlocks;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Reusable floral fertilizer.
 *
 * <p>Places Botania flowers in the same 7x5x7 area as Botania's own fertilizer, but never
 * consumes the item. Botania 1.16.5 picks a random coloured mystical flower directly, so no
 * tag lookup is needed.
 */
public class EverlastingFertilizerItem extends Item {

    private static final int RANGE = 3;

    public EverlastingFertilizerItem(Properties properties) {
        super(properties);
    }

    @Override
    public ActionResultType useOn(ItemUseContext context) {
        World level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (!BotaniaAdditionConfig.everlastingFertilizerEnabled()) {
            return ActionResultType.PASS;
        }

        if (level.isClientSide) {
            for (int index = 0; index < 15; index++) {
                double x = pos.getX() - RANGE + level.random.nextInt(RANGE * 2 + 1) + Math.random();
                double y = pos.getY() + 1 + Math.random();
                double z = pos.getZ() - RANGE + level.random.nextInt(RANGE * 2 + 1) + Math.random();
                level.addParticle(ParticleTypes.HAPPY_VILLAGER, x, y, z, 0.0D, 0.05D, 0.0D);
            }
            return ActionResultType.SUCCESS;
        }

        List<BlockPos> validPositions = new ArrayList<BlockPos>();
        for (int dx = -4; dx <= 2; dx++) {
            for (int dz = -4; dz <= 2; dz++) {
                for (int dy = 2; dy >= -2; dy--) {
                    BlockPos candidate = pos.offset(dx + 1, dy + 1, dz + 1);
                    if (!level.getBlockState(candidate).isAir()) {
                        continue;
                    }
                    if (level.dimensionType().ultraWarm() && candidate.getY() >= 255) {
                        continue;
                    }
                    BlockState flower = ModBlocks.whiteFlower.defaultBlockState();
                    if (flower.canSurvive(level, candidate)) {
                        validPositions.add(candidate);
                    }
                }
            }
        }

        Random random = level.random;
        int attempts = Math.min(validPositions.size(), random.nextBoolean() ? 3 : 4);
        for (int index = 0; index < attempts; index++) {
            BlockPos chosen = validPositions.get(random.nextInt(validPositions.size()));
            validPositions.remove(chosen);
            level.setBlockAndUpdate(chosen,
                    ModBlocks.getFlower(DyeColor.byId(random.nextInt(16))).defaultBlockState());
        }
        return ActionResultType.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable World level, List<ITextComponent> tooltip,
                                ITooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(new TranslationTextComponent(
                "tooltip.infwhat_botania.everlasting_fertilizer").withStyle(TextFormatting.GRAY));
    }
}
