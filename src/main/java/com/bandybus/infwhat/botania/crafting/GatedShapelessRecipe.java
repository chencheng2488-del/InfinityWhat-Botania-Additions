package com.bandybus.infwhat.botania.crafting;

import com.bandybus.infwhat.botania.config.BotaniaAdditionConfig;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.Level;

/**
 * Shapeless crafting recipe that only matches while its config switch is enabled.
 *
 * <p>The recipe stays registered so the recipe book and datapack removal keep working; the gate
 * lives in {@link #matches}, which is what both the crafting menu and every crafter call.
 */
public class GatedShapelessRecipe extends ShapelessRecipe {

    public GatedShapelessRecipe(ResourceLocation id, String group, ItemStack result,
                                NonNullList<Ingredient> ingredients) {
        super(id, group, CraftingBookCategory.MISC, result, ingredients);
    }

    @Override
    public boolean matches(CraftingContainer container, Level level) {
        return BotaniaAdditionConfig.gaiaSpiritRecipeEnabled() && super.matches(container, level);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeSerializers.GATED_SHAPELESS.get();
    }
}
