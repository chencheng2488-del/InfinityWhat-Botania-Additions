package com.bandybus.infwhat.botania.device;

import net.minecraft.network.chat.Component;

/** Which accelerator a GUI belongs to; controls labels and editable ranges. */
public enum AcceleratorKind {

    RUNE("block.infwhat_botania.rune_accelerator",
            "gui.infwhat_botania.accelerator.rate",
            "gui.infwhat_botania.accelerator.per_tick",
            0, Integer.MAX_VALUE, true),
    TERRASTEEL("block.infwhat_botania.terrasteel_accelerator",
            "gui.infwhat_botania.accelerator.rate",
            "gui.infwhat_botania.accelerator.per_tick",
            0, Integer.MAX_VALUE, false),
    FLORAL("block.infwhat_botania.floral_overclock",
            "gui.infwhat_botania.accelerator.speed",
            "gui.infwhat_botania.accelerator.multiplier",
            1, 16, true);

    private final String titleKey;
    private final String rateKey;
    private final String unitKey;
    private final int minRate;
    private final int maxRate;
    private final boolean hasCooldown;

    AcceleratorKind(String titleKey, String rateKey, String unitKey,
                    int minRate, int maxRate, boolean hasCooldown) {
        this.titleKey = titleKey;
        this.rateKey = rateKey;
        this.unitKey = unitKey;
        this.minRate = minRate;
        this.maxRate = maxRate;
        this.hasCooldown = hasCooldown;
    }

    public Component title() {
        return Component.translatable(titleKey);
    }

    public Component rateLabel() {
        return Component.translatable(rateKey);
    }

    public Component rateUnit() {
        return Component.translatable(unitKey);
    }

    public int minRate() {
        return minRate;
    }

    public int maxRate() {
        return maxRate;
    }

    public boolean hasCooldown() {
        return hasCooldown;
    }
}
