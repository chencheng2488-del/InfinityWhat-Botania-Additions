package com.bandybus.infwhat.botania.item;

import com.bandybus.infwhat.botania.config.BotaniaAdditionConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import vazkii.botania.common.block.ModBlocks;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Reusable floral fertilizer.
 *
 * <p>Places Botania flowers in the same 7x5x7 area as Botania's own fertilizer, but never
 * consumes the item. Botania 1.18.2 picks a random coloured mystical flower directly, so no
 * tag lookup is needed.
 */
public class EverlastingFertilizerItem extends Item {

    private static final int RANGE = 3;

    public EverlastingFertilizerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (!BotaniaAdditionConfig.everlastingFertilizerEnabled()) {
            return InteractionResult.PASS;
        }

        if (level.isClientSide) {
            for (int index = 0; index < 15; index++) {
                double x = pos.getX() - RANGE + level.random.nextInt(RANGE * 2 + 1) + Math.random();
                double y = pos.getY() + 1 + Math.random();
                double z = pos.getZ() - RANGE + level.random.nextInt(RANGE * 2 + 1) + Math.random();
                level.addParticle(net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER,
                        x, y, z, 0.0D, 0.05D, 0.0D);
            }
            return InteractionResult.SUCCESS;
        }

        List<BlockPos> validPositions = new ArrayList<>();
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
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip,
                                TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(new TranslatableComponent(
                "tooltip.infwhat_botania.everlasting_fertilizer").withStyle(ChatFormatting.GRAY));
    }
}
