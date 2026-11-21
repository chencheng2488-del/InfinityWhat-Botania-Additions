package com.bandybus.infwhat.botania.event;

import com.bandybus.infwhat.botania.block.entity.GaiaBeaconBlockEntity;
import com.bandybus.infwhat.botania.block.entity.ManaBeaconBlockEntity;
import com.bandybus.infwhat.botania.block.entity.ModBlockEntities;
import vazkii.botania.api.BotaniaFabricCapabilities;

/** Exposes our wireless devices as Botania mana receivers so wells can feed them directly. */
public final class ModCapabilities {

    private ModCapabilities() {
    }

    public static void register() {
        BotaniaFabricCapabilities.MANA_RECEIVER.registerForBlockEntities(
                (blockEntity, direction) -> blockEntity instanceof ManaBeaconBlockEntity beacon
                        ? beacon
                        : blockEntity instanceof GaiaBeaconBlockEntity gaiaBeacon ? gaiaBeacon : null,
                ModBlockEntities.MANA_BEACON, ModBlockEntities.GAIA_BEACON);
    }
}
