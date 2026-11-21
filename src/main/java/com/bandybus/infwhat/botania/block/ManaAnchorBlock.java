package com.bandybus.infwhat.botania.block;

import com.bandybus.infwhat.botania.block.entity.ManaAnchorTileEntity;
import com.bandybus.infwhat.botania.device.DeviceKind;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.world.IBlockReader;

/** The receiving half of the wireless mana bridge. */
public class ManaAnchorBlock extends AbstractManaEndpointBlock {

    /** Matches the block model element by element, so the rune plate has no invisible volume. */
    private static final VoxelShape SHAPE = VoxelShapes.or(
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
    public VoxelShape getShape(BlockState state, IBlockReader level, BlockPos pos,
                               ISelectionContext context) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, IBlockReader level, BlockPos pos,
                                        ISelectionContext context) {
        return SHAPE;
    }

    @Override
    public TileEntity newBlockEntity(IBlockReader world) {
        return new ManaAnchorTileEntity();
    }
}
