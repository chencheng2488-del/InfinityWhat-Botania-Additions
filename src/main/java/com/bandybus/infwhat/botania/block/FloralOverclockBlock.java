package com.bandybus.infwhat.botania.block;

import com.bandybus.infwhat.botania.block.entity.FloralOverclockBlockEntity;
import com.bandybus.infwhat.botania.block.entity.ModBlockEntities;
import com.bandybus.infwhat.botania.device.AcceleratorKind;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** Ticks nearby generating flowers multiple times per game tick. */
public class FloralOverclockBlock extends AbstractAcceleratorBlock {

    public FloralOverclockBlock(Properties properties) {
        super(properties);
    }

    @Override
    public AcceleratorKind kind() {
        return AcceleratorKind.FLORAL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FloralOverclockBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        if (type != ModBlockEntities.FLORAL_OVERCLOCK) {
            return null;
        }
        return (tickLevel, tickPos, tickState, blockEntity) ->
                FloralOverclockBlockEntity.serverTick(tickLevel, tickPos, tickState,
                        (FloralOverclockBlockEntity) blockEntity);
    }
}
