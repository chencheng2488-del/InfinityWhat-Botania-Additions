package com.bandybus.infwhat.botania.client;

import com.bandybus.infwhat.botania.device.DeviceState;
import com.bandybus.infwhat.botania.device.AcceleratorState;
import com.bandybus.infwhat.botania.container.AcceleratorContainer;
import com.bandybus.infwhat.botania.container.ManaDeviceContainer;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.BlockPos;

/** Applies a state packet to the currently open container. */
public final class ClientStateReceiver {

	public static void receive(BlockPos pos, DeviceState state) {
		Minecraft minecraft = Minecraft.getMinecraft();
		if (minecraft.player != null
				&& minecraft.player.openContainer instanceof ManaDeviceContainer) {
			ManaDeviceContainer container = (ManaDeviceContainer) minecraft.player.openContainer;
			if (container.matches(pos)) {
				container.applyState(state);
			}
		}
	}

	public static void receiveAccelerator(BlockPos pos, AcceleratorState state) {
		Minecraft minecraft = Minecraft.getMinecraft();
		if (minecraft.player != null
				&& minecraft.player.openContainer instanceof AcceleratorContainer) {
			AcceleratorContainer container =
					(AcceleratorContainer) minecraft.player.openContainer;
			if (container.matches(pos)) {
				container.applyState(state);
			}
		}
	}

	private ClientStateReceiver() {
	}
}
