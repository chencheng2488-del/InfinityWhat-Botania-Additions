package com.bandybus.infwhat.botania.crafting;

import com.bandybus.infwhat.botania.config.BotaniaAdditionConfig;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import vazkii.botania.common.crafting.RecipeTerraPlate;

/**
 * Terrestrial Agglomeration Plate recipe that only matches while its config switch is enabled.
 *
 * <p>Extending Botania's own recipe keeps the plate's item validation and mana handling intact;
 * only the match check is gated.
 */
public class GatedTerraPlateRecipe extends RecipeTerraPlate {

    public GatedTerraPlateRecipe(ResourceLocation id, int mana, NonNullList<Ingredient> ingredients,
                                 ItemStack result) {
        super(id, mana, ingredients, result);
    }

    @Override
    public boolean matches(Container container, Level level) {
        if (!BotaniaAdditionConfig.cheapTerrasteelRecipeEnabled()) {
            return false;
        }
        // Botania 1.18.2 compares the plate's ingredients with a used-slot set. The production
        // RecipeTerraPlate#matches override is SRG-named at compile time, so the equivalent check
        // is implemented here instead of calling super.matches.
        int nonEmpty = 0;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            // Botania's RecipeUtils stops at the first empty slot, so a gap ends the item list.
            if (container.getItem(slot).isEmpty()) {
                break;
            }
            if (container.getItem(slot).getCount() > 1) {
                return false;
            }
            nonEmpty++;
        }
        boolean[] used = new boolean[nonEmpty];
        for (Ingredient ingredient : getIngredients()) {
            boolean found = false;
            for (int slot = 0; slot < nonEmpty; slot++) {
                if (!used[slot] && ingredient.test(container.getItem(slot))) {
                    used[slot] = true;
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }
        int usedSlots = 0;
        for (boolean value : used) {
            if (value) {
                usedSlots++;
            }
        }
        return usedSlots == nonEmpty;
    }

    @Override
    public RecipeSerializer<RecipeTerraPlate> getSerializer() {
        return ModRecipeSerializers.GATED_TERRA_PLATE.get();
    }
}
