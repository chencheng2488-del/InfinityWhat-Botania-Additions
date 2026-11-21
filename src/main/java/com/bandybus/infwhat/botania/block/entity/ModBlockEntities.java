package com.bandybus.infwhat.botania.block.entity;

import com.bandybus.infwhat.botania.InfinityWhatBotania;
import com.bandybus.infwhat.botania.block.ModBlocks;
import net.minecraft.tileentity.TileEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

/** Tile-entity registration for the Botania Additions. */
public final class ModBlockEntities {

    public static final DeferredRegister<TileEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.TILE_ENTITIES, InfinityWhatBotania.MOD_ID);

    public static final RegistryObject<TileEntityType<ManaWellTileEntity>> MANA_WELL =
            BLOCK_ENTITIES.register("mana_well", () -> TileEntityType.Builder.of(
                    ManaWellTileEntity::new, ModBlocks.MANA_WELL.get()).build(null));

    public static final RegistryObject<TileEntityType<ManaBeaconTileEntity>> MANA_BEACON =
            BLOCK_ENTITIES.register("mana_beacon", () -> TileEntityType.Builder.of(
                    ManaBeaconTileEntity::new, ModBlocks.MANA_BEACON.get()).build(null));

    public static final RegistryObject<TileEntityType<ManaAnchorTileEntity>> MANA_ANCHOR =
            BLOCK_ENTITIES.register("mana_anchor", () -> TileEntityType.Builder.of(
                    ManaAnchorTileEntity::new, ModBlocks.MANA_ANCHOR.get()).build(null));

    public static final RegistryObject<TileEntityType<RuneAcceleratorTileEntity>>
            RUNE_ACCELERATOR = BLOCK_ENTITIES.register("rune_accelerator",
                    () -> TileEntityType.Builder.of(RuneAcceleratorTileEntity::new,
                            ModBlocks.RUNE_ACCELERATOR.get()).build(null));

    public static final RegistryObject<TileEntityType<TerrasteelAcceleratorTileEntity>>
            TERRASTEEL_ACCELERATOR = BLOCK_ENTITIES.register("terrasteel_accelerator",
                    () -> TileEntityType.Builder.of(TerrasteelAcceleratorTileEntity::new,
                            ModBlocks.TERRASTEEL_ACCELERATOR.get()).build(null));

    public static final RegistryObject<TileEntityType<FloralOverclockTileEntity>>
            FLORAL_OVERCLOCK = BLOCK_ENTITIES.register("floral_overclock",
                    () -> TileEntityType.Builder.of(FloralOverclockTileEntity::new,
                            ModBlocks.FLORAL_OVERCLOCK.get()).build(null));

    public static final RegistryObject<TileEntityType<GaiaBeaconTileEntity>>
            GAIA_BEACON = BLOCK_ENTITIES.register("gaia_beacon",
                    () -> TileEntityType.Builder.of(GaiaBeaconTileEntity::new,
                            ModBlocks.GAIA_BEACON.get()).build(null));

    private ModBlockEntities() {
    }

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}
