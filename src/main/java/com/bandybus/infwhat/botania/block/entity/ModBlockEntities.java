package com.bandybus.infwhat.botania.block.entity;

import com.bandybus.infwhat.botania.InfinityWhatBotania;
import com.bandybus.infwhat.botania.block.ModBlocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Block-entity registration for the Botania Additions. */
public final class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITIES, InfinityWhatBotania.MOD_ID);

    public static final RegistryObject<BlockEntityType<ManaWellBlockEntity>> MANA_WELL =
            BLOCK_ENTITIES.register("mana_well", () -> BlockEntityType.Builder.of(
                    ManaWellBlockEntity::new, ModBlocks.MANA_WELL.get()).build(null));

    public static final RegistryObject<BlockEntityType<ManaBeaconBlockEntity>> MANA_BEACON =
            BLOCK_ENTITIES.register("mana_beacon", () -> BlockEntityType.Builder.of(
                    ManaBeaconBlockEntity::new, ModBlocks.MANA_BEACON.get()).build(null));

    public static final RegistryObject<BlockEntityType<ManaAnchorBlockEntity>> MANA_ANCHOR =
            BLOCK_ENTITIES.register("mana_anchor", () -> BlockEntityType.Builder.of(
                    ManaAnchorBlockEntity::new, ModBlocks.MANA_ANCHOR.get()).build(null));

    public static final RegistryObject<BlockEntityType<RuneAcceleratorBlockEntity>>
            RUNE_ACCELERATOR = BLOCK_ENTITIES.register("rune_accelerator",
                    () -> BlockEntityType.Builder.of(RuneAcceleratorBlockEntity::new,
                            ModBlocks.RUNE_ACCELERATOR.get()).build(null));

    public static final RegistryObject<BlockEntityType<TerrasteelAcceleratorBlockEntity>>
            TERRASTEEL_ACCELERATOR = BLOCK_ENTITIES.register("terrasteel_accelerator",
                    () -> BlockEntityType.Builder.of(TerrasteelAcceleratorBlockEntity::new,
                            ModBlocks.TERRASTEEL_ACCELERATOR.get()).build(null));

    public static final RegistryObject<BlockEntityType<FloralOverclockBlockEntity>>
            FLORAL_OVERCLOCK = BLOCK_ENTITIES.register("floral_overclock",
                    () -> BlockEntityType.Builder.of(FloralOverclockBlockEntity::new,
                            ModBlocks.FLORAL_OVERCLOCK.get()).build(null));

    public static final RegistryObject<BlockEntityType<GaiaBeaconBlockEntity>>
            GAIA_BEACON = BLOCK_ENTITIES.register("gaia_beacon",
                    () -> BlockEntityType.Builder.of(GaiaBeaconBlockEntity::new,
                            ModBlocks.GAIA_BEACON.get()).build(null));

    private ModBlockEntities() {
    }

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}
