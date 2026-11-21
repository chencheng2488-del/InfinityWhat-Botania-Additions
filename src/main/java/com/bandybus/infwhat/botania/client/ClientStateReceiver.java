package com.bandybus.infwhat.botania.client;

import com.bandybus.infwhat.botania.device.AcceleratorState;
import com.bandybus.infwhat.botania.device.DeviceState;
import com.bandybus.infwhat.botania.menu.AcceleratorMenu;
import com.bandybus.infwhat.botania.menu.ManaDeviceMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

/** Applies a state packet to the currently open menu. */
public final class ClientStateReceiver {

    private ClientStateReceiver() {
    }

    public static void receive(BlockPos pos, DeviceState state) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null
                && minecraft.player.containerMenu instanceof ManaDeviceMenu menu
                && menu.matches(pos)) {
            menu.applyState(state);
        }
    }

    public static void receiveAccelerator(BlockPos pos, AcceleratorState state) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null
                && minecraft.player.containerMenu instanceof AcceleratorMenu menu
                && menu.matches(pos)) {
            menu.applyState(state);
        }
    }
}
