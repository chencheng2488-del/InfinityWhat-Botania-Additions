package com.bandybus.infwhat.botania.config;

import com.bandybus.infwhat.botania.InfinityWhatBotania;
import net.minecraft.world.World;
import net.minecraftforge.common.config.Configuration;

import java.io.File;

/**
 * Server/common configuration for the Botania addition, stored in {@code config/infwhat_botania.cfg}.
 *
 * <p>All mana production is gated by a global switch, a dedicated-server switch, a per-tick cap
 * and a dimension blacklist. The defaults are deliberately conservative: the mana well produces a
 * finite amount per tick and the wireless bridge never creates mana by itself.
 */
public final class BotaniaAdditionConfig {

	private static final String MANA_WELL = "mana_well";
	private static final String MANA_WIRELESS = "mana_wireless";
	private static final String DIMENSIONS = "dimensions";
	private static final String RECIPES = "recipes";
	private static final String GAIA_CHARM = "gaia_charm";
	private static final String GAIA_BEACON = "gaia_beacon";
	private static final String RUNE_TEMPLATE = "rune_template";
	private static final String ACCELERATORS = "accelerators";
	private static final String RUNE_ACCELERATOR = "rune_accelerator";
	private static final String TERRASTEEL_ACCELERATOR = "terrasteel_accelerator";
	private static final String FLORAL_OVERCLOCK = "floral_overclock";
	private static final String QOL = "qol";

	private static boolean enableManaWell = true;
	private static boolean allowOnDedicatedServer;
	private static int manaPerTick = 1_000;
	private static int maxManaPerTick = 1_000_000;
	private static int radius = 4;
	private static boolean enableWireless = true;
	private static long maxPerChannel = 1_000_000L;
	private static int lossPercent;
	private static int[] dimensionBlacklist = new int[0];
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

