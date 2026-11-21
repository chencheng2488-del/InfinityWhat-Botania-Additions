package com.bandybus.infwhat.botania.block;

import com.bandybus.infwhat.botania.InfinityWhatBotania;
import com.bandybus.infwhat.botania.item.GaiaBeaconBlockItem;
import com.bandybus.infwhat.botania.item.ModItems;
import com.bandybus.infwhat.botania.item.TooltipBlockItem;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;
import java.util.function.Supplier;

/** Block and block-item registration for the Botania Additions. */
public final class ModBlocks {

    public static Block MANA_WELL;
    public static Block MANA_BEACON;
    public static Block MANA_ANCHOR;
    public static Block RUNE_ACCELERATOR;
    public static Block TERRASTEEL_ACCELERATOR;
    public static Block FLORAL_OVERCLOCK;
    public static Block GAIA_BEACON;

    public static void register() {
        MANA_WELL = registerBlock("mana_well",
                () -> new ManaWellBlock(BlockBehaviour.Properties.of()
                        .mapColor(MapColor.COLOR_CYAN)
                        .strength(2.0F, 6.0F)
                        .sound(SoundType.STONE)
                        .lightLevel(state -> 15)
                        .noOcclusion()
                        .requiresCorrectToolForDrops()));

        MANA_BEACON = registerBlock("mana_beacon",
                () -> new ManaBeaconBlock(BlockBehaviour.Properties.of()
                        .mapColor(MapColor.COLOR_LIGHT_BLUE)
                        .strength(2.0F, 6.0F)
                        .sound(SoundType.STONE)
                        .lightLevel(state -> 12)
                        .noOcclusion()
                        .requiresCorrectToolForDrops()));

        MANA_ANCHOR = registerBlock("mana_anchor",
                () -> new ManaAnchorBlock(BlockBehaviour.Properties.of()
                        .mapColor(MapColor.COLOR_BLUE)
                        .strength(2.0F, 6.0F)
                        .sound(SoundType.STONE)
                        .lightLevel(state -> 8)
                        .noOcclusion()
                        .requiresCorrectToolForDrops()));

        RUNE_ACCELERATOR = registerBlock("rune_accelerator",
                () -> new RuneAcceleratorBlock(BlockBehaviour.Properties.of()
                        .mapColor(MapColor.COLOR_CYAN)
                        .strength(2.0F, 6.0F)
                        .sound(SoundType.STONE)
                        .lightLevel(state -> 9)
                        .requiresCorrectToolForDrops()),
                "tooltip.infwhat_botania.rune_accelerator");

        TERRASTEEL_ACCELERATOR = registerBlock("terrasteel_accelerator",
                () -> new TerrasteelAcceleratorBlock(BlockBehaviour.Properties.of()
                        .mapColor(MapColor.COLOR_BLUE)
                        .strength(2.0F, 6.0F)
                        .sound(SoundType.STONE)
                        .lightLevel(state -> 9)
                        .requiresCorrectToolForDrops()),
                "tooltip.infwhat_botania.terrasteel_accelerator");

        FLORAL_OVERCLOCK = registerBlock("floral_overclock",
                () -> new FloralOverclockBlock(BlockBehaviour.Properties.of()
                        .mapColor(MapColor.PLANT)
                        .strength(2.0F, 6.0F)
                        .sound(SoundType.WOOD)
                        .lightLevel(state -> 7)
                        .requiresCorrectToolForDrops()),
                "tooltip.infwhat_botania.floral_overclock");

        GAIA_BEACON = registerBlock("gaia_beacon",
                () -> new GaiaBeaconBlock(BlockBehaviour.Properties.of()
                        .mapColor(MapColor.COLOR_PURPLE)
                        .strength(2.0F, 6.0F)
                        .sound(SoundType.STONE)
                        .lightLevel(state -> 12)
                        .requiresCorrectToolForDrops()),
                block -> new GaiaBeaconBlockItem(block, new Item.Properties().stacksTo(1)));
    }

    private static <T extends Block> T registerBlock(String name, Supplier<T> block) {
        return registerBlock(name, block, (String) null);
    }

    private static <T extends Block> T registerBlock(String name, Supplier<T> block,
                                                     @Nullable String tooltipKey) {
        T registered = Registry.register(BuiltInRegistries.BLOCK,
                new ResourceLocation(InfinityWhatBotania.MOD_ID, name), block.get());
        ModItems.register(name, tooltipKey == null
                ? new BlockItem(registered, new Item.Properties().stacksTo(1))
                : new TooltipBlockItem(registered, new Item.Properties().stacksTo(1), tooltipKey));
        return registered;
    }

    private static <T extends Block> T registerBlock(String name, Supplier<T> block,
                                                     Function<Block, Item> itemFactory) {
        T registered = Registry.register(BuiltInRegistries.BLOCK,
                new ResourceLocation(InfinityWhatBotania.MOD_ID, name), block.get());
        ModItems.register(name, itemFactory.apply(registered));
        return registered;
    }

    private ModBlocks() {
    }
}
