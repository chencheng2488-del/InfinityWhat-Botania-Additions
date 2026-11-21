package com.bandybus.infwhat.botania.config;

import net.minecraftforge.common.config.Configuration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** 1.7.10 configuration, stored in {@code config/infwhat_botania.cfg}. */
public final class BotaniaAdditionConfig {

    private static Configuration configuration;

    private static boolean enableManaWell = true;
    private static boolean allowOnDedicatedServer = false;
    private static int manaPerTick = 1000;
    private static int maxManaPerTick = 1000000;
    private static int radius = 4;

    private static boolean enableWireless = true;
    private static long maxPerChannel = 1000000L;
    private static int lossPercent = 0;
    private static List<String> dimensionBlacklist = new ArrayList<String>();

    private static boolean enableGaiaSpiritRecipe;
    private static boolean enableCheapTerrasteelRecipe;
    private static boolean enableGaiaCharm;
    private static boolean gaiaCharmHardMode;
    private static boolean gaiaCharmConsumes = true;
    private static boolean enableGaiaBeacon;
    private static int gaiaBeaconManaCost = 500000;
    private static int gaiaBeaconCapacity = 1000000;
    private static int gaiaBeaconRadius = 24;

    private static boolean enableRuneTemplate;
    private static int runeTemplateManaCost = 5200;

    private static int acceleratorMaxReach = 8;
    private static int acceleratorMaxOffset = 8;

    private static boolean enableRuneAccelerator;
    private static int runeAcceleratorManaPerTick = 1000;
    private static int runeAcceleratorRadius = 3;
    private static int runeAcceleratorCooldownMultiplier = 3;

    private static boolean enableTerrasteelAccelerator;
    private static int terrasteelAcceleratorManaPerTick = 10000;
    private static int terrasteelAcceleratorRadius = 3;

    private static boolean enableFloralOverclock;
    private static int floralOverclockMultiplier = 2;
    private static int floralOverclockCooldownMultiplier = 3;
    private static int floralOverclockRadius = 3;

    private static boolean enableEverlastingFertilizer = true;

    private BotaniaAdditionConfig() {
    }

