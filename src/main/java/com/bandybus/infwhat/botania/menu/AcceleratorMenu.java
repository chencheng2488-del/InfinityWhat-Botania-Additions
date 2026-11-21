package com.bandybus.infwhat.botania.menu;

import com.bandybus.infwhat.botania.block.entity.AbstractAcceleratorBlockEntity;
import com.bandybus.infwhat.botania.device.AcceleratorKind;
import com.bandybus.infwhat.botania.device.AcceleratorState;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkHooks;

/** Slot-less menu backing the accelerator GUI; all state is synced through packets. */
public class AcceleratorMenu extends AbstractContainerMenu {

    private final BlockPos pos;
    private final AcceleratorKind kind;
    private AcceleratorState state;

    public static void open(ServerPlayer player, BlockPos pos, AcceleratorKind kind) {
        NetworkHooks.openScreen(player, new SimpleMenuProvider(
                        (windowId, inventory, p) -> new AcceleratorMenu(windowId, inventory, pos, kind),
                        kind.title()),
                buffer -> {
                    buffer.writeBlockPos(pos);
                    buffer.writeByte(kind.ordinal());
                });
    }

    public AcceleratorMenu(int windowId, Inventory inventory, FriendlyByteBuf data) {
        this(windowId, inventory, data.readBlockPos(),
                AcceleratorKind.values()[data.readByte()]);
    }

    public AcceleratorMenu(int windowId, Inventory inventory, BlockPos pos,
                           AcceleratorKind kind) {
        super(ModMenus.ACCELERATOR.get(), windowId);
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
