package com.bandybus.infwhat.botania;

import com.bandybus.infwhat.botania.network.ModNetwork;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/** Server-safe proxy. Client-only work happens in {@code ClientProxy}. */
public class CommonProxy {

	public void preInit(FMLPreInitializationEvent event) {
	}

	public void init(FMLInitializationEvent event) {
	}

	public void postInit(FMLPostInitializationEvent event) {
	}

	/** Called from the network handler for the client-bound device state packet. */
	public void handleDeviceState(ModNetwork.StateMessage message) {
	}

	/** Called from the network handler for the client-bound accelerator state packet. */
	public void handleAcceleratorState(ModNetwork.AcceleratorStateMessage message) {
	}
}
