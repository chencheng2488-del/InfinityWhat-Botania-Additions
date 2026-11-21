package com.bandybus.infwhat.botania.config;

import com.bandybus.infwhat.botania.InfinityWhatBotania;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Server/common configuration for the Botania addition, stored in
 * {@code config/infwhat_botania.toml}.
 *
 * <p>All mana production is gated by a global switch, a dedicated-server switch, a per-tick
 * cap and a dimension blacklist. The defaults are deliberately conservative: the mana well
 * produces a finite amount per tick and the wireless bridge never creates mana by itself.
 *
 * <p>Fabric has no config system, so this class contains a deliberately small TOML reader: it
 * only understands the sections, booleans, integers and string arrays used below.
 */
public final class BotaniaAdditionConfig {

    private static final String FILE_NAME = "infwhat_botania.toml";

    private static final String DEFAULT_FILE = """
            # Infinity What: Botania Additions settings.
            # Invalid values fall back to the defaults below.

            [mana_well]
            # Master switch for the Mana Well's output.
            # When false the block still exists, but provides no mana.
            enableManaWell = true
            # Allow the Mana Well and the wireless bridge to work on a dedicated server.
            # Defaults to false so a server administrator must opt in explicitly.
            allowOnDedicatedServer = false
            # Mana produced per tick by one Mana Well.
            # The well shares this budget between all of its linked outputs.
            manaPerTick = 1000
            # Upper bound accepted by the config loader. Values above this are rejected.
            maxManaPerTick = 1000000
            # Default cube radius scanned by the Mana Well for nearby mana pools.
            radius = 4

            [mana_wireless]
            # Master switch for the Mana Beacon and Mana Anchor.
            enableWireless = true
            # Maximum mana buffered by one implicit network.
            maxPerChannel = 1000000
            # Percentage of mana lost when a beacon deposits into the network.
            lossPercent = 0

            [dimensions]
            # Dimension ids in which the addition must not operate, e.g. "minecraft:the_nether".
            blacklist = []

            [recipes]
            # Allow crafting botania:life_essence without fighting the Gaia Guardian.
            enableGaiaSpiritRecipe = false
            # Allow an iron + diamond + mana diamond recipe on the Terrestrial Agglomeration Plate.
            enableCheapTerrasteelRecipe = false

            [gaia_charm]
            # Enable the Gaia Charm.
            enable = false
            # Summon hard mode (Gaia Guardian II).
            hardMode = false
            # Consume one charm per summon. When false the charm is reusable.
            consumeOnUse = true

            [gaia_beacon]
            # Enable the Gaia Beacon block.
            enable = false
            # Mana taken from a nearby pool for each finished fight.
            manaCost = 500000
            # Internal mana buffer of the Gaia Beacon.
            capacity = 1000000
            # Cube radius used to find the guardian and the players to credit.
            radius = 24

            [rune_template]
            # Enable the Rune Template.
            enable = false
            # Mana consumed per rune produced by the template.
            manaCost = 5200

            [accelerators]
            # Maximum reach on each axis for the accelerator range boxes.
            maxReach = 8
            # Maximum absolute offset on each axis for the accelerator range boxes.
            maxOffset = 8

            [rune_accelerator]
            # Enable the Runic Altar Accelerator block.
            enable = false
            # Default mana injected into each nearby altar per tick.
            manaPerTick = 1000
            # Default cube radius scanned for Runic Altars.
            radius = 3
            # Default multiplier for the runic altar's crafting cooldown.
            cooldownMultiplier = 3

            [terrasteel_accelerator]
            # Enable the Terrasteel Plate Accelerator block.
            enable = false
            # Default mana injected into each nearby plate per tick.
            manaPerTick = 10000
            # Default cube radius scanned for Terrestrial Agglomeration Plates.
            radius = 3

            [floral_overclock]
            # Enable the Floral Overclock block.
            enable = false
            # Default tick multiplier for generating flowers. 1 disables the effect.
            speedMultiplier = 2
            # Cooldown speed for generating flowers such as Thermalily.
            cooldownMultiplier = 3
            # Default cube radius scanned for generating flowers.
            radius = 3

            [qol]
            # Enable the reusable Everlasting Fertilizer.
            enableEverlastingFertilizer = true
            """;

