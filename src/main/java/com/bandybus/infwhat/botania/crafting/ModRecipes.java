package com.bandybus.infwhat.botania.crafting;

import com.bandybus.infwhat.botania.InfinityWhatBotania;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.item.crafting.ShapedRecipes;
import net.minecraft.item.crafting.ShapelessRecipes;
import net.minecraft.util.NonNullList;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import vazkii.botania.api.BotaniaAPI;
import vazkii.botania.common.block.ModBlocks;

/** All crafting and infusion recipes of the add-on. */
public final class ModRecipes {

	private static final Item MANA_RESOURCE = vazkii.botania.common.item.ModItems.manaResource;
	private static final Item RUNE = vazkii.botania.common.item.ModItems.rune;

	private static GatedManaInfusionRecipe terrasteelShortcut;

	public static GatedManaInfusionRecipe terrasteelShortcut() {
		return terrasteelShortcut;
	}

	public static void register() {
		Item livingrock = Item.getItemFromBlock(ModBlocks.livingrock);
		Item livingwood = Item.getItemFromBlock(ModBlocks.livingwood);

		Ingredient rock = stack(livingrock, 0);
		Ingredient wood = stack(livingwood, 0);
		Ingredient powder = stack(MANA_RESOURCE, 23);
		Ingredient pearl = stack(MANA_RESOURCE, 1);
		Ingredient diamond = stack(MANA_RESOURCE, 2);
		Ingredient terrasteel = stack(MANA_RESOURCE, 4);
		Ingredient lifeEssence = stack(MANA_RESOURCE, 5);
		Ingredient manasteel = stack(MANA_RESOURCE, 0);
		Ingredient runeMana = stack(RUNE, 8);

		shaped("mana_well", rock, powder, rock,
				powder, diamond, powder,
				rock, powder, rock);
		shaped("mana_beacon", rock, pearl, rock,
				powder, diamond, powder,
				rock, pearl, rock);
		shaped("mana_anchor", rock, powder, rock,
				pearl, diamond, pearl,
				rock, powder, rock);
		shaped("rune_accelerator", rock, manasteel, rock,
				manasteel, runeMana, manasteel,
				rock, manasteel, rock);
		shaped("terrasteel_accelerator", rock, terrasteel, rock,
				terrasteel, diamond, terrasteel,
				rock, terrasteel, rock);
		shaped("floral_overclock", wood, stack(Items.REDSTONE, 0), wood,
				stack(Items.REDSTONE, 0), diamond, stack(Items.REDSTONE, 0),
				wood, stack(Items.REDSTONE, 0), wood);
		shaped("gaia_beacon", terrasteel, lifeEssence, terrasteel,
				lifeEssence, diamond, lifeEssence,
				terrasteel, lifeEssence, terrasteel);
		shaped("gaia_charm", empty(), lifeEssence, empty(),
				lifeEssence, diamond, lifeEssence,
				empty(), lifeEssence, empty());
		shaped("rune_template", rock, stack(Items.PAPER, 0), rock,
				stack(Items.PAPER, 0), diamond, stack(Items.PAPER, 0),
				rock, stack(Items.PAPER, 0), rock);

		shapeless("channel_linker",
				new ItemStack(com.bandybus.infwhat.botania.item.ModItems.CHANNEL_LINKER),
				stack(MANA_RESOURCE, 3), powder, stack(Items.ENDER_PEARL, 0));
		shapeless("everlasting_fertilizer",
				new ItemStack(com.bandybus.infwhat.botania.item.ModItems.EVERLASTING_FERTILIZER),
				stack(vazkii.botania.common.item.ModItems.fertilizer, 0),
				diamond, stack(Items.DYE, 15));

		// Gated progress-skip recipes. They stay registered while the config switch is off so
		// the gate is evaluated at match time.
		ForgeRegistries.RECIPES.register(new GatedShapelessRecipe("",
				new ItemStack(MANA_RESOURCE, 1, 5),
				ingredients(stack(MANA_RESOURCE, 4), stack(MANA_RESOURCE, 7),
						stack(MANA_RESOURCE, 8), stack(MANA_RESOURCE, 8),
						stack(MANA_RESOURCE, 2)))
				.setRegistryName(InfinityWhatBotania.MOD_ID, "gaia_spirit"));

		GatedManaInfusionRecipe shortcut = new GatedManaInfusionRecipe(
				new ItemStack(MANA_RESOURCE, 1, 4),
				new ItemStack(Items.IRON_INGOT), 100_000);
		shortcut.setCatalyst(Blocks.DIAMOND_BLOCK.getDefaultState());
		BotaniaAPI.manaInfusionRecipes.add(shortcut);
		terrasteelShortcut = shortcut;
	}

	private static void shaped(String name, Ingredient... grid) {
		NonNullList<Ingredient> ingredients = NonNullList.create();
		for (Ingredient ingredient : grid) {
			ingredients.add(ingredient);
		}
		ForgeRegistries.RECIPES.register(new ShapedRecipes("", 3, 3, ingredients,
				new ItemStack(blockOf(name))).setRegistryName(InfinityWhatBotania.MOD_ID, name));
	}

	private static void shapeless(String name, ItemStack result, Ingredient... grid) {
		NonNullList<Ingredient> ingredients = NonNullList.create();
		for (Ingredient ingredient : grid) {
			ingredients.add(ingredient);
		}
		ForgeRegistries.RECIPES.register(new ShapelessRecipes("", result, ingredients)
				.setRegistryName(InfinityWhatBotania.MOD_ID, name));
	}

	private static NonNullList<Ingredient> ingredients(Ingredient... grid) {
		NonNullList<Ingredient> ingredients = NonNullList.create();
		for (Ingredient ingredient : grid) {
			ingredients.add(ingredient);
		}
		return ingredients;
	}

	private static Ingredient stack(Item item, int meta) {
		return Ingredient.fromStacks(new ItemStack(item, 1, meta));
	}

	private static Ingredient empty() {
		return Ingredient.EMPTY;
	}

	private static Item blockOf(String name) {
		switch (name) {
			case "mana_well":
				return Item.getItemFromBlock(com.bandybus.infwhat.botania.block.ModBlocks.MANA_WELL);
			case "mana_beacon":
				return Item.getItemFromBlock(com.bandybus.infwhat.botania.block.ModBlocks.MANA_BEACON);
			case "mana_anchor":
				return Item.getItemFromBlock(com.bandybus.infwhat.botania.block.ModBlocks.MANA_ANCHOR);
			case "rune_accelerator":
				return Item.getItemFromBlock(
						com.bandybus.infwhat.botania.block.ModBlocks.RUNE_ACCELERATOR);
			case "terrasteel_accelerator":
				return Item.getItemFromBlock(
						com.bandybus.infwhat.botania.block.ModBlocks.TERRASTEEL_ACCELERATOR);
			case "floral_overclock":
				return Item.getItemFromBlock(
						com.bandybus.infwhat.botania.block.ModBlocks.FLORAL_OVERCLOCK);
			case "gaia_beacon":
				return Item.getItemFromBlock(
						com.bandybus.infwhat.botania.block.ModBlocks.GAIA_BEACON);
			case "gaia_charm":
				return com.bandybus.infwhat.botania.item.ModItems.GAIA_CHARM;
			case "rune_template":
				return com.bandybus.infwhat.botania.item.ModItems.RUNE_TEMPLATE;
			default:
				throw new IllegalArgumentException("Unknown recipe output " + name);
		}
	}

	private ModRecipes() {
	}
}
