package com.bandybus.infwhat.botania.block;

import com.bandybus.infwhat.botania.block.entity.ManaWellTileEntity;
import com.bandybus.infwhat.botania.container.ManaDeviceContainer;
import com.bandybus.infwhat.botania.device.DeviceKind;
import com.bandybus.infwhat.botania.grid.ManaNetworkData;
import com.bandybus.infwhat.botania.item.ModItems;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.ContainerBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

/** The Mana Well block. Right-click opens the GUI; sneak-right-click toggles output. */
public class ManaWellBlock extends ContainerBlock {

    /** Matches the block model element by element, so the well has no invisible volume. */
    private static final VoxelShape SHAPE = VoxelShapes.or(
            Block.box(0, 0, 0, 16, 3, 16),
            Block.box(3, 3, 3, 13, 8, 13),
            Block.box(13, 3, 3, 16, 10, 13),
            Block.box(0, 3, 0, 16, 10, 3),
            Block.box(0, 3, 13, 16, 10, 16),
            Block.box(0, 3, 3, 3, 10, 13),
            Block.box(13, 10, 3, 16, 11, 13),
            Block.box(0, 10, 0, 16, 11, 3),
            Block.box(0, 10, 13, 16, 11, 16),
            Block.box(0, 10, 3, 3, 11, 13),
            Block.box(0, 11, 0, 2, 14, 2),
            Block.box(14, 11, 0, 16, 14, 2),
            Block.box(0, 11, 14, 2, 14, 16),
            Block.box(14, 11, 14, 16, 14, 16));

    public ManaWellBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockRenderType getRenderShape(BlockState state) {
        return BlockRenderType.MODEL;
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
        return new ManaWellTileEntity();
    }

    @Override
    public void onRemove(BlockState state, World world, BlockPos pos, BlockState newState,
                         boolean isMoving) {
        if (state.getBlock() != newState.getBlock() && world instanceof ServerWorld) {
            ManaNetworkData data = ManaNetworkData.get(world);
            if (data != null) {
                data.removeNode((ServerWorld) world, pos);
            }
        }
        super.onRemove(state, world, pos, newState, isMoving);
    }

    @Override
    public ActionResultType use(BlockState state, World world, BlockPos pos,
                                PlayerEntity player, Hand hand, BlockRayTraceResult hit) {
        if (player.getItemInHand(hand).getItem() == ModItems.CHANNEL_LINKER.get()) {
            return ActionResultType.PASS;
        }
        if (player.isShiftKeyDown()) {
            if (!world.isClientSide && world.getBlockEntity(pos) instanceof ManaWellTileEntity) {
                ManaWellTileEntity well = (ManaWellTileEntity) world.getBlockEntity(pos);
                boolean enabled = well.toggleEnabled();
                player.displayClientMessage(new TranslationTextComponent(enabled
                        ? "message.infwhat_botania.mana_well.on"
                        : "message.infwhat_botania.mana_well.off"), true);
            }
            return ActionResultType.sidedSuccess(world.isClientSide);
        }
        if (!world.isClientSide && player instanceof ServerPlayerEntity) {
            ManaDeviceContainer.open((ServerPlayerEntity) player, pos, DeviceKind.WELL);
        }
        return ActionResultType.sidedSuccess(world.isClientSide);
    }
}
