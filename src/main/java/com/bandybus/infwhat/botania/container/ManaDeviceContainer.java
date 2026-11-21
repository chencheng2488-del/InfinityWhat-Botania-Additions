package com.bandybus.infwhat.botania.container;

import com.bandybus.infwhat.botania.device.DeviceKind;
import com.bandybus.infwhat.botania.device.DeviceState;
import com.bandybus.infwhat.botania.device.ManaDeviceHost;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;

/** Slot-less container backing the device GUI; all state is synced through packets. */
public class ManaDeviceContainer extends Container {

	private final BlockPos pos;
	private final DeviceKind kind;
	private DeviceState state;

	public ManaDeviceContainer(InventoryPlayer inventory, BlockPos pos, DeviceKind kind) {
		this.pos = pos.toImmutable();
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

	public boolean matches(BlockPos other) {
		return pos.equals(other);
	}

	@Override
	public boolean canInteractWith(EntityPlayer player) {
		return player.getDistanceSq(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= 64.0D
				&& player.world.getTileEntity(pos) instanceof ManaDeviceHost;
	}

	@Override
	public ItemStack transferStackInSlot(EntityPlayer player, int index) {
		return ItemStack.EMPTY;
	}
}
