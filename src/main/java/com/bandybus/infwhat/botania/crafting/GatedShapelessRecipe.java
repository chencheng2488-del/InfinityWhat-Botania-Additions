package com.bandybus.infwhat.botania.crafting;

import com.bandybus.infwhat.botania.config.BotaniaAdditionConfig;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.item.crafting.ShapelessRecipe;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

/**
 * Shapeless crafting recipe that only matches while its config switch is enabled.
 *
 * <p>The recipe stays registered so the recipe book and datapack removal keep working; the gate
 * lives in {@link #matches}, which is what both the crafting menu and every crafter call.
 */
public class GatedShapelessRecipe extends ShapelessRecipe {

    public GatedShapelessRecipe(ResourceLocation id, String group, ItemStack result,
                                NonNullList<Ingredient> ingredients) {
        super(id, group, result, ingredients);
    }

    @Override
    public boolean matches(CraftingInventory container, World level) {
        return BotaniaAdditionConfig.gaiaSpiritRecipeEnabled()
                && super.matches(container, level);
    }

    @Override
    public IRecipeSerializer<?> getSerializer() {
        return ModRecipeSerializers.GATED_SHAPELESS.get();
    }
}
