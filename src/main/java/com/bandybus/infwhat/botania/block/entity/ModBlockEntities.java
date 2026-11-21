package com.bandybus.infwhat.botania.block.entity;

import com.bandybus.infwhat.botania.InfinityWhatBotania;
import com.bandybus.infwhat.botania.block.ModBlocks;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntityType;

/** Block-entity registration for the Botania Additions. */
public final class ModBlockEntities {

    public static BlockEntityType<ManaWellBlockEntity> MANA_WELL;
    public static BlockEntityType<ManaBeaconBlockEntity> MANA_BEACON;
    public static BlockEntityType<ManaAnchorBlockEntity> MANA_ANCHOR;
    public static BlockEntityType<RuneAcceleratorBlockEntity> RUNE_ACCELERATOR;
    public static BlockEntityType<TerrasteelAcceleratorBlockEntity> TERRASTEEL_ACCELERATOR;
    public static BlockEntityType<FloralOverclockBlockEntity> FLORAL_OVERCLOCK;
    public static BlockEntityType<GaiaBeaconBlockEntity> GAIA_BEACON;

    public static void register() {
        MANA_WELL = register("mana_well",
                FabricBlockEntityTypeBuilder.create(ManaWellBlockEntity::new, ModBlocks.MANA_WELL));
        MANA_BEACON = register("mana_beacon",
                FabricBlockEntityTypeBuilder.create(ManaBeaconBlockEntity::new,
                        ModBlocks.MANA_BEACON));
        MANA_ANCHOR = register("mana_anchor",
                FabricBlockEntityTypeBuilder.create(ManaAnchorBlockEntity::new,
                        ModBlocks.MANA_ANCHOR));
        RUNE_ACCELERATOR = register("rune_accelerator",
                FabricBlockEntityTypeBuilder.create(RuneAcceleratorBlockEntity::new,
                        ModBlocks.RUNE_ACCELERATOR));
        TERRASTEEL_ACCELERATOR = register("terrasteel_accelerator",
                FabricBlockEntityTypeBuilder.create(TerrasteelAcceleratorBlockEntity::new,
                        ModBlocks.TERRASTEEL_ACCELERATOR));
        FLORAL_OVERCLOCK = register("floral_overclock",
                FabricBlockEntityTypeBuilder.create(FloralOverclockBlockEntity::new,
                        ModBlocks.FLORAL_OVERCLOCK));
        GAIA_BEACON = register("gaia_beacon",
                FabricBlockEntityTypeBuilder.create(GaiaBeaconBlockEntity::new,
                        ModBlocks.GAIA_BEACON));
    }

    private static <T extends net.minecraft.world.level.block.entity.BlockEntity>
            BlockEntityType<T> register(
                    String name, FabricBlockEntityTypeBuilder<T> builder) {
        return Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
                new ResourceLocation(InfinityWhatBotania.MOD_ID, name), builder.build());
    }

    private ModBlockEntities() {
    }
}
