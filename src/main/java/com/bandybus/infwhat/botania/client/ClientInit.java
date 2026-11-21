package com.bandybus.infwhat.botania.client;

import com.bandybus.infwhat.botania.network.ModNetwork;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/** Client entrypoint: screens, block-entity renderers and state packet receivers. */
public class ClientInit implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ClientRenderers.register();

        ClientPlayNetworking.registerGlobalReceiver(ModNetwork.STATE,
                (client, handler, buffer, responseSender) -> {
                    ModNetwork.StateMessage message = new ModNetwork.StateMessage(buffer);
                    client.execute(() -> ClientStateReceiver.receive(message.pos(), message.state()));
                });
        ClientPlayNetworking.registerGlobalReceiver(ModNetwork.ACCELERATOR_STATE,
                (client, handler, buffer, responseSender) -> {
                    ModNetwork.AcceleratorStateMessage message =
                            new ModNetwork.AcceleratorStateMessage(buffer);
                    client.execute(() -> ClientStateReceiver.receiveAccelerator(
                            message.pos(), message.state()));
                });
    }
}