    private static final Pattern QUOTED = Pattern.compile("\"([^\"]*)\"");

    private static boolean enableManaWell = true;
    private static boolean allowOnDedicatedServer;
    private static int manaPerTick = 1_000;
    private static int maxManaPerTick = 1_000_000;
    private static int radius = 4;
    private static boolean enableWireless = true;
    private static long maxPerChannel = 1_000_000L;
    private static int lossPercent;
    private static List<String> dimensionBlacklist = List.of();
    private static boolean enableGaiaSpiritRecipe;
    private static boolean enableCheapTerrasteelRecipe;
    private static boolean enableGaiaCharm;
    private static boolean gaiaCharmHardMode;
    private static boolean gaiaCharmConsumes = true;
    private static boolean enableGaiaBeacon;
    private static int gaiaBeaconManaCost = 500_000;
    private static int gaiaBeaconCapacity = 1_000_000;
    private static int gaiaBeaconRadius = 24;
    private static boolean enableRuneTemplate;
    private static int runeTemplateManaCost = 5_200;
    private static int acceleratorMaxReach = 8;
    private static int acceleratorMaxOffset = 8;
    private static boolean enableRuneAccelerator;
    private static int runeAcceleratorManaPerTick = 1_000;
    private static int runeAcceleratorRadius = 3;
    private static int runeAcceleratorCooldownMultiplier = 3;
    private static boolean enableTerrasteelAccelerator;
    private static int terrasteelAcceleratorManaPerTick = 10_000;
    private static int terrasteelAcceleratorRadius = 3;
    private static boolean enableFloralOverclock;
    private static int floralOverclockMultiplier = 2;
    private static int floralOverclockCooldownMultiplier = 3;
    private static int floralOverclockRadius = 3;
    private static boolean enableEverlastingFertilizer = true;

