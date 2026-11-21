package com.bandybus.infwhat.botania.menu;

import com.bandybus.infwhat.botania.device.DeviceKind;
import com.bandybus.infwhat.botania.device.DeviceState;
import com.bandybus.infwhat.botania.device.ManaDeviceHost;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/** Slot-less menu backing the device GUI; all state is synced through packets. */
public class ManaDeviceMenu extends AbstractContainerMenu {

    private final BlockPos pos;
    private final DeviceKind kind;
    private DeviceState state;

    public static void open(ServerPlayer player, BlockPos pos, DeviceKind kind) {
        player.openMenu(new ExtendedScreenHandlerFactory() {
            @Override
            public void writeScreenOpeningData(ServerPlayer player, FriendlyByteBuf buffer) {
                buffer.writeBlockPos(pos);
                buffer.writeByte(kind.ordinal());
            }

            @Override
            public Component getDisplayName() {
                return kind.title();
            }

            @Nullable
            @Override
            public AbstractContainerMenu createMenu(int windowId, Inventory inventory,
                                                    Player player) {
                return new ManaDeviceMenu(windowId, inventory, pos, kind);
            }
        });
    }

    public ManaDeviceMenu(int windowId, Inventory inventory, FriendlyByteBuf data) {
        this(windowId, inventory, data.readBlockPos(), DeviceKind.values()[data.readByte()]);
    }

    public ManaDeviceMenu(int windowId, Inventory inventory, BlockPos pos, DeviceKind kind) {
        super(ModMenus.DEVICE, windowId);
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
    public boolean stillValid(Player player) {
        return player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= 64.0D
                && player.level().getBlockEntity(pos) instanceof ManaDeviceHost;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}
