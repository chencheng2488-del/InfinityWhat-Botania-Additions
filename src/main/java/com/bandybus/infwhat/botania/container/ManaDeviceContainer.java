package com.bandybus.infwhat.botania.container;

import com.bandybus.infwhat.botania.device.DeviceKind;
import com.bandybus.infwhat.botania.device.DeviceState;
import com.bandybus.infwhat.botania.device.ManaDeviceHost;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.SimpleNamedContainerProvider;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.network.NetworkHooks;

/** Slot-less container backing the device GUI; all state is synced through packets. */
public class ManaDeviceContainer extends Container {

    private final BlockPos pos;
    private final DeviceKind kind;
    private DeviceState state;

    public static void open(ServerPlayerEntity player, BlockPos pos, DeviceKind kind) {
        NetworkHooks.openGui(player, new SimpleNamedContainerProvider(
                (windowId, inventory, p) -> new ManaDeviceContainer(windowId, inventory, pos, kind),
                kind.title()), buffer -> {
            buffer.writeBlockPos(pos);
            buffer.writeByte(kind.ordinal());
        });
    }

    public ManaDeviceContainer(int windowId, PlayerInventory inventory, PacketBuffer data) {
        this(windowId, inventory, data.readBlockPos(),
                DeviceKind.values()[data.readByte()]);
    }

    public ManaDeviceContainer(int windowId, PlayerInventory inventory, BlockPos pos,
                               DeviceKind kind) {
        super(ModContainers.DEVICE.get(), windowId);
        this.pos = pos;
        this.kind = kind;
    }

    public BlockPos pos() {
        return pos;
    }

    public DeviceKind kind() {
        return kind;
    }

    public DeviceState state() {
        return state;
    }

    public void applyState(DeviceState state) {
        this.state = state;
    }

    public boolean matches(BlockPos pos) {
        return this.pos.equals(pos);
    }

    @Override
    public boolean stillValid(PlayerEntity player) {
        return player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= 64.0D
                && player.level.getBlockEntity(pos) instanceof ManaDeviceHost;
    }

    @Override
    public ItemStack quickMoveStack(PlayerEntity player, int index) {
        return ItemStack.EMPTY;
    }
}
