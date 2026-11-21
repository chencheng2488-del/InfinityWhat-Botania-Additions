package com.bandybus.infwhat.botania.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/** Client-to-server packets for the device and accelerator GUIs. */
public final class ClientNetwork {

    private ClientNetwork() {
    }

    public static void requestDeviceState(BlockPos pos) {
        send(ModNetwork.REQUEST_STATE, buffer -> new ModNetwork.RequestState(pos).encode(buffer));
    }

    public static void requestAcceleratorState(BlockPos pos) {
        send(ModNetwork.ACCELERATOR_REQUEST_STATE,
                buffer -> new ModNetwork.AcceleratorRequestState(pos).encode(buffer));
    }

    public static void toggleDevice(BlockPos pos) {
        send(ModNetwork.TOGGLE_ENABLED, buffer -> new ModNetwork.ToggleEnabled(pos).encode(buffer));
    }

    public static void clearExcluded(BlockPos pos) {
        send(ModNetwork.CLEAR_EXCLUDED,
                buffer -> new ModNetwork.ClearExcluded(pos).encode(buffer));
    }

    public static void setRate(BlockPos pos, int rate) {
        send(ModNetwork.SET_RATE, buffer -> new ModNetwork.SetRate(pos, rate).encode(buffer));
    }

    public static void setRange(BlockPos pos, int reachX, int reachY, int reachZ,
                               int offsetX, int offsetY, int offsetZ, boolean showRange) {
        send(ModNetwork.SET_RANGE, buffer -> new ModNetwork.SetRange(pos, reachX, reachY, reachZ,
                offsetX, offsetY, offsetZ, showRange).encode(buffer));
    }

    public static void toggleAccelerator(BlockPos pos) {
        send(ModNetwork.ACCELERATOR_TOGGLE_ENABLED,
                buffer -> new ModNetwork.AcceleratorToggleEnabled(pos).encode(buffer));
    }

    public static void setAcceleratorSettings(BlockPos pos, int rate, int cooldown,
                                              int reachX, int reachY, int reachZ,
                                              int offsetX, int offsetY, int offsetZ,
                                              boolean showRange) {
        send(ModNetwork.ACCELERATOR_SET_SETTINGS,
                buffer -> new ModNetwork.AcceleratorSetSettings(pos, rate, cooldown,
                        reachX, reachY, reachZ, offsetX, offsetY, offsetZ, showRange).encode(buffer));
    }

    private static void send(ResourceLocation channel, PacketWriter writer) {
        FriendlyByteBuf buffer = PacketByteBufs.create();
        writer.write(buffer);
        ClientPlayNetworking.send(channel, buffer);
    }

    @FunctionalInterface
    private interface PacketWriter {
        void write(FriendlyByteBuf buffer);
    }
}
