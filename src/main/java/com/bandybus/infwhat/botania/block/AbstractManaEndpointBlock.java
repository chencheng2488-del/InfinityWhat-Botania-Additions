package com.bandybus.infwhat.botania.block;

import com.bandybus.infwhat.botania.block.entity.AbstractManaEndpointBlockEntity;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/** Shared block behaviour for the two wireless endpoints. */
public abstract class AbstractManaEndpointBlock extends BaseEntityBlock {

    protected AbstractManaEndpointBlock(Properties properties) {
        super(properties);
    }

    protected abstract DeviceKind kind();

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state,
                            @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && placer instanceof Player player
                && level.getBlockEntity(pos) instanceof AbstractManaEndpointBlockEntity endpoint) {
            endpoint.setOwner(player.getUUID());
        }
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
            toggle(level, pos, player);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            if (level.getBlockEntity(pos) instanceof AbstractManaEndpointBlockEntity endpoint
                    && endpoint.getOwnerId() != null && !endpoint.isOwnedBy(player.getUUID())) {
                player.displayClientMessage(
                        Component.translatable("gui.infwhat_botania.not_owner"), true);
            } else {
                ManaDeviceMenu.open(serverPlayer, pos, kind());
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    protected void toggle(Level level, BlockPos pos, Player player) {
        if (!level.isClientSide
                && level.getBlockEntity(pos) instanceof AbstractManaEndpointBlockEntity endpoint) {
            boolean enabled = endpoint.toggleEnabled();
            player.displayClientMessage(Component.translatable(enabled
                    ? "message.infwhat_botania.endpoint.on"
                    : "message.infwhat_botania.endpoint.off"), true);
        }
    }
}
