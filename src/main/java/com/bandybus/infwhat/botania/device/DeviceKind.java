package com.bandybus.infwhat.botania.device;

import net.minecraft.util.StatCollector;

/** Which device a GUI belongs to; controls labels and which widgets are shown. */
public enum DeviceKind {

    WELL("gui.infwhat_botania.mana_well", "gui.infwhat_botania.output_rate", false, true),
    BEACON("gui.infwhat_botania.mana_beacon", "gui.infwhat_botania.extract_rate", true, false),
    ANCHOR("gui.infwhat_botania.mana_anchor", "gui.infwhat_botania.output_rate", true, true);

    private final String titleKey;
    private final String rateKey;
    private final boolean showBuffer;
    private final boolean hasRange;

    DeviceKind(String titleKey, String rateKey, boolean showBuffer, boolean hasRange) {
        this.titleKey = titleKey;
        this.rateKey = rateKey;
        this.showBuffer = showBuffer;
        this.hasRange = hasRange;
    }

    public String title() {
        return StatCollector.translateToLocal(titleKey);
    }

    public String rateLabel() {
        return StatCollector.translateToLocal(rateKey);
    }

    public boolean showBuffer() {
        return showBuffer;
    }

    public boolean hasRange() {
        return hasRange;
    }
}