    public static void load(File file) {
        configuration = new Configuration(file);
        configuration.load();

        enableManaWell = configuration.getBoolean("enableManaWell", "mana_well", true,
                "Master switch for the Mana Well's output.");
        allowOnDedicatedServer = configuration.getBoolean("allowOnDedicatedServer", "mana_well",
                false, "Allow the Mana Well and the wireless bridge to work on a dedicated server.");
        manaPerTick = configuration.getInt("manaPerTick", "mana_well", 1000, 1,
                Integer.MAX_VALUE, "Mana produced per tick by one Mana Well.");
        maxManaPerTick = configuration.getInt("maxManaPerTick", "mana_well", 1000000, 1,
                Integer.MAX_VALUE, "Upper bound accepted by the config loader.");
        radius = configuration.getInt("radius", "mana_well", 4, 1, 8,
                "Default cube radius scanned by the Mana Well for nearby mana pools.");

        enableWireless = configuration.getBoolean("enableWireless", "mana_wireless", true,
                "Master switch for the Mana Beacon and Mana Anchor.");
        maxPerChannel = readLong("maxPerChannel", "mana_wireless", 1000000L, 1L,
                Long.MAX_VALUE, "Maximum mana buffered by one implicit network.");
        lossPercent = configuration.getInt("lossPercent", "mana_wireless", 0, 0, 100,
                "Percentage of mana lost when a beacon deposits into the network.");
        String[] blacklist = configuration.getStringList("blacklist", "dimensions",
                new String[0], "Dimension ids as numbers: -1 for the Nether, 1 for the End, "
                        + "0 for the Overworld.");
        dimensionBlacklist = new ArrayList<String>();
        for (String entry : blacklist) {
            dimensionBlacklist.add(entry);
        }

        enableGaiaSpiritRecipe = configuration.getBoolean("enableGaiaSpiritRecipe", "recipes",
                false, "Allow crafting botania:life_essence without fighting the Gaia Guardian.");
        enableCheapTerrasteelRecipe = configuration.getBoolean("enableCheapTerrasteelRecipe",
                "recipes", false, "Allow the cheap terrasteel recipe on the Terrestrial "
                        + "Agglomeration Plate.");

        enableGaiaCharm = configuration.getBoolean("enable", "gaia_charm", false,
                "Enable the Gaia Charm.");
        gaiaCharmHardMode = configuration.getBoolean("hardMode", "gaia_charm", false,
                "Summon hard mode (Gaia Guardian II).");
        gaiaCharmConsumes = configuration.getBoolean("consumeOnUse", "gaia_charm", true,
                "Consume one charm per summon.");
        enableGaiaBeacon = configuration.getBoolean("enable", "gaia_beacon", false,
                "Enable the Gaia Beacon block.");
        gaiaBeaconManaCost = configuration.getInt("manaCost", "gaia_beacon", 500000, 0,
                1000000, "Mana taken from a nearby pool for each finished fight.");
        gaiaBeaconCapacity = configuration.getInt("capacity", "gaia_beacon", 1000000, 0,
                Integer.MAX_VALUE, "Internal mana buffer of the Gaia Beacon.");
        gaiaBeaconRadius = configuration.getInt("radius", "gaia_beacon", 24, 4, 64,
                "Cube radius used to find the guardian and the players to credit.");

        enableRuneTemplate = configuration.getBoolean("enable", "rune_template", false,
                "Enable the Rune Template.");
        runeTemplateManaCost = configuration.getInt("manaCost", "rune_template", 5200, 0,
                1000000, "Mana consumed per rune produced by the template.");

        acceleratorMaxReach = configuration.getInt("maxReach", "accelerators", 8, 1, 8,
                "Maximum reach on each axis for the accelerator range boxes.");
        acceleratorMaxOffset = configuration.getInt("maxOffset", "accelerators", 8, 0, 8,
                "Maximum absolute offset on each axis for the accelerator range boxes.");

        enableRuneAccelerator = configuration.getBoolean("enable", "rune_accelerator", false,
                "Enable the Runic Altar Accelerator block.");
        runeAcceleratorManaPerTick = configuration.getInt("manaPerTick", "rune_accelerator",
                1000, 1, Integer.MAX_VALUE, "Default mana injected into each altar per tick.");
        runeAcceleratorRadius = configuration.getInt("radius", "rune_accelerator", 3, 1, 8,
                "Default cube radius scanned for Runic Altars.");
        runeAcceleratorCooldownMultiplier = configuration.getInt("cooldownMultiplier",
                "rune_accelerator", 3, 1, Integer.MAX_VALUE,
                "Default multiplier for the runic altar's crafting cooldown.");

        enableTerrasteelAccelerator = configuration.getBoolean("enable", "terrasteel_accelerator",
                false, "Enable the Terrasteel Plate Accelerator block.");
        terrasteelAcceleratorManaPerTick = configuration.getInt("manaPerTick",
                "terrasteel_accelerator", 10000, 1, Integer.MAX_VALUE,
                "Default mana injected into each plate per tick.");
        terrasteelAcceleratorRadius = configuration.getInt("radius", "terrasteel_accelerator",
                3, 1, 8, "Default cube radius scanned for Terrestrial Agglomeration Plates.");

        enableFloralOverclock = configuration.getBoolean("enable", "floral_overclock", false,
                "Enable the Floral Overclock block.");
        floralOverclockMultiplier = configuration.getInt("speedMultiplier", "floral_overclock",
                2, 1, 16, "Default tick multiplier for generating flowers.");
        floralOverclockCooldownMultiplier = configuration.getInt("cooldownMultiplier",
                "floral_overclock", 3, 1, Integer.MAX_VALUE,
                "Cooldown speed for generating flowers such as Thermalily.");
        floralOverclockRadius = configuration.getInt("radius", "floral_overclock", 3, 1, 8,
                "Default cube radius scanned for generating flowers.");

        enableEverlastingFertilizer = configuration.getBoolean("enableEverlastingFertilizer",
                "qol", true, "Enable the reusable Everlasting Fertilizer.");

        if (configuration.hasChanged()) {
            configuration.save();
        }
    }