	public static void load(File file) {
		Configuration config = new Configuration(file);
		try {
			config.load();

			enableManaWell = config.getBoolean("enableManaWell", MANA_WELL, true,
					"Master switch for the Mana Well's output. When false the block still exists, but provides no mana.");
			allowOnDedicatedServer = config.getBoolean("allowOnDedicatedServer", MANA_WELL, false,
					"Allow the Mana Well and the wireless bridge to work on a dedicated server.");
			manaPerTick = config.getInt("manaPerTick", MANA_WELL, 1_000, 1, Integer.MAX_VALUE,
					"Mana produced per tick by one Mana Well. The well shares this budget between all of its linked outputs.");
			maxManaPerTick = config.getInt("maxManaPerTick", MANA_WELL, 1_000_000, 1, Integer.MAX_VALUE,
					"Upper bound accepted by the config loader. Values above this are rejected.");
			radius = config.getInt("radius", MANA_WELL, 4, 1, 8,
					"Default cube radius scanned by the Mana Well for nearby mana pools.");

			enableWireless = config.getBoolean("enableWireless", MANA_WIRELESS, true,
					"Master switch for the Mana Beacon and Mana Anchor.");
			maxPerChannel = config.getInt("maxPerChannel", MANA_WIRELESS, 1_000_000, 1, Integer.MAX_VALUE,
					"Maximum mana buffered by one implicit network.");
			lossPercent = config.getInt("lossPercent", MANA_WIRELESS, 0, 0, 100,
					"Percentage of mana lost when a beacon deposits into the network.");

			dimensionBlacklist = config.get(DIMENSIONS, "blacklist", new int[0],
					"Dimension ids in which the addition must not operate, e.g. -1 for the Nether.")
					.getIntList();

			enableGaiaSpiritRecipe = config.getBoolean("enableGaiaSpiritRecipe", RECIPES, false,
					"Allow crafting Botania's Gaia Spirit without fighting the Gaia Guardian.");
			enableCheapTerrasteelRecipe = config.getBoolean("enableCheapTerrasteelRecipe", RECIPES, false,
					"Allow a shorter, gated Terrasteel route. In 1.12.2 this is a mana infusion: an iron ingot on a diamond block catalyst, 100000 mana.");

			enableGaiaCharm = config.getBoolean("enable", GAIA_CHARM, false,
					"Enable the Gaia Charm.");
			gaiaCharmHardMode = config.getBoolean("hardMode", GAIA_CHARM, false,
					"Summon hard mode (Gaia Guardian II).");
			gaiaCharmConsumes = config.getBoolean("consumeOnUse", GAIA_CHARM, true,
					"Consume one charm per summon. When false the charm is reusable.");

			enableGaiaBeacon = config.getBoolean("enable", GAIA_BEACON, false,
					"Enable the Gaia Beacon block.");
			gaiaBeaconManaCost = config.getInt("manaCost", GAIA_BEACON, 500_000, 0, 1_000_000,
					"Mana taken from a nearby pool for each finished fight.");
			gaiaBeaconCapacity = config.getInt("capacity", GAIA_BEACON, 1_000_000, 0, Integer.MAX_VALUE,
					"Internal mana buffer of the Gaia Beacon.");
			gaiaBeaconRadius = config.getInt("radius", GAIA_BEACON, 24, 4, 64,
					"Cube radius used to find the guardian and the players to credit.");

			enableRuneTemplate = config.getBoolean("enable", RUNE_TEMPLATE, false,
					"Enable the Rune Template.");
			runeTemplateManaCost = config.getInt("manaCost", RUNE_TEMPLATE, 5_200, 0, 1_000_000,
					"Mana consumed per rune produced by the template.");

			acceleratorMaxReach = config.getInt("maxReach", ACCELERATORS, 8, 1, 8,
					"Maximum reach on each axis for the accelerator range boxes.");
			acceleratorMaxOffset = config.getInt("maxOffset", ACCELERATORS, 8, 0, 8,
					"Maximum absolute offset on each axis for the accelerator range boxes.");

			enableRuneAccelerator = config.getBoolean("enable", RUNE_ACCELERATOR, false,
					"Enable the Runic Altar Accelerator block.");
			runeAcceleratorManaPerTick = config.getInt("manaPerTick", RUNE_ACCELERATOR, 1_000, 1, Integer.MAX_VALUE,
					"Default mana injected into each nearby altar per tick.");
			runeAcceleratorRadius = config.getInt("radius", RUNE_ACCELERATOR, 3, 1, 8,
					"Default cube radius scanned for Runic Altars.");
			runeAcceleratorCooldownMultiplier = config.getInt("cooldownMultiplier", RUNE_ACCELERATOR, 3, 1, Integer.MAX_VALUE,
					"Default multiplier for the runic altar's crafting cooldown.");

			enableTerrasteelAccelerator = config.getBoolean("enable", TERRASTEEL_ACCELERATOR, false,
					"Enable the Terrasteel Plate Accelerator block.");
			terrasteelAcceleratorManaPerTick = config.getInt("manaPerTick", TERRASTEEL_ACCELERATOR, 10_000, 1, Integer.MAX_VALUE,
					"Default mana injected into each nearby plate per tick.");
			terrasteelAcceleratorRadius = config.getInt("radius", TERRASTEEL_ACCELERATOR, 3, 1, 8,
					"Default cube radius scanned for Terrestrial Agglomeration Plates.");

			enableFloralOverclock = config.getBoolean("enable", FLORAL_OVERCLOCK, false,
					"Enable the Floral Overclock block.");
			floralOverclockMultiplier = config.getInt("speedMultiplier", FLORAL_OVERCLOCK, 2, 1, 16,
					"Default tick multiplier for generating flowers. 1 disables the effect.");
			floralOverclockCooldownMultiplier = config.getInt("cooldownMultiplier", FLORAL_OVERCLOCK, 3, 1, Integer.MAX_VALUE,
					"Cooldown speed for generating flowers such as Thermalily.");
			floralOverclockRadius = config.getInt("radius", FLORAL_OVERCLOCK, 3, 1, 8,
					"Default cube radius scanned for generating flowers.");

			enableEverlastingFertilizer = config.getBoolean("enableEverlastingFertilizer", QOL, true,
					"Enable the reusable Everlasting Fertilizer.");
		} catch (RuntimeException exception) {
			InfinityWhatBotania.LOGGER.warn(
					"Infinity What: Botania Additions failed to read {}, using defaults", file, exception);
		} finally {
			if (config.hasChanged()) {
				config.save();
			}
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
	public static boolean isDimensionAllowed(int dimension) {
		for (int blocked : dimensionBlacklist) {
			if (blocked == dimension) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Central runtime gate for both the well and the wireless bridge.
	 *
	 * <p>Dedicated servers must opt in explicitly; integrated servers are allowed by default.
	 */
	public static boolean canOperate(World world) {
		if (world == null || world.isRemote) {
			return false;
		}
		if (world.getMinecraftServer() == null) {
			return false;
		}
		if (world.getMinecraftServer().isDedicatedServer() && !allowOnDedicatedServer()) {
			return false;
		}
		return isDimensionAllowed(world.provider.getDimension());
	}

	private BotaniaAdditionConfig() {
	}
}
