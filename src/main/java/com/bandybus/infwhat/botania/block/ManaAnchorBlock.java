package com.bandybus.infwhat.botania.block;

import com.bandybus.infwhat.botania.block.entity.ManaAnchorBlockEntity;
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

/** The receiving half of the wireless mana bridge. */
public class ManaAnchorBlock extends AbstractManaEndpointBlock {

    /** Matches the block model element by element, so the rune plate has no invisible volume. */
    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(0, 0, 0, 16, 3, 16),
            Block.box(1, 3, 1, 15, 4, 15),
            Block.box(7, 4, 7, 9, 7, 9),
            Block.box(1, 4, 1, 3, 9, 3),
            Block.box(13, 4, 1, 15, 9, 3),
            Block.box(1, 4, 13, 3, 9, 15),
            Block.box(13, 4, 13, 15, 9, 15),
            Block.box(5, 4, 4, 11, 5, 6),
            Block.box(5, 4, 10, 11, 5, 12),
            Block.box(4, 4, 6, 6, 5, 10),
            Block.box(10, 4, 6, 12, 5, 10));

    public ManaAnchorBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected DeviceKind kind() {
        return DeviceKind.ANCHOR;
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
        return new ManaAnchorBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.MANA_ANCHOR.get(),
                ManaAnchorBlockEntity::serverTick);
    }
}
