package com.bandybus.infwhat.botania.block;

import com.bandybus.infwhat.botania.block.entity.GaiaBeaconBlockEntity;
import com.bandybus.infwhat.botania.block.entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Finishes an ongoing Gaia fight once the boss bar is ready.
 *
 * <p>It never summons the guardian itself; use the Gaia Charm for that.
 */
public class GaiaBeaconBlock extends BaseEntityBlock {

    public GaiaBeaconBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GaiaBeaconBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        if (type != ModBlockEntities.GAIA_BEACON) {
            return null;
        }
        return (tickLevel, tickPos, tickState, blockEntity) ->
                GaiaBeaconBlockEntity.serverTick(tickLevel, tickPos, tickState,
                        (GaiaBeaconBlockEntity) blockEntity);
    }
}
