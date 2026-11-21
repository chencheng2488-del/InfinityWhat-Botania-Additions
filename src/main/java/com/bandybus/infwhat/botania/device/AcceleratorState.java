package com.bandybus.infwhat.botania.device;

import java.util.List;

/** Snapshot sent to the client while an accelerator GUI is open. */
public record AcceleratorState(boolean enabled, boolean featureEnabled,
                               int rate, int cooldown,
                               int reachX, int reachY, int reachZ,
                               int offsetX, int offsetY, int offsetZ,
                               boolean showRange,
                               List<AcceleratorTarget> targets) {
}