    public static void load() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
        try {
            if (!Files.exists(path)) {
                Files.createDirectories(path.getParent());
                Files.writeString(path, DEFAULT_FILE, StandardCharsets.UTF_8);
            }
            parse(Files.readAllLines(path, StandardCharsets.UTF_8));
        } catch (IOException | RuntimeException exception) {
            InfinityWhatBotania.LOGGER.warn(
                    "Infinity What: Botania Additions failed to read {}, using defaults", path,
                    exception);
        }
    }

    private static void parse(List<String> lines) {
        String section = "";
        for (int index = 0; index < lines.size(); index++) {
            String line = stripComment(lines.get(index)).trim();
            if (line.isEmpty()) {
                continue;
            }
            if (line.startsWith("[") && line.endsWith("]")) {
                section = line.substring(1, line.length() - 1).trim().toLowerCase(Locale.ROOT);
                continue;
            }
            int equals = line.indexOf('=');
            if (equals < 0) {
                continue;
            }
            String key = normalize(line.substring(0, equals));
            String value = line.substring(equals + 1).trim();
            if (value.startsWith("[")) {
                // Multi-line arrays: keep appending until the brackets are balanced.
                StringBuilder array = new StringBuilder(value);
                while (bracketDepth(array.toString()) > 0 && index + 1 < lines.size()) {
                    array.append(' ').append(stripComment(lines.get(++index)).trim());
                }
                value = array.toString();
            }
            apply(section, key, value);
        }
    }

    private static void apply(String section, String key, String value) {
        switch (section) {
            case "mana_well" -> {
                if (key.equals("enablemanawell")) {
                    enableManaWell = parseBoolean(value, true);
                } else if (key.equals("allowondedicatedserver")) {
                    allowOnDedicatedServer = parseBoolean(value, false);
                } else if (key.equals("manapertick")) {
                    manaPerTick = (int) parseLong(value, 1_000L, 1L, Integer.MAX_VALUE);
                } else if (key.equals("maxmanapertick")) {
                    maxManaPerTick = (int) parseLong(value, 1_000_000L, 1L, Integer.MAX_VALUE);
                } else if (key.equals("radius")) {
                    radius = (int) parseLong(value, 4L, 1L, 8L);
                }
            }
            case "mana_wireless" -> {
                if (key.equals("enablewireless")) {
                    enableWireless = parseBoolean(value, true);
                } else if (key.equals("maxperchannel")) {
                    maxPerChannel = parseLong(value, 1_000_000L, 1L, Long.MAX_VALUE);
                } else if (key.equals("losspercent")) {
                    lossPercent = (int) parseLong(value, 0L, 0L, 100L);
                }
            }
            case "dimensions" -> {
                if (key.equals("blacklist")) {
                    dimensionBlacklist = parseList(value);
                }
            }
            case "recipes" -> {
                if (key.equals("enablegaiaspiritrecipe")) {
                    enableGaiaSpiritRecipe = parseBoolean(value, false);
                } else if (key.equals("enablecheapterrasteelrecipe")) {
                    enableCheapTerrasteelRecipe = parseBoolean(value, false);
                }
            }
            case "gaia_charm" -> {
                if (key.equals("enable")) {
                    enableGaiaCharm = parseBoolean(value, false);
                } else if (key.equals("hardmode")) {
                    gaiaCharmHardMode = parseBoolean(value, false);
                } else if (key.equals("consumeonuse")) {
                    gaiaCharmConsumes = parseBoolean(value, true);
                }
            }
            case "gaia_beacon" -> {
                if (key.equals("enable")) {
                    enableGaiaBeacon = parseBoolean(value, false);
                } else if (key.equals("manacost")) {
                    gaiaBeaconManaCost = (int) parseLong(value, 500_000L, 0L, 1_000_000L);
                } else if (key.equals("capacity")) {
                    gaiaBeaconCapacity = (int) parseLong(value, 1_000_000L, 0L, Integer.MAX_VALUE);
                } else if (key.equals("radius")) {
                    gaiaBeaconRadius = (int) parseLong(value, 24L, 4L, 64L);
                }
            }
            case "rune_template" -> {
                if (key.equals("enable")) {
                    enableRuneTemplate = parseBoolean(value, false);
                } else if (key.equals("manacost")) {
                    runeTemplateManaCost = (int) parseLong(value, 5_200L, 0L, 1_000_000L);
                }
            }
            case "accelerators" -> {
                if (key.equals("maxreach")) {
                    acceleratorMaxReach = (int) parseLong(value, 8L, 1L, 8L);
                } else if (key.equals("maxoffset")) {
                    acceleratorMaxOffset = (int) parseLong(value, 8L, 0L, 8L);
                }
            }
            case "rune_accelerator" -> {
                if (key.equals("enable")) {
                    enableRuneAccelerator = parseBoolean(value, false);
                } else if (key.equals("manapertick")) {
                    runeAcceleratorManaPerTick =
                            (int) parseLong(value, 1_000L, 1L, Integer.MAX_VALUE);
                } else if (key.equals("radius")) {
                    runeAcceleratorRadius = (int) parseLong(value, 3L, 1L, 8L);
                } else if (key.equals("cooldownmultiplier")) {
                    runeAcceleratorCooldownMultiplier =
                            (int) parseLong(value, 3L, 1L, Integer.MAX_VALUE);
                }
            }
            case "terrasteel_accelerator" -> {
                if (key.equals("enable")) {
                    enableTerrasteelAccelerator = parseBoolean(value, false);
                } else if (key.equals("manapertick")) {
                    terrasteelAcceleratorManaPerTick =
                            (int) parseLong(value, 10_000L, 1L, Integer.MAX_VALUE);
                } else if (key.equals("radius")) {
                    terrasteelAcceleratorRadius = (int) parseLong(value, 3L, 1L, 8L);
                }
            }
            case "floral_overclock" -> {
                if (key.equals("enable")) {
                    enableFloralOverclock = parseBoolean(value, false);
                } else if (key.equals("speedmultiplier")) {
                    floralOverclockMultiplier = (int) parseLong(value, 2L, 1L, 16L);
                } else if (key.equals("cooldownmultiplier")) {
                    floralOverclockCooldownMultiplier =
                            (int) parseLong(value, 3L, 1L, Integer.MAX_VALUE);
                } else if (key.equals("radius")) {
                    floralOverclockRadius = (int) parseLong(value, 3L, 1L, 8L);
                }
            }
            case "qol" -> {
                if (key.equals("enableeverlastingfertilizer")) {
                    enableEverlastingFertilizer = parseBoolean(value, true);
                }
            }
            default -> {
            }
        }
    }

    /** Lowercases a key and strips the underscores so {@code manaPerTick} and
     *  {@code mana_per_tick} are both accepted. */
    private static String normalize(String key) {
        return key.trim().replace("_", "").toLowerCase(Locale.ROOT);
    }

    private static boolean parseBoolean(String value, boolean fallback) {
        if (value.equalsIgnoreCase("true")) {
            return true;
        }
        if (value.equalsIgnoreCase("false")) {
            return false;
        }
        return fallback;
    }

    private static long parseLong(String value, long fallback, long min, long max) {
        try {
            long parsed = Long.parseLong(value.trim());
            return Math.max(min, Math.min(max, parsed));
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private static List<String> parseList(String value) {
        List<String> result = new ArrayList<>();
        Matcher matcher = QUOTED.matcher(value);
        while (matcher.find()) {
            String id = matcher.group(1).trim();
            if (id.contains(":")) {
                result.add(id);
            }
        }
        return List.copyOf(result);
    }

    /** Removes a trailing comment while leaving {@code #} inside quotes alone. */
    private static String stripComment(String line) {
        boolean quoted = false;
        for (int index = 0; index < line.length(); index++) {
            char character = line.charAt(index);
            if (character == '"') {
                quoted = !quoted;
            } else if (character == '#' && !quoted) {
                return line.substring(0, index);
            }
        }
        return line;
    }

    private static int bracketDepth(String value) {
        int depth = 0;
        boolean quoted = false;
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (character == '"') {
                quoted = !quoted;
            } else if (!quoted && character == '[') {
                depth++;
            } else if (!quoted && character == ']') {
                depth--;
            }
        }
        return depth;
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

    public static boolean runeTemplateEnabled() {
        return enableRuneTemplate;
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

    public static int runeTemplateManaCost() {
        return runeTemplateManaCost;
    }

    public static boolean runeAcceleratorEnabled() {
        return enableRuneAccelerator;
    }

    public static int runeAcceleratorManaPerTick() {
        return runeAcceleratorManaPerTick;
    }

    public static int acceleratorMaxReach() {
        return acceleratorMaxReach;
    }

    public static int acceleratorMaxOffset() {
        return acceleratorMaxOffset;
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

    /** Applies the configured channel loss and returns the amount that reaches the buffer. */
    public static int applyLoss(int amount) {
        int loss = lossPercent();
        if (loss <= 0) {
            return amount;
        }
        if (loss >= 100) {
            return 0;
        }
        return (int) ((long) amount * (100 - loss) / 100L);
    }

    /** Returns false when the dimension id is listed in the blacklist. */
    public static boolean isDimensionAllowed(@Nullable ResourceLocation dimensionId) {
        return dimensionId == null
                || !dimensionBlacklist.contains(dimensionId.toString());
    }

    /**
     * Central runtime gate for both the well and the wireless bridge.
     *
     * <p>Dedicated servers must opt in explicitly; integrated servers are allowed by default.
     */
    public static boolean canOperate(Level level) {
        if (level.isClientSide) {
            return false;
        }
        MinecraftServer server = level.getServer();
        if (server == null) {
            return false;
        }
        if (server.isDedicatedServer() && !allowOnDedicatedServer()) {
            return false;
        }
        return isDimensionAllowed(level.dimension().location());
    }

    private BotaniaAdditionConfig() {
    }
}
