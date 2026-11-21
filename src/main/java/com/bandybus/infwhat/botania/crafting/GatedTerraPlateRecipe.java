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
        return BotaniaAdditionConfig.cheapTerrasteelRecipeEnabled()
                && super.matches(container, level);
    }

    @Override
    public RecipeSerializer<RecipeTerraPlate> getSerializer() {
        return ModRecipeSerializers.GATED_TERRA_PLATE.get();
    }
}
