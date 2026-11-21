package com.bandybus.infwhat.botania.container;

import com.bandybus.infwhat.botania.block.tile.AbstractAcceleratorTileEntity;
import com.bandybus.infwhat.botania.block.tile.FloralOverclockTileEntity;
import com.bandybus.infwhat.botania.block.tile.ManaAnchorTileEntity;
import com.bandybus.infwhat.botania.block.tile.ManaBeaconTileEntity;
import com.bandybus.infwhat.botania.block.tile.RuneAcceleratorTileEntity;
import com.bandybus.infwhat.botania.block.tile.TerrasteelAcceleratorTileEntity;
import com.bandybus.infwhat.botania.client.GuiAccelerator;
import com.bandybus.infwhat.botania.client.GuiManaDevice;
import com.bandybus.infwhat.botania.device.AcceleratorKind;
import com.bandybus.infwhat.botania.device.DeviceKind;
import com.bandybus.infwhat.botania.device.ManaDeviceHost;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;

/** Server and client GUI factory for the device and accelerator panels. */
public class ModGuiHandler implements IGuiHandler {

	public static final int GUI_DEVICE = 0;
	public static final int GUI_ACCELERATOR = 1;

	@Override
	public Object getServerGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
		BlockPos pos = new BlockPos(x, y, z);
		TileEntity tileEntity = world.getTileEntity(pos);
		if (id == GUI_DEVICE && tileEntity instanceof ManaDeviceHost) {
			return new ManaDeviceContainer(player.inventory, pos, deviceKind(tileEntity));
		}
		if (id == GUI_ACCELERATOR && tileEntity instanceof AbstractAcceleratorTileEntity) {
			return new AcceleratorContainer(player.inventory, pos,
					acceleratorKind((AbstractAcceleratorTileEntity) tileEntity));
		}
		return null;
	}

	@Override
	public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
		BlockPos pos = new BlockPos(x, y, z);
		TileEntity tileEntity = world.getTileEntity(pos);
		if (id == GUI_DEVICE && tileEntity instanceof ManaDeviceHost) {
			return new GuiManaDevice(new ManaDeviceContainer(player.inventory, pos,
					deviceKind(tileEntity)), player.inventory);
		}
		if (id == GUI_ACCELERATOR && tileEntity instanceof AbstractAcceleratorTileEntity) {
			return new GuiAccelerator(new AcceleratorContainer(player.inventory, pos,
					acceleratorKind((AbstractAcceleratorTileEntity) tileEntity)), player.inventory);
		}
		return null;
	}

	public static DeviceKind deviceKind(TileEntity tileEntity) {
		if (tileEntity instanceof ManaBeaconTileEntity) {
			return DeviceKind.BEACON;
		}
		if (tileEntity instanceof ManaAnchorTileEntity) {
			return DeviceKind.ANCHOR;
		}
		return DeviceKind.WELL;
	}

	public static AcceleratorKind acceleratorKind(AbstractAcceleratorTileEntity tileEntity) {
		if (tileEntity instanceof RuneAcceleratorTileEntity) {
			return AcceleratorKind.RUNE;
		}
		if (tileEntity instanceof TerrasteelAcceleratorTileEntity) {
			return AcceleratorKind.TERRASTEEL;
		}
		if (tileEntity instanceof FloralOverclockTileEntity) {
			return AcceleratorKind.FLORAL;
		}
		return AcceleratorKind.RUNE;
	}
}
