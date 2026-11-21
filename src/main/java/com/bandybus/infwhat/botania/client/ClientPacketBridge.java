package com.bandybus.infwhat.botania.client;

import com.bandybus.infwhat.botania.device.AcceleratorState;
import com.bandybus.infwhat.botania.device.DeviceState;
import com.bandybus.infwhat.botania.util.Pos;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;

/** Applies state packets on the client thread. */
@SideOnly(Side.CLIENT)
public final class ClientPacketBridge {

    private ClientPacketBridge() {
    }

    public static void applyDevice(final Pos pos, final DeviceState state) {
        // The packet arrives on the network thread; GUI changes must run on the client thread.
        Minecraft.getMinecraft().addScheduledTask(new Runnable() {
            @Override
            public void run() {
                ClientStateReceiver.putDevice(pos, state);
            }
        });
    }

    public static void applyAccelerator(final Pos pos, final AcceleratorState state) {
        Minecraft.getMinecraft().addScheduledTask(new Runnable() {
            @Override
            public void run() {
                ClientStateReceiver.putAccelerator(pos, state);
            }
        });
    }
}
