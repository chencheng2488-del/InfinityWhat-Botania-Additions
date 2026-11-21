package com.bandybus.infwhat.botania.container;

import com.bandybus.infwhat.botania.block.entity.ManaWellTileEntity;
import com.bandybus.infwhat.botania.device.DeviceKind;
import com.bandybus.infwhat.botania.device.DeviceState;
import com.bandybus.infwhat.botania.device.ManaDeviceHost;
import com.bandybus.infwhat.botania.util.Pos;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.tileentity.TileEntity;

/** Slot-less container backing the device GUI; all state is synced through packets. */
public class ContainerManaDevice extends Container {

    private final Pos pos;
    private final DeviceKind kind;
    private DeviceState state;

    public ContainerManaDevice(InventoryPlayer inventory, int x, int y, int z, DeviceKind kind) {
        this.pos = new Pos(x, y, z);
        this.kind = kind;
    }

    public Pos pos() {
        return pos;
    }

    public DeviceKind kind() {
        return kind;
    }

    public DeviceState state() {
        return state;
    }

    public void applyState(DeviceState value) {
        this.state = value;
    }

    public boolean matches(Pos value) {
        return pos.equals(value);
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        if (player.getDistanceSq(pos.x + 0.5D, pos.y + 0.5D, pos.z + 0.5D) > 64.0D) {
            return false;
        }
        TileEntity tileEntity = player.worldObj.getTileEntity(pos.x, pos.y, pos.z);
        if (!(tileEntity instanceof ManaDeviceHost)) {
            return false;
        }
        if (kind == DeviceKind.WELL) {
            return tileEntity instanceof ManaWellTileEntity;
        }
        return true;
    }
}
