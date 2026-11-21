package com.bandybus.infwhat.botania.block;

import com.bandybus.infwhat.botania.block.entity.AbstractManaEndpointTileEntity;
import com.bandybus.infwhat.botania.container.ManaDeviceContainer;
import com.bandybus.infwhat.botania.device.DeviceKind;
import com.bandybus.infwhat.botania.grid.ManaNetworkData;
import com.bandybus.infwhat.botania.item.ModItems;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.ContainerBlock;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

import javax.annotation.Nullable;

/** Shared block behaviour for the two wireless endpoints. */
public abstract class AbstractManaEndpointBlock extends ContainerBlock {

    protected AbstractManaEndpointBlock(Properties properties) {
        super(properties);
    }

    protected abstract DeviceKind kind();

    @Override
    public BlockRenderType getRenderShape(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    public void setPlacedBy(World world, BlockPos pos, BlockState state,
                            @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(world, pos, state, placer, stack);
        if (!world.isClientSide && placer instanceof PlayerEntity
                && world.getBlockEntity(pos) instanceof AbstractManaEndpointTileEntity) {
            ((AbstractManaEndpointTileEntity) world.getBlockEntity(pos))
                    .setOwner(placer.getUUID());
        }
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
            toggle(world, pos, player);
            return ActionResultType.sidedSuccess(world.isClientSide);
        }
        if (!world.isClientSide && player instanceof ServerPlayerEntity) {
            if (world.getBlockEntity(pos) instanceof AbstractManaEndpointTileEntity) {
                AbstractManaEndpointTileEntity endpoint =
                        (AbstractManaEndpointTileEntity) world.getBlockEntity(pos);
                if (endpoint.getOwnerId() != null && !endpoint.isOwnedBy(player.getUUID())) {
                    player.displayClientMessage(
                            new TranslationTextComponent("gui.infwhat_botania.not_owner"), true);
                } else {
                    ManaDeviceContainer.open((ServerPlayerEntity) player, pos, kind());
                }
            }
        }
        return ActionResultType.sidedSuccess(world.isClientSide);
    }

    protected void toggle(World world, BlockPos pos, PlayerEntity player) {
        if (!world.isClientSide
                && world.getBlockEntity(pos) instanceof AbstractManaEndpointTileEntity) {
            AbstractManaEndpointTileEntity endpoint =
                    (AbstractManaEndpointTileEntity) world.getBlockEntity(pos);
            boolean enabled = endpoint.toggleEnabled();
            player.displayClientMessage(new TranslationTextComponent(enabled
                    ? "message.infwhat_botania.endpoint.on"
                    : "message.infwhat_botania.endpoint.off"), true);
        }
    }
}
