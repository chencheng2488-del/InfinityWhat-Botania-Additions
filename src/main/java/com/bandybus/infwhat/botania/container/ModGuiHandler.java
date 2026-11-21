package com.bandybus.infwhat.botania.container;

import com.bandybus.infwhat.botania.block.entity.ManaAnchorTileEntity;
import com.bandybus.infwhat.botania.block.entity.ManaBeaconTileEntity;
import com.bandybus.infwhat.botania.block.entity.ManaWellTileEntity;
import com.bandybus.infwhat.botania.block.entity.RuneAcceleratorTileEntity;
import com.bandybus.infwhat.botania.block.entity.TerrasteelAcceleratorTileEntity;
import com.bandybus.infwhat.botania.block.entity.FloralOverclockTileEntity;
import com.bandybus.infwhat.botania.client.gui.GuiAccelerator;
import com.bandybus.infwhat.botania.client.gui.GuiManaDevice;
import com.bandybus.infwhat.botania.device.AcceleratorKind;
import com.bandybus.infwhat.botania.device.DeviceKind;
import cpw.mods.fml.common.network.IGuiHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

/** Maps the six GUIs to their server containers and client screens. */
public class ModGuiHandler implements IGuiHandler {

    @Override
    public Object getServerGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        TileEntity tileEntity = world.getTileEntity(x, y, z);
        if (id == ModGuiIds.MANA_WELL && tileEntity instanceof ManaWellTileEntity) {
            return new ContainerManaDevice(player.inventory, x, y, z, DeviceKind.WELL);
        }
        if (id == ModGuiIds.MANA_BEACON && tileEntity instanceof ManaBeaconTileEntity) {
            return new ContainerManaDevice(player.inventory, x, y, z, DeviceKind.BEACON);
        }
        if (id == ModGuiIds.MANA_ANCHOR && tileEntity instanceof ManaAnchorTileEntity) {
            return new ContainerManaDevice(player.inventory, x, y, z, DeviceKind.ANCHOR);
        }
        if (id == ModGuiIds.RUNE_ACCELERATOR
                && tileEntity instanceof RuneAcceleratorTileEntity) {
            return new ContainerAccelerator(player.inventory, x, y, z, AcceleratorKind.RUNE);
        }
        if (id == ModGuiIds.TERRASTEEL_ACCELERATOR
                && tileEntity instanceof TerrasteelAcceleratorTileEntity) {
            return new ContainerAccelerator(player.inventory, x, y, z,
                    AcceleratorKind.TERRASTEEL);
        }
        if (id == ModGuiIds.FLORAL_OVERCLOCK
                && tileEntity instanceof FloralOverclockTileEntity) {
            return new ContainerAccelerator(player.inventory, x, y, z,
                    AcceleratorKind.FLORAL);
        }
        return null;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        Object server = getServerGuiElement(id, player, world, x, y, z);
        if (server instanceof ContainerManaDevice) {
            return new GuiManaDevice((ContainerManaDevice) server, player.inventory);
        }
        if (server instanceof ContainerAccelerator) {
            return new GuiAccelerator((ContainerAccelerator) server, player.inventory);
        }
        return null;
    }
}
