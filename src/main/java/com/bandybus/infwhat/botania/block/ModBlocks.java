package com.bandybus.infwhat.botania.block;

import com.bandybus.infwhat.botania.InfinityWhatBotania;
import com.bandybus.infwhat.botania.item.GaiaBeaconBlockItem;
import com.bandybus.infwhat.botania.item.ModItems;
import com.bandybus.infwhat.botania.item.TooltipBlockItem;
import com.bandybus.infwhat.botania.util.CreativeTabHelper;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.material.MaterialColor;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraftforge.common.ToolType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.function.Function;
import java.util.function.Supplier;

/** Block and block-item registration for the Botania Additions. */
public final class ModBlocks {

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, InfinityWhatBotania.MOD_ID);

    public static final RegistryObject<Block> MANA_WELL = registerBlock("mana_well",
            () -> new ManaWellBlock(properties(Material.STONE, MaterialColor.COLOR_CYAN, 15)
                    .noOcclusion()));

    public static final RegistryObject<Block> MANA_BEACON = registerBlock("mana_beacon",
            () -> new ManaBeaconBlock(properties(Material.STONE, MaterialColor.COLOR_LIGHT_BLUE, 12)
                    .noOcclusion()));

    public static final RegistryObject<Block> MANA_ANCHOR = registerBlock("mana_anchor",
            () -> new ManaAnchorBlock(properties(Material.STONE, MaterialColor.COLOR_BLUE, 8)
                    .noOcclusion()));

    public static final RegistryObject<Block> RUNE_ACCELERATOR = registerBlock("rune_accelerator",
            () -> new RuneAcceleratorBlock(properties(Material.STONE, MaterialColor.COLOR_CYAN, 9)),
            "tooltip.infwhat_botania.rune_accelerator");

    public static final RegistryObject<Block> TERRASTEEL_ACCELERATOR =
            registerBlock("terrasteel_accelerator",
                    () -> new TerrasteelAcceleratorBlock(
                            properties(Material.STONE, MaterialColor.COLOR_BLUE, 9)),
                    "tooltip.infwhat_botania.terrasteel_accelerator");

    public static final RegistryObject<Block> FLORAL_OVERCLOCK =
            registerBlock("floral_overclock",
                    () -> new FloralOverclockBlock(AbstractBlock.Properties
                            .of(Material.WOOD, MaterialColor.PLANT)
                            .strength(2.0F, 6.0F)
                            .sound(SoundType.WOOD)
                            .lightLevel(state -> 7)
                            .harvestTool(ToolType.AXE)
                            .harvestLevel(0)
                            .requiresCorrectToolForDrops()),
                    "tooltip.infwhat_botania.floral_overclock");

    public static final RegistryObject<Block> GAIA_BEACON = registerBlock("gaia_beacon",
            () -> new GaiaBeaconBlock(properties(Material.STONE, MaterialColor.COLOR_PURPLE, 12)),
            block -> new GaiaBeaconBlockItem(block, new Item.Properties().stacksTo(1)
                    .tab(CreativeTabHelper.coreTab())));

    private ModBlocks() {
    }

    private static AbstractBlock.Properties properties(Material material, MaterialColor color,
                                                       int light) {
        return AbstractBlock.Properties.of(material, color)
                .strength(2.0F, 6.0F)
                .sound(SoundType.STONE)
                .lightLevel(state -> light)
                .harvestTool(ToolType.PICKAXE)
                .harvestLevel(0)
                .requiresCorrectToolForDrops();
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
                ? new BlockItem(registered.get(), new Item.Properties().stacksTo(1)
                        .tab(CreativeTabHelper.coreTab()))
                : new TooltipBlockItem(registered.get(), new Item.Properties().stacksTo(1)
                        .tab(CreativeTabHelper.coreTab()), tooltipKey));
        return registered;
    }

    private static <T extends Block> RegistryObject<T> registerBlock(String name,
                                                                     Supplier<T> block,
                                                                     Function<Block, Item> factory) {
        RegistryObject<T> registered = BLOCKS.register(name, block);
        ModItems.ITEMS.register(name, () -> factory.apply(registered.get()));
        return registered;
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
