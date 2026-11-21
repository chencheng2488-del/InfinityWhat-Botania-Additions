package com.bandybus.infwhat.botania.container;

import com.bandybus.infwhat.botania.block.entity.AbstractAcceleratorTileEntity;
import com.bandybus.infwhat.botania.device.AcceleratorKind;
import com.bandybus.infwhat.botania.device.AcceleratorState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.SimpleNamedContainerProvider;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.network.NetworkHooks;

/** Slot-less container backing the accelerator GUI; all state is synced through packets. */
public class AcceleratorContainer extends Container {

    private final BlockPos pos;
    private final AcceleratorKind kind;
    private AcceleratorState state;

    public static void open(ServerPlayerEntity player, BlockPos pos, AcceleratorKind kind) {
        NetworkHooks.openGui(player, new SimpleNamedContainerProvider(
                (windowId, inventory, p) -> new AcceleratorContainer(windowId, inventory, pos, kind),
                kind.title()), buffer -> {
            buffer.writeBlockPos(pos);
            buffer.writeByte(kind.ordinal());
        });
    }

    public AcceleratorContainer(int windowId, PlayerInventory inventory, PacketBuffer data) {
        this(windowId, inventory, data.readBlockPos(),
                AcceleratorKind.values()[data.readByte()]);
    }

    public AcceleratorContainer(int windowId, PlayerInventory inventory, BlockPos pos,
                                AcceleratorKind kind) {
        super(ModContainers.ACCELERATOR.get(), windowId);
        this.pos = pos;
        this.kind = kind;
    }

    public BlockPos pos() {
        return pos;
    }

    public AcceleratorKind kind() {
        return kind;
    }

    public AcceleratorState state() {
        return state;
    }

    public void applyState(AcceleratorState state) {
        this.state = state;
    }

    public boolean matches(BlockPos pos) {
        return this.pos.equals(pos);
    }

    @Override
    public boolean stillValid(PlayerEntity player) {
        return player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= 64.0D
                && player.level.getBlockEntity(pos) instanceof AbstractAcceleratorTileEntity;
    }

    @Override
    public ItemStack quickMoveStack(PlayerEntity player, int index) {
        return ItemStack.EMPTY;
    }
}
