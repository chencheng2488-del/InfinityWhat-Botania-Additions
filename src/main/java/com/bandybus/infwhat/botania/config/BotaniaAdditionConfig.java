package com.bandybus.infwhat.botania.config;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Server/common configuration for the Botania addition.
 *
 * <p>All mana production is gated by a global switch, a dedicated-server switch, a per-tick
 * cap and a dimension blacklist. The defaults are deliberately conservative: the mana well
 * produces a finite amount per tick and the wireless bridge never creates mana by itself.
 */
public final class BotaniaAdditionConfig {

    public static final ForgeConfigSpec SPEC;
    public static final Common COMMON;

    static {
        Pair<Common, ForgeConfigSpec> pair = new ForgeConfigSpec.Builder().configure(Common::new);
        COMMON = pair.getLeft();
        SPEC = pair.getRight();
    }

    public static final class Common {
        public final ForgeConfigSpec.BooleanValue enableManaWell;
        public final ForgeConfigSpec.BooleanValue allowOnDedicatedServer;
        public final ForgeConfigSpec.IntValue manaPerTick;
        public final ForgeConfigSpec.IntValue maxManaPerTick;
        public final ForgeConfigSpec.IntValue radius;
        public final ForgeConfigSpec.BooleanValue enableWireless;
        public final ForgeConfigSpec.LongValue maxPerChannel;
        public final ForgeConfigSpec.IntValue lossPercent;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> dimensionBlacklist;
        public final ForgeConfigSpec.BooleanValue enableGaiaSpiritRecipe;
        public final ForgeConfigSpec.BooleanValue enableCheapTerrasteelRecipe;
        public final ForgeConfigSpec.BooleanValue enableGaiaCharm;
        public final ForgeConfigSpec.BooleanValue gaiaCharmHardMode;
        public final ForgeConfigSpec.BooleanValue gaiaCharmConsumes;
        public final ForgeConfigSpec.BooleanValue enableGaiaBeacon;
        public final ForgeConfigSpec.IntValue gaiaBeaconManaCost;
        public final ForgeConfigSpec.IntValue gaiaBeaconCapacity;
        public final ForgeConfigSpec.IntValue gaiaBeaconRadius;
        public final ForgeConfigSpec.BooleanValue enableRuneTemplate;
        public final ForgeConfigSpec.IntValue runeTemplateManaCost;
        public final ForgeConfigSpec.BooleanValue enableRuneAccelerator;
        public final ForgeConfigSpec.IntValue acceleratorMaxReach;
        public final ForgeConfigSpec.IntValue acceleratorMaxOffset;
        public final ForgeConfigSpec.IntValue runeAcceleratorManaPerTick;
        public final ForgeConfigSpec.IntValue runeAcceleratorRadius;
        public final ForgeConfigSpec.IntValue runeAcceleratorCooldownMultiplier;
        public final ForgeConfigSpec.BooleanValue enableTerrasteelAccelerator;
        public final ForgeConfigSpec.IntValue terrasteelAcceleratorManaPerTick;
        public final ForgeConfigSpec.IntValue terrasteelAcceleratorRadius;
        public final ForgeConfigSpec.BooleanValue enableFloralOverclock;
        public final ForgeConfigSpec.IntValue floralOverclockMultiplier;
        public final ForgeConfigSpec.IntValue floralOverclockCooldownMultiplier;
        public final ForgeConfigSpec.IntValue floralOverclockRadius;
        public final ForgeConfigSpec.BooleanValue enableEverlastingFertilizer;

