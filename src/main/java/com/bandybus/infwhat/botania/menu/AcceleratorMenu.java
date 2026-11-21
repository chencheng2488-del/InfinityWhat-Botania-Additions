package com.bandybus.infwhat.botania.menu;

import com.bandybus.infwhat.botania.block.entity.AbstractAcceleratorBlockEntity;
import com.bandybus.infwhat.botania.device.AcceleratorKind;
import com.bandybus.infwhat.botania.device.AcceleratorState;
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

/** Slot-less menu backing the accelerator GUI; all state is synced through packets. */
public class AcceleratorMenu extends AbstractContainerMenu {

    private final BlockPos pos;
    private final AcceleratorKind kind;
    private AcceleratorState state;

    public static void open(ServerPlayer player, BlockPos pos, AcceleratorKind kind) {
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
                return new AcceleratorMenu(windowId, inventory, pos, kind);
            }
        });
    }

    public AcceleratorMenu(int windowId, Inventory inventory, FriendlyByteBuf data) {
        this(windowId, inventory, data.readBlockPos(),
                AcceleratorKind.values()[data.readByte()]);
    }

    public AcceleratorMenu(int windowId, Inventory inventory, BlockPos pos,
                           AcceleratorKind kind) {
        super(ModMenus.ACCELERATOR, windowId);
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
    public boolean stillValid(Player player) {
        return player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= 64.0D
                && player.level().getBlockEntity(pos) instanceof AbstractAcceleratorBlockEntity;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}
