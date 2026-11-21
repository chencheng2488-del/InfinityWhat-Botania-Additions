package com.bandybus.infwhat.botania;

import com.bandybus.infwhat.botania.device.AcceleratorState;
import com.bandybus.infwhat.botania.device.DeviceState;
import com.bandybus.infwhat.botania.util.Pos;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

/** Server-side proxy: client state packets land here as no-ops. */
public class CommonProxy {

    /** Server-side pre-initialisation hook; intentionally empty. */
    public void preInit(FMLPreInitializationEvent event) {
    }

    /** Server-side initialisation hook; intentionally empty. */
    public void init(FMLInitializationEvent event) {
    }

    /** Client-only state cache hook; the server proxy intentionally does nothing. */
    public void applyDeviceState(Pos pos, DeviceState state) {
    }

    /** Client-only state cache hook; the server proxy intentionally does nothing. */
    public void applyAcceleratorState(Pos pos, AcceleratorState state) {
    }
}
