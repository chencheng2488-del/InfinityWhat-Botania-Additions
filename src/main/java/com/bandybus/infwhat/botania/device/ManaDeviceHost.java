package com.bandybus.infwhat.botania.device;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.server.ServerWorld;

import java.util.List;

/** Implemented by the block entities that expose the device GUI and draw network links. */
public interface ManaDeviceHost {

    ManaDeviceSettings settings();

    boolean isMasterEnabled();

    void setMasterEnabled(boolean enabled);

    /** Builds the current snapshot; called on the server when a client requests it. */
    DeviceState createState(ServerWorld level);

    /** Neighbouring network node positions, synced to the client for the in-world link beams. */
    List<BlockPos> clientLinks();

    /** Nearby mana pools that were auto-connected by the scan, synced to the client. */
    List<BlockPos> clientAutoTargets();

    int reachX();

    int reachY();

    int reachZ();

    int offsetX();

    int offsetY();

    int offsetZ();

    boolean showRange();
}
