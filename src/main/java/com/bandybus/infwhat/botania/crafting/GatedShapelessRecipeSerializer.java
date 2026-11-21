package com.bandybus.infwhat.botania.crafting;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.registries.ForgeRegistryEntry;

/** Reads {@link GatedShapelessRecipe} from JSON and the network. */
public class GatedShapelessRecipeSerializer
        extends ForgeRegistryEntry<RecipeSerializer<?>>
        implements RecipeSerializer<GatedShapelessRecipe> {

    @Override
    public GatedShapelessRecipe fromJson(ResourceLocation id, JsonObject json) {
        NonNullList<Ingredient> ingredients = NonNullList.create();
        for (JsonElement element : GsonHelper.getAsJsonArray(json, "ingredients")) {
            Ingredient ingredient = Ingredient.fromJson(element);
            if (!ingredient.isEmpty()) {
                ingredients.add(ingredient);
            }
        }
        String group = GsonHelper.getAsString(json, "group", "");
        ItemStack result = CraftingHelper.getItemStack(GsonHelper.getAsJsonObject(json, "result"), true);
        return new GatedShapelessRecipe(id, group, result, ingredients);
    }

    @Override
    public GatedShapelessRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
        String group = buffer.readUtf();
        int size = buffer.readVarInt();
        NonNullList<Ingredient> ingredients = NonNullList.withSize(size, Ingredient.EMPTY);
        for (int index = 0; index < size; index++) {
            ingredients.set(index, Ingredient.fromNetwork(buffer));
        }
        ItemStack result = buffer.readItem();
        return new GatedShapelessRecipe(id, group, result, ingredients);
    }

    @Override
    public void toNetwork(FriendlyByteBuf buffer, GatedShapelessRecipe recipe) {
        buffer.writeUtf(recipe.getGroup());
        buffer.writeVarInt(recipe.getIngredients().size());
        for (Ingredient ingredient : recipe.getIngredients()) {
            ingredient.toNetwork(buffer);
        }
        buffer.writeItem(recipe.getResultItem());
    }
}
