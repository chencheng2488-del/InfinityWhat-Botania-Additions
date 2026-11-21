package com.bandybus.infwhat.botania.device;

import com.bandybus.infwhat.botania.util.Pos;
import net.minecraft.world.World;

import java.util.List;

/** Implemented by the tile entities that expose the device GUI and draw network links. */
public interface ManaDeviceHost {

    ManaDeviceSettings settings();

    boolean isMasterEnabled();

    void setMasterEnabled(boolean enabled);

    /** Builds the current snapshot; called on the server when a client requests it. */
    DeviceState createState(World world);

    /** Neighbouring network node positions, synced to the client for the in-world link beams. */
    List<Pos> clientLinks();

    /** Nearby mana pools that were auto-connected by the scan, synced to the client. */
    List<Pos> clientAutoTargets();

    int reachX();

    int reachY();

    int reachZ();

    int offsetX();

    int offsetY();

    int offsetZ();

    boolean showRange();
}
