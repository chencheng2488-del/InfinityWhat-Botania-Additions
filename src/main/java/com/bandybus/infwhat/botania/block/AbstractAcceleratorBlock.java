package com.bandybus.infwhat.botania.block;

import com.bandybus.infwhat.botania.block.entity.AbstractAcceleratorBlockEntity;
import com.bandybus.infwhat.botania.device.AcceleratorKind;
import com.bandybus.infwhat.botania.menu.AcceleratorMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Shared block behaviour for the three accelerators. */
public abstract class AbstractAcceleratorBlock extends BaseEntityBlock {

    protected AbstractAcceleratorBlock(Properties properties) {
        super(properties);
    }

    public abstract AcceleratorKind kind();

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
                               CollisionContext context) {
        return Shapes.block();
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
                                        CollisionContext context) {
        return Shapes.block();
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(level.getBlockEntity(pos) instanceof AbstractAcceleratorBlockEntity accelerator)) {
            return InteractionResult.PASS;
        }
        if (player.isShiftKeyDown()) {
            accelerator.toggleEnabled();
            player.displayClientMessage(Component.translatable(accelerator.isEnabled()
                    ? "message.infwhat_botania.accelerator.on"
                    : "message.infwhat_botania.accelerator.off"), true);
        } else if (player instanceof ServerPlayer serverPlayer) {
            AcceleratorMenu.open(serverPlayer, pos, kind());
        }
        return InteractionResult.CONSUME;
    }
}
