package com.bandybus.infwhat.botania.container;

import com.bandybus.infwhat.botania.block.tile.AbstractAcceleratorTileEntity;
import com.bandybus.infwhat.botania.device.AcceleratorKind;
import com.bandybus.infwhat.botania.device.AcceleratorState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;

/** Slot-less container backing the accelerator GUI; all state is synced through packets. */
public class AcceleratorContainer extends Container {

	private final BlockPos pos;
	private final AcceleratorKind kind;
	private AcceleratorState state;

	public AcceleratorContainer(InventoryPlayer inventory, BlockPos pos, AcceleratorKind kind) {
		this.pos = pos.toImmutable();
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

	public boolean matches(BlockPos other) {
		return pos.equals(other);
	}

	@Override
	public boolean canInteractWith(EntityPlayer player) {
		return player.getDistanceSq(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= 64.0D
				&& player.world.getTileEntity(pos) instanceof AbstractAcceleratorTileEntity;
	}

	@Override
	public ItemStack transferStackInSlot(EntityPlayer player, int index) {
		return ItemStack.EMPTY;
	}
}
