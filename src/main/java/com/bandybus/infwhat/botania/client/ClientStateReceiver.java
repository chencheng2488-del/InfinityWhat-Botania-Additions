package com.bandybus.infwhat.botania.client;

import com.bandybus.infwhat.botania.container.AcceleratorContainer;
import com.bandybus.infwhat.botania.container.ManaDeviceContainer;
import com.bandybus.infwhat.botania.device.AcceleratorState;
import com.bandybus.infwhat.botania.device.DeviceState;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.BlockPos;

/** Applies a state packet to the currently open container. */
public final class ClientStateReceiver {

    private ClientStateReceiver() {
    }

    public static void receive(BlockPos pos, DeviceState state) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null
                && minecraft.player.containerMenu instanceof ManaDeviceContainer
                && ((ManaDeviceContainer) minecraft.player.containerMenu).matches(pos)) {
            ((ManaDeviceContainer) minecraft.player.containerMenu).applyState(state);
        }
    }

    public static void receiveAccelerator(BlockPos pos, AcceleratorState state) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null
                && minecraft.player.containerMenu instanceof AcceleratorContainer
                && ((AcceleratorContainer) minecraft.player.containerMenu).matches(pos)) {
            ((AcceleratorContainer) minecraft.player.containerMenu).applyState(state);
        }
    }
}
