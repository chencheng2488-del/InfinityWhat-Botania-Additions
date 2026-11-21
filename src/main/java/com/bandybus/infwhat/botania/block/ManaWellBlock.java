package com.bandybus.infwhat.botania.block;

import com.bandybus.infwhat.botania.block.entity.ManaWellBlockEntity;
import com.bandybus.infwhat.botania.block.entity.ModBlockEntities;
import com.bandybus.infwhat.botania.device.DeviceKind;
import com.bandybus.infwhat.botania.grid.ManaNetworkData;
import com.bandybus.infwhat.botania.item.ModItems;
import com.bandybus.infwhat.botania.menu.ManaDeviceMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/** The Mana Well block. Right-click opens the GUI; sneak-right-click toggles output. */
public class ManaWellBlock extends BaseEntityBlock {

    /** Matches the block model element by element, so the well has no invisible volume. */
    private static final VoxelShape SHAPE = Shapes.or(
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
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
                                        CollisionContext context) {
        return SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ManaWellBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.MANA_WELL.get(),
                ManaWellBlockEntity::serverTick);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState,
                         boolean isMoving) {
        if (!state.is(newState.getBlock()) && level instanceof ServerLevel serverLevel) {
            ManaNetworkData.get(serverLevel).removeNode(serverLevel, pos);
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (player.getItemInHand(hand).is(ModItems.CHANNEL_LINKER.get())) {
            return InteractionResult.PASS;
        }
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide && level.getBlockEntity(pos) instanceof ManaWellBlockEntity well) {
                boolean enabled = well.toggleEnabled();
                player.displayClientMessage(new net.minecraft.network.chat.TranslatableComponent(enabled
                        ? "message.infwhat_botania.mana_well.on"
                        : "message.infwhat_botania.mana_well.off"), true);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            ManaDeviceMenu.open(serverPlayer, pos, DeviceKind.WELL);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
