package com.bandybus.infwhat.botania.block;

import com.bandybus.infwhat.botania.block.entity.ManaBeaconBlockEntity;
import com.bandybus.infwhat.botania.block.entity.ModBlockEntities;
import com.bandybus.infwhat.botania.device.DeviceKind;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/** The beacon half of the wireless mana bridge. */
public class ManaBeaconBlock extends AbstractManaEndpointBlock {

    /** Matches the block model element by element, so the lamp has no invisible volume. */
    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(0, 0, 0, 16, 3, 16),
            Block.box(2, 3, 2, 14, 5, 14),
            Block.box(6, 5, 6, 10, 12, 10),
            Block.box(3, 12, 3, 13, 13, 13),
            Block.box(5, 13, 5, 11, 16, 11),
            Block.box(5, 7, 5, 11, 8, 11),
            Block.box(5, 10, 5, 11, 11, 11),
            Block.box(3, 13, 3, 5, 16, 5),
            Block.box(11, 13, 3, 13, 16, 5),
            Block.box(3, 13, 11, 5, 16, 13),
            Block.box(11, 13, 11, 13, 16, 13));

    public ManaBeaconBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected DeviceKind kind() {
        return DeviceKind.BEACON;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
                                        CollisionContext context) {
        return SHAPE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ManaBeaconBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        if (type != ModBlockEntities.MANA_BEACON) {
            return null;
        }
        return (tickLevel, tickPos, tickState, blockEntity) ->
                ManaBeaconBlockEntity.serverTick(tickLevel, tickPos, tickState,
                        (ManaBeaconBlockEntity) blockEntity);
    }
}