        private Common(ForgeConfigSpec.Builder builder) {
            builder.comment("Infinity What: Botania Additions settings.").push("mana_well");
            this.enableManaWell = builder
                    .comment("Master switch for the Mana Well's output.",
                            "When false the block still exists, but provides no mana.")
                    .define("enableManaWell", true);
            this.allowOnDedicatedServer = builder
                    .comment("Allow the Mana Well and the wireless bridge to work on a dedicated server.",
                            "Defaults to false so a server administrator must opt in explicitly.")
                    .define("allowOnDedicatedServer", false);
            this.manaPerTick = builder
                    .comment("Mana produced per tick by one Mana Well.",
                            "The well shares this budget between all of its linked outputs.")
                    .defineInRange("manaPerTick", 1_000, 1, Integer.MAX_VALUE);
            this.maxManaPerTick = builder
                    .comment("Upper bound accepted by the config loader. Values above this are rejected.",
                            "Kept as a separate key so administrators can tighten the hard cap.")
                    .defineInRange("maxManaPerTick", 1_000_000, 1, Integer.MAX_VALUE);
            this.radius = builder
                    .comment("Default cube radius scanned by the Mana Well for nearby mana pools.",
                            "The well and the anchor can override this per block in their GUI.")
                    .defineInRange("radius", 4, 1, 8);
            builder.pop();

            builder.comment("Wireless mana endpoints.").push("mana_wireless");
            this.enableWireless = builder
                    .comment("Master switch for the Mana Beacon and Mana Anchor.",
                            "When false both blocks stay inert and keep their links.")
                    .define("enableWireless", true);
            this.maxPerChannel = builder
                    .comment("Maximum mana buffered by one implicit network.",
                            "The buffer is only a routing buffer; it is not an unlimited battery.")
                    .defineInRange("maxPerChannel", 1_000_000L, 1L, Long.MAX_VALUE);
            this.lossPercent = builder
                    .comment("Percentage of mana lost when a beacon deposits into the network.",
                            "0 keeps the transfer lossless; 100 voids everything the beacon extracts.")
                    .defineInRange("lossPercent", 0, 0, 100);
            builder.pop();

            builder.comment("Dimensions in which the addition must not operate.").push("dimensions");
            this.dimensionBlacklist = builder
                    .comment("Dimension ids, for example \"minecraft:the_nether\".")
                    .defineList("blacklist", List.of(),
                            obj -> obj instanceof String s && s.contains(":"));
            builder.pop();

            builder.comment("Optional recipes that skip Botania progression.",
                            "Both are off by default; enable them deliberately.")
                    .push("recipes");
            this.enableGaiaSpiritRecipe = builder
                    .comment("Allow crafting botania:life_essence without fighting the Gaia Guardian.",
                            "The recipe is always registered but never matches while this is false.")
                    .define("enableGaiaSpiritRecipe", false);
            this.enableCheapTerrasteelRecipe = builder
                    .comment("Allow an iron + diamond + mana diamond recipe on the Terrestrial Agglomeration Plate.",
                            "Costs 100000 mana instead of the usual 500000 and skips the mana pearl.",
                            "The recipe is always registered but never matches while this is false.")
                    .define("enableCheapTerrasteelRecipe", false);
            builder.pop();

            builder.comment("Gaia Charm.",
                            "Summons the Gaia Guardian without a beacon, Gaia Pylons or a clear arena.",
                            "Progression skip: off by default.")
                    .push("gaia_charm");
            this.enableGaiaCharm = builder
                    .comment("Enable the Gaia Charm.")
                    .define("enable", false);
            this.gaiaCharmHardMode = builder
                    .comment("Summon hard mode (Gaia Guardian II).")
                    .define("hardMode", false);
            this.gaiaCharmConsumes = builder
                    .comment("Consume one charm per summon.",
                            "When false the charm is reusable.")
                    .define("consumeOnUse", true);
            builder.pop();

            builder.comment("Gaia Beacon.",
                            "Finishes an ongoing Gaia fight once the boss bar is ready.",
                            "It does not summon anything; use the Gaia Charm for that.",
                            "Costs mana from a nearby mana pool and credits nearby players.")
                    .push("gaia_beacon");
            this.enableGaiaBeacon = builder
                    .comment("Enable the Gaia Beacon block.")
                    .define("enable", false);
            this.gaiaBeaconManaCost = builder
                    .comment("Mana taken from a nearby pool for each finished fight.")
                    .defineInRange("manaCost", 500_000, 0, 1_000_000);
            this.gaiaBeaconCapacity = builder
                    .comment("Internal mana buffer of the Gaia Beacon.",
                            "A Mana Well or a Mana Anchor can fill it like a mana pool.")
                    .defineInRange("capacity", 1_000_000, 0, Integer.MAX_VALUE);
            this.gaiaBeaconRadius = builder
                    .comment("Cube radius used to find the guardian and the players to credit.")
                    .defineInRange("radius", 24, 4, 64);
            builder.pop();

            builder.comment("Reusable Rune Template.",
                            "Bind it to a rune by sneak-right-clicking with a rune in the inventory.",
                            "Right-click with livingrock and a nearby mana pool to produce the bound rune.")
                    .push("rune_template");
            this.enableRuneTemplate = builder
                    .comment("Enable the Rune Template.")
                    .define("enable", false);
            this.runeTemplateManaCost = builder
                    .comment("Mana consumed per rune produced by the template.")
                    .defineInRange("manaCost", 5_200, 0, 1_000_000);
            builder.pop();

            builder.comment("Shared limits for the three accelerators.").push("accelerators");
            this.acceleratorMaxReach = builder
                    .comment("Maximum reach on each axis for the accelerator range boxes.",
                            "Individual accelerators can use a smaller reach in their GUI.")
                    .defineInRange("maxReach", 8, 1, 8);
            this.acceleratorMaxOffset = builder
                    .comment("Maximum absolute offset on each axis for the accelerator range boxes.")
                    .defineInRange("maxOffset", 8, 0, 8);
            builder.pop();

            builder.comment("Runic Altar accelerator.",
                            "Fills nearby altars and triggers the wand step once the mana is full.",
                            "When an accelerator is nearby, the altar's 60 tick cooldown ticks down",
                            "cooldownMultiplier times per tick.")
                    .push("rune_accelerator");
            this.enableRuneAccelerator = builder
                    .comment("Enable the Runic Altar Accelerator block.")
                    .define("enable", false);
            this.runeAcceleratorManaPerTick = builder
                    .comment("Default mana injected into each nearby altar per tick.",
                            "Can be changed per block in its GUI.")
                    .defineInRange("manaPerTick", 1_000, 1, Integer.MAX_VALUE);
            this.runeAcceleratorRadius = builder
                    .comment("Default cube radius scanned for Runic Altars.",
                            "Can be changed per block in its GUI.",
                            "Also used to decide whether an altar is close enough for the cooldown boost.")
                    .defineInRange("radius", 3, 1, 8);
            this.runeAcceleratorCooldownMultiplier = builder
                    .comment("Default multiplier for the runic altar's crafting cooldown.",
                            "3 means the cooldown ticks down three times as fast.",
                            "1 disables the cooldown boost. Can be changed per block in its GUI.")
                    .defineInRange("cooldownMultiplier", 3, 1, Integer.MAX_VALUE);
            builder.pop();

            builder.comment("Terrestrial Agglomeration Plate accelerator.",
                            "Injects mana into nearby plates; the plate itself still performs the craft.")
                    .push("terrasteel_accelerator");
            this.enableTerrasteelAccelerator = builder
                    .comment("Enable the Terrasteel Plate Accelerator block.")
                    .define("enable", false);
            this.terrasteelAcceleratorManaPerTick = builder
                    .comment("Default mana injected into each nearby plate per tick.",
                            "Can be changed per block in its GUI.")
                    .defineInRange("manaPerTick", 10_000, 1, Integer.MAX_VALUE);
            this.terrasteelAcceleratorRadius = builder
                    .comment("Default cube radius scanned for Terrestrial Agglomeration Plates.",
                            "Can be changed per block in its GUI.")
                    .defineInRange("radius", 3, 1, 8);
            builder.pop();

            builder.comment("Generating flower overclock.",
                            "Calls the flower tick extra times. Fuel, lifetime and particles also speed up.")
                    .push("floral_overclock");
            this.enableFloralOverclock = builder
                    .comment("Enable the Floral Overclock block.")
                    .define("enable", false);
            this.floralOverclockMultiplier = builder
                    .comment("Default tick multiplier for generating flowers. 1 disables the effect.",
                            "Hard capped at 16 to keep the server tick safe.",
                            "Can be changed per block in its GUI.")
                    .defineInRange("speedMultiplier", 2, 1, 16);
            this.floralOverclockCooldownMultiplier = builder
                    .comment("Cooldown speed for generating flowers such as Thermalily.",
                            "The remaining cooldown is divided by this value every tick, so 3 is",
                            "already almost instant and very large values clear it immediately.",
                            "1 disables the extra cooldown acceleration.")
                    .defineInRange("cooldownMultiplier", 3, 1, Integer.MAX_VALUE);
            this.floralOverclockRadius = builder
                    .comment("Default cube radius scanned for generating flowers.",
                            "Can be changed per block in its GUI.")
                    .defineInRange("radius", 3, 1, 8);
            builder.pop();

            builder.comment("Quality of life items.").push("qol");
            this.enableEverlastingFertilizer = builder
                    .comment("Enable the reusable Everlasting Fertilizer.")
                    .define("enableEverlastingFertilizer", true);
            builder.pop();
        }
    }

