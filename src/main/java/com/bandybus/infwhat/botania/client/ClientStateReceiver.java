package com.bandybus.infwhat.botania.client;

import com.bandybus.infwhat.botania.device.AcceleratorState;
import com.bandybus.infwhat.botania.device.DeviceState;
import com.bandybus.infwhat.botania.util.Pos;

import java.util.HashMap;
import java.util.Map;

/** Client-side cache of the last state snapshot received for each block. */
public final class ClientStateReceiver {

    private static final Map<Pos, DeviceState> DEVICES = new HashMap<Pos, DeviceState>();
    private static final Map<Pos, AcceleratorState> ACCELERATORS =
            new HashMap<Pos, AcceleratorState>();

    private ClientStateReceiver() {
    }

    public static void putDevice(Pos pos, DeviceState state) {
        DEVICES.put(pos, state);
    }

    public static void putAccelerator(Pos pos, AcceleratorState state) {
        ACCELERATORS.put(pos, state);
    }

    public static DeviceState device(Pos pos) {
        return DEVICES.get(pos);
    }

    public static AcceleratorState accelerator(Pos pos) {
        return ACCELERATORS.get(pos);
    }

    /** Drops cached states, for example when the connection to a world ends. */
    public static void clear() {
        DEVICES.clear();
        ACCELERATORS.clear();
    }
}
