package com.bandybus.infwhat.botania.block;

import com.bandybus.infwhat.botania.InfinityWhatBotania;
import com.bandybus.infwhat.botania.item.GaiaBeaconBlockItem;
import com.bandybus.infwhat.botania.item.ModItems;
import com.bandybus.infwhat.botania.item.TooltipBlockItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;
import java.util.function.Supplier;

/** Block and block-item registration for the Botania Additions. */
public final class ModBlocks {

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, InfinityWhatBotania.MOD_ID);

    public static final RegistryObject<Block> MANA_WELL = registerBlock("mana_well",
            () -> new ManaWellBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_CYAN)
                    .strength(2.0F, 6.0F)
                    .sound(SoundType.STONE)
                    .lightLevel(state -> 15)
                    .noOcclusion()
                    .requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> MANA_BEACON = registerBlock("mana_beacon",
            () -> new ManaBeaconBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(2.0F, 6.0F)
                    .sound(SoundType.STONE)
                    .lightLevel(state -> 12)
                    .noOcclusion()
                    .requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> MANA_ANCHOR = registerBlock("mana_anchor",
            () -> new ManaAnchorBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLUE)
                    .strength(2.0F, 6.0F)
                    .sound(SoundType.STONE)
                    .lightLevel(state -> 8)
                    .noOcclusion()
                    .requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> RUNE_ACCELERATOR = registerBlock("rune_accelerator",
            () -> new RuneAcceleratorBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_CYAN)
                    .strength(2.0F, 6.0F)
                    .sound(SoundType.STONE)
                    .lightLevel(state -> 9)
                    .requiresCorrectToolForDrops()),
            "tooltip.infwhat_botania.rune_accelerator");

    public static final RegistryObject<Block> TERRASTEEL_ACCELERATOR =
            registerBlock("terrasteel_accelerator",
                    () -> new TerrasteelAcceleratorBlock(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_BLUE)
                            .strength(2.0F, 6.0F)
                            .sound(SoundType.STONE)
                            .lightLevel(state -> 9)
                            .requiresCorrectToolForDrops()),
                    "tooltip.infwhat_botania.terrasteel_accelerator");

    public static final RegistryObject<Block> FLORAL_OVERCLOCK =
            registerBlock("floral_overclock",
                    () -> new FloralOverclockBlock(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.PLANT)
                            .strength(2.0F, 6.0F)
                            .sound(SoundType.WOOD)
                            .lightLevel(state -> 7)
                            .requiresCorrectToolForDrops()),
                    "tooltip.infwhat_botania.floral_overclock");

    public static final RegistryObject<Block> GAIA_BEACON =
            registerBlock("gaia_beacon",
                    () -> new GaiaBeaconBlock(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_PURPLE)
                            .strength(2.0F, 6.0F)
                            .sound(SoundType.STONE)
                            .lightLevel(state -> 12)
                            .requiresCorrectToolForDrops()),
                    block -> new GaiaBeaconBlockItem(block, new Item.Properties().stacksTo(1)));

    private ModBlocks() {
    }

    private static <T extends Block> RegistryObject<T> registerBlock(String name,
                                                                     Supplier<T> block) {
        return registerBlock(name, block, (String) null);
    }

    private static <T extends Block> RegistryObject<T> registerBlock(String name,
                                                                     Supplier<T> block,
                                                                     @Nullable String tooltipKey) {
        RegistryObject<T> registered = BLOCKS.register(name, block);
        ModItems.ITEMS.register(name, () -> tooltipKey == null
                ? new BlockItem(registered.get(), new Item.Properties().stacksTo(1))
                : new TooltipBlockItem(registered.get(), new Item.Properties().stacksTo(1),
                tooltipKey));
        return registered;
    }

    private static <T extends Block> RegistryObject<T> registerBlock(String name,
                                                                     Supplier<T> block,
                                                                     Function<Block, Item> itemFactory) {
        RegistryObject<T> registered = BLOCKS.register(name, block);
        ModItems.ITEMS.register(name, () -> itemFactory.apply(registered.get()));
        return registered;
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
