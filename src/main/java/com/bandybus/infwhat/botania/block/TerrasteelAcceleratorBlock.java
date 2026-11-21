package com.bandybus.infwhat.botania.block;

import com.bandybus.infwhat.botania.block.entity.ModBlockEntities;
import com.bandybus.infwhat.botania.block.entity.TerrasteelAcceleratorBlockEntity;
import com.bandybus.infwhat.botania.device.AcceleratorKind;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** Feeds nearby Terrestrial Agglomeration Plates. */
public class TerrasteelAcceleratorBlock extends AbstractAcceleratorBlock {

    public TerrasteelAcceleratorBlock(Properties properties) {
        super(properties);
    }

    @Override
    public AcceleratorKind kind() {
        return AcceleratorKind.TERRASTEEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TerrasteelAcceleratorBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        if (type != ModBlockEntities.TERRASTEEL_ACCELERATOR) {
            return null;
        }
        return (tickLevel, tickPos, tickState, blockEntity) ->
                TerrasteelAcceleratorBlockEntity.serverTick(tickLevel, tickPos, tickState,
                        (TerrasteelAcceleratorBlockEntity) blockEntity);
    }
}
