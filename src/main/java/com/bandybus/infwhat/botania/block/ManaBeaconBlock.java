package com.bandybus.infwhat.botania.block;

import com.bandybus.infwhat.botania.block.entity.ManaBeaconTileEntity;
import com.bandybus.infwhat.botania.device.DeviceKind;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.world.IBlockReader;

/** The beacon half of the wireless mana bridge. */
public class ManaBeaconBlock extends AbstractManaEndpointBlock {

    /** Matches the block model element by element, so the lamp has no invisible volume. */
    private static final VoxelShape SHAPE = VoxelShapes.or(
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
        return new ManaBeaconTileEntity();
    }
}
