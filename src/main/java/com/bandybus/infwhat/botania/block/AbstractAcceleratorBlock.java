package com.bandybus.infwhat.botania.block;

import com.bandybus.infwhat.botania.block.entity.AbstractAcceleratorTileEntity;
import com.bandybus.infwhat.botania.container.AcceleratorContainer;
import com.bandybus.infwhat.botania.device.AcceleratorKind;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.ContainerBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkHooks;

/** Shared block behaviour for the three accelerators. */
public abstract class AbstractAcceleratorBlock extends ContainerBlock {

    protected AbstractAcceleratorBlock(Properties properties) {
        super(properties);
    }

    public abstract AcceleratorKind kind();

    @Override
    public BlockRenderType getRenderShape(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    public VoxelShape getShape(BlockState state, IBlockReader level, BlockPos pos,
                               ISelectionContext context) {
        return Block.box(0, 0, 0, 16, 16, 16);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, IBlockReader level, BlockPos pos,
                                        ISelectionContext context) {
        return Block.box(0, 0, 0, 16, 16, 16);
    }

    @Override
    public ActionResultType use(BlockState state, World world, BlockPos pos,
                                PlayerEntity player, Hand hand, BlockRayTraceResult hit) {
        if (world.isClientSide) {
            return ActionResultType.SUCCESS;
        }
        if (!(world.getBlockEntity(pos) instanceof AbstractAcceleratorTileEntity)) {
            return ActionResultType.PASS;
        }
        AbstractAcceleratorTileEntity accelerator =
                (AbstractAcceleratorTileEntity) world.getBlockEntity(pos);
        if (player.isShiftKeyDown()) {
            accelerator.toggleEnabled();
            player.displayClientMessage(new TranslationTextComponent(accelerator.isEnabled()
                    ? "message.infwhat_botania.accelerator.on"
                    : "message.infwhat_botania.accelerator.off"), true);
        } else if (player instanceof ServerPlayerEntity) {
            AcceleratorContainer.open((ServerPlayerEntity) player, pos, kind());
        }
        return ActionResultType.CONSUME;
    }
}