    public static boolean manaWellEnabled() {
        return COMMON.enableManaWell.get();
    }

    public static boolean wirelessEnabled() {
        return COMMON.enableWireless.get();
    }

    public static boolean allowOnDedicatedServer() {
        return COMMON.allowOnDedicatedServer.get();
    }

    public static int manaPerTick() {
        return Math.min(COMMON.manaPerTick.get(), COMMON.maxManaPerTick.get());
    }

    public static int maxManaPerTick() {
        return COMMON.maxManaPerTick.get();
    }

    public static int radius() {
        return COMMON.radius.get();
    }

    public static long maxPerChannel() {
        return COMMON.maxPerChannel.get();
    }

    public static int lossPercent() {
        return COMMON.lossPercent.get();
    }

    public static boolean gaiaSpiritRecipeEnabled() {
        return COMMON.enableGaiaSpiritRecipe.get();
    }

    public static boolean cheapTerrasteelRecipeEnabled() {
        return COMMON.enableCheapTerrasteelRecipe.get();
    }

    public static boolean runeTemplateEnabled() {
        return COMMON.enableRuneTemplate.get();
    }

    public static boolean gaiaCharmEnabled() {
        return COMMON.enableGaiaCharm.get();
    }

    public static boolean gaiaCharmHardMode() {
        return COMMON.gaiaCharmHardMode.get();
    }

