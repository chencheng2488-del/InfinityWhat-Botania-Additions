package com.bandybus.infwhat.botania.container;

import com.bandybus.infwhat.botania.block.entity.AbstractAcceleratorTileEntity;
import com.bandybus.infwhat.botania.device.AcceleratorKind;
import com.bandybus.infwhat.botania.device.AcceleratorState;
import com.bandybus.infwhat.botania.util.Pos;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.tileentity.TileEntity;

/** Slot-less container backing the accelerator GUI; all state is synced through packets. */
public class ContainerAccelerator extends Container {

    private final Pos pos;
    private final AcceleratorKind kind;
    private AcceleratorState state;

    public ContainerAccelerator(InventoryPlayer inventory, int x, int y, int z,
                                AcceleratorKind kind) {
        this.pos = new Pos(x, y, z);
        this.kind = kind;
    }

    public Pos pos() {
        return pos;
    }

    public AcceleratorKind kind() {
        return kind;
    }

    public AcceleratorState state() {
        return state;
    }

    public void applyState(AcceleratorState value) {
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
        return tileEntity instanceof AbstractAcceleratorTileEntity;
    }
}
