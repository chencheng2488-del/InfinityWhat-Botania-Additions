package com.bandybus.infwhat.botania.crafting;

import com.bandybus.infwhat.botania.config.BotaniaAdditionConfig;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.ShapelessRecipes;
import net.minecraft.util.NonNullList;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.world.World;

/**
 * Shapeless crafting recipe that only matches while its config switch is enabled.
 *
 * <p>The recipe stays registered so the recipe book and the config gate keep working; the gate
 * lives in {@link #matches}, which is what both the crafting menu and every crafter call.
 */
public class GatedShapelessRecipe extends ShapelessRecipes {

	public GatedShapelessRecipe(String group, ItemStack output, NonNullList<Ingredient> ingredients) {
		super(group, output, ingredients);
	}

	@Override
	public boolean matches(InventoryCrafting inventory, World world) {
		return BotaniaAdditionConfig.gaiaSpiritRecipeEnabled()
				&& super.matches(inventory, world);
	}
}
