package com.bandybus.infwhat.botania.crafting;

import com.bandybus.infwhat.botania.config.BotaniaAdditionConfig;
import net.minecraft.item.ItemStack;
import vazkii.botania.api.recipe.RecipeManaInfusion;

/**
 * Mana infusion recipe that only matches while its config switch is enabled.
 *
 * <p>The 1.12.2 Terrestrial Agglomeration Plate hard-codes its three input items, so the shorter
 * Terrasteel route is implemented as a mana infusion instead: an iron ingot on a diamond-block
 * catalyst, 100000 mana. The mana cost and the "replace the pearl/diamond chain" intent are kept;
 * only the crafting station differs from the 1.20.1 version, which could register a real plate
 * recipe.
 */
public class GatedManaInfusionRecipe extends RecipeManaInfusion {

	public GatedManaInfusionRecipe(ItemStack output, Object input, int mana) {
		super(output, input, mana);
	}

	@Override
	public boolean matches(ItemStack stack) {
		return BotaniaAdditionConfig.cheapTerrasteelRecipeEnabled() && super.matches(stack);
	}
}