    public static boolean gaiaCharmConsumes() {
        return COMMON.gaiaCharmConsumes.get();
    }

    public static boolean gaiaBeaconEnabled() {
        return COMMON.enableGaiaBeacon.get();
    }

    public static int gaiaBeaconManaCost() {
        return COMMON.gaiaBeaconManaCost.get();
    }

    public static int gaiaBeaconCapacity() {
        return COMMON.gaiaBeaconCapacity.get();
    }

    public static int gaiaBeaconRadius() {
        return COMMON.gaiaBeaconRadius.get();
    }

    public static int runeTemplateManaCost() {
        return COMMON.runeTemplateManaCost.get();
    }

    public static boolean runeAcceleratorEnabled() {
        return COMMON.enableRuneAccelerator.get();
    }

    public static int runeAcceleratorManaPerTick() {
        return COMMON.runeAcceleratorManaPerTick.get();
    }

    public static int acceleratorMaxReach() {
        return COMMON.acceleratorMaxReach.get();
    }

    public static int acceleratorMaxOffset() {
        return COMMON.acceleratorMaxOffset.get();
    }

    public static int runeAcceleratorRadius() {
        return COMMON.runeAcceleratorRadius.get();
    }

    public static int runeAcceleratorCooldownMultiplier() {
        return COMMON.runeAcceleratorCooldownMultiplier.get();
    }

    public static boolean terrasteelAcceleratorEnabled() {
        return COMMON.enableTerrasteelAccelerator.get();
    }

    public static int terrasteelAcceleratorManaPerTick() {
        return COMMON.terrasteelAcceleratorManaPerTick.get();
    }

    public static int terrasteelAcceleratorRadius() {
        return COMMON.terrasteelAcceleratorRadius.get();
    }

    public static boolean floralOverclockEnabled() {
        return COMMON.enableFloralOverclock.get();
    }

    public static int floralOverclockMultiplier() {
        return COMMON.floralOverclockMultiplier.get();
    }

    public static int floralOverclockCooldownMultiplier() {
        return COMMON.floralOverclockCooldownMultiplier.get();
    }

    public static int floralOverclockRadius() {
        return COMMON.floralOverclockRadius.get();
    }

    public static boolean everlastingFertilizerEnabled() {
        return COMMON.enableEverlastingFertilizer.get();
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
                || !COMMON.dimensionBlacklist.get().contains(dimensionId.toString());
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

    public static void register(FMLJavaModLoadingContext context) {
        context.registerConfig(ModConfig.Type.COMMON, SPEC, "infwhat_botania.toml");
    }

    private BotaniaAdditionConfig() {
    }
}
