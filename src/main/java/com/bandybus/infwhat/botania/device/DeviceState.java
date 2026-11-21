package com.bandybus.infwhat.botania.device;

import java.util.List;

/** Snapshot sent to the client while a device GUI is open. */
public record DeviceState(int rate, int effectiveRate, boolean enabled,
                          long buffer, long bufferMax,
                          String networkName,
                          int reachX, int reachY, int reachZ,
                          int offsetX, int offsetY, int offsetZ,
                          boolean showRange,
                          List<LinkEntry> links) {
}