    private static long readLong(String key, String category, long defaultValue,
                                 long min, long max, String comment) {
        String value = configuration.getString(key, category, Long.toString(defaultValue),
                comment);
        try {
            long parsed = Long.parseLong(value);
            return Math.max(min, Math.min(max, parsed));
        } catch (NumberFormatException exception) {
            return defaultValue;
        }
    }

    public static boolean manaWellEnabled() {
        return enableManaWell;
    }

    public static boolean wirelessEnabled() {
        return enableWireless;
    }

    public static boolean allowOnDedicatedServer() {
        return allowOnDedicatedServer;
    }

    public static int manaPerTick() {
        return Math.min(manaPerTick, maxManaPerTick);
    }

    public static int maxManaPerTick() {
        return maxManaPerTick;
    }

    public static int radius() {
        return radius;
    }

    public static long maxPerChannel() {
        return maxPerChannel;
    }

    public static int lossPercent() {
        return lossPercent;
    }

    public static boolean gaiaSpiritRecipeEnabled() {
        return enableGaiaSpiritRecipe;
    }

    public static boolean cheapTerrasteelRecipeEnabled() {
        return enableCheapTerrasteelRecipe;
    }

    public static boolean gaiaCharmEnabled() {
        return enableGaiaCharm;
    }

    public static boolean gaiaCharmHardMode() {
        return gaiaCharmHardMode;
    }

    public static boolean gaiaCharmConsumes() {
        return gaiaCharmConsumes;
    }

    public static boolean gaiaBeaconEnabled() {
        return enableGaiaBeacon;
    }

    public static int gaiaBeaconManaCost() {
        return gaiaBeaconManaCost;
    }

    public static int gaiaBeaconCapacity() {
        return gaiaBeaconCapacity;
    }

    public static int gaiaBeaconRadius() {
        return gaiaBeaconRadius;
    }

    public static boolean runeTemplateEnabled() {
        return enableRuneTemplate;
    }

    public static int runeTemplateManaCost() {
        return runeTemplateManaCost;
    }

    public static int acceleratorMaxReach() {
        return acceleratorMaxReach;
    }

    public static int acceleratorMaxOffset() {
        return acceleratorMaxOffset;
    }

    public static boolean runeAcceleratorEnabled() {
        return enableRuneAccelerator;
    }

    public static int runeAcceleratorManaPerTick() {
        return runeAcceleratorManaPerTick;
    }

    public static int runeAcceleratorRadius() {
        return runeAcceleratorRadius;
    }

    public static int runeAcceleratorCooldownMultiplier() {
        return runeAcceleratorCooldownMultiplier;
    }

    public static boolean terrasteelAcceleratorEnabled() {
        return enableTerrasteelAccelerator;
    }

    public static int terrasteelAcceleratorManaPerTick() {
        return terrasteelAcceleratorManaPerTick;
    }

    public static int terrasteelAcceleratorRadius() {
        return terrasteelAcceleratorRadius;
    }

    public static boolean floralOverclockEnabled() {
        return enableFloralOverclock;
    }

    public static int floralOverclockMultiplier() {
        return floralOverclockMultiplier;
    }

    public static int floralOverclockCooldownMultiplier() {
        return floralOverclockCooldownMultiplier;
    }

    public static int floralOverclockRadius() {
        return floralOverclockRadius;
    }

    public static boolean everlastingFertilizerEnabled() {
        return enableEverlastingFertilizer;
    }

    public static int applyLoss(int amount) {
        int loss = lossPercent;
        if (loss <= 0) {
            return amount;
        }
        if (loss >= 100) {
            return 0;
        }
        return (int) ((long) amount * (100 - loss) / 100L);
    }

    public static boolean isDimensionAllowed(int dimensionId) {
        return !dimensionBlacklist.contains(Integer.toString(dimensionId));
    }

    public static boolean canOperate(net.minecraft.world.World world) {
        if (world == null || world.isRemote) {
            return false;
        }
        if (net.minecraft.server.MinecraftServer.getServer() != null
                && net.minecraft.server.MinecraftServer.getServer().isDedicatedServer()
                && !allowOnDedicatedServer) {
            return false;
        }
        return isDimensionAllowed(world.provider.dimensionId);
    }
}
