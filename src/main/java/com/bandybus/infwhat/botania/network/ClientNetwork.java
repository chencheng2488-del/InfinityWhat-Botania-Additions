package com.bandybus.infwhat.botania.network;

import net.minecraft.util.math.BlockPos;

/** Client-to-server packets for the device and accelerator GUIs. */
public final class ClientNetwork {

	public static void requestDeviceState(BlockPos pos) {
		ModNetwork.INSTANCE.sendToServer(new ModNetwork.RequestState(pos));
	}

	public static void requestAcceleratorState(BlockPos pos) {
		ModNetwork.INSTANCE.sendToServer(new ModNetwork.AcceleratorRequestState(pos));
	}

	public static void toggleDevice(BlockPos pos) {
		ModNetwork.INSTANCE.sendToServer(new ModNetwork.ToggleEnabled(pos));
	}

	public static void clearExcluded(BlockPos pos) {
		ModNetwork.INSTANCE.sendToServer(new ModNetwork.ClearExcluded(pos));
	}

	public static void setRate(BlockPos pos, int rate) {
		ModNetwork.INSTANCE.sendToServer(new ModNetwork.SetRate(pos, rate));
	}

	public static void setRange(BlockPos pos, int reachX, int reachY, int reachZ,
							   int offsetX, int offsetY, int offsetZ, boolean showRange) {
		ModNetwork.INSTANCE.sendToServer(new ModNetwork.SetRange(pos,
				reachX, reachY, reachZ, offsetX, offsetY, offsetZ, showRange));
	}

	public static void toggleAccelerator(BlockPos pos) {
		ModNetwork.INSTANCE.sendToServer(new ModNetwork.AcceleratorToggleEnabled(pos));
	}

	public static void setAcceleratorSettings(BlockPos pos, int rate, int cooldown,
											  int reachX, int reachY, int reachZ,
											  int offsetX, int offsetY, int offsetZ,
											  boolean showRange) {
		ModNetwork.INSTANCE.sendToServer(new ModNetwork.AcceleratorSetSettings(pos,
				rate, cooldown, reachX, reachY, reachZ, offsetX, offsetY, offsetZ, showRange));
	}

	private ClientNetwork() {
	}
}
