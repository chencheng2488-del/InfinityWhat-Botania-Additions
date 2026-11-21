package com.bandybus.infwhat.botania.item;

import com.bandybus.infwhat.botania.config.BotaniaAdditionConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Reusable floral fertilizer.
 *
 * <p>Places Botania flowers and shimmering mushrooms in the same 7x5x7 area as Botania's own
 * fertilizer, but never consumes the item. The placement logic is implemented directly so no
 * server-only inner class of Botania is needed.
 */
public class EverlastingFertilizerItem extends Item {

    private static final int RANGE = 3;
    private static final TagKey<Block> MYSTICAL_FLOWERS =
            TagKey.create(Registries.BLOCK, new ResourceLocation("botania", "mystical_flowers"));
    private static final TagKey<Block> SHIMMERING_MUSHROOMS =
            TagKey.create(Registries.BLOCK, new ResourceLocation("botania", "shimmering_mushrooms"));

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
                level.addParticle(ParticleTypes.HAPPY_VILLAGER, x, y, z, 0.0D, 0.05D, 0.0D);
            }
            return InteractionResult.SUCCESS;
        }

        Optional<HolderSet.Named<Block>> flowersTag =
                BuiltInRegistries.BLOCK.getTag(MYSTICAL_FLOWERS);
        Optional<HolderSet.Named<Block>> mushroomsTag =
                BuiltInRegistries.BLOCK.getTag(SHIMMERING_MUSHROOMS);
        boolean flowersAvailable = flowersTag.map(holders -> holders.size() > 0).orElse(false);
        boolean mushroomsAvailable = mushroomsTag.map(holders -> holders.size() > 0).orElse(false);

        List<BlockPos> validPositions = new ArrayList<>();
        for (BlockPos candidate : BlockPos.betweenClosed(
                pos.getX() - RANGE, pos.getY() - 2, pos.getZ() - RANGE,
                pos.getX() + RANGE, pos.getY() + 2, pos.getZ() + RANGE)) {
            if (!level.isInWorldBounds(candidate) || !level.isEmptyBlock(candidate)) {
                continue;
            }
            BlockState below = level.getBlockState(candidate.below());
            if (flowersAvailable && canPlaceFlower(below, level)
                    || mushroomsAvailable && canPlaceMushroom(below)) {
                validPositions.add(candidate.immutable());
            }
        }

        int attempts = level.random.nextIntBetweenInclusive(5, 7);
        while (attempts > 0 && !validPositions.isEmpty()) {
            attempts--;
            BlockPos chosen = validPositions.remove(level.random.nextInt(validPositions.size()));
            BlockState below = level.getBlockState(chosen.below());
            boolean tryFlower = flowersAvailable && canPlaceFlower(below, level);
            boolean tryMushroom = mushroomsAvailable && canPlaceMushroom(below);

            Optional<Holder<Block>> toPlace;
            if (tryMushroom && (!tryFlower || level.random.nextInt(3) == 0)) {
                toPlace = mushroomsTag.get().getRandomElement(level.random);
            } else if (tryFlower) {
                toPlace = flowersTag.get().getRandomElement(level.random);
                attempts--;
            } else {
                continue;
            }

            if (toPlace.isPresent() && !toPlace.get().value().defaultBlockState().isAir()) {
                level.setBlockAndUpdate(chosen, toPlace.get().value().defaultBlockState());
            }
        }

        return InteractionResult.CONSUME;
    }

    private static boolean canPlaceFlower(BlockState below, Level level) {
        return below.is(BlockTags.DIRT) && !level.dimensionType().ultraWarm();
    }

    private static boolean canPlaceMushroom(BlockState below) {
        return below.is(BlockTags.MUSHROOM_GROW_BLOCK);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip,
                                TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.infwhat_botania.everlasting_fertilizer")
                .withStyle(ChatFormatting.GRAY));
    }
}
