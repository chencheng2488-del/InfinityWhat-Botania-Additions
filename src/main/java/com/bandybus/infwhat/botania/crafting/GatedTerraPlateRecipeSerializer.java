package com.bandybus.infwhat.botania.crafting;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import vazkii.botania.common.crafting.RecipeTerraPlate;

/** Reads {@link GatedTerraPlateRecipe} from JSON and the network. */
public class GatedTerraPlateRecipeSerializer implements RecipeSerializer<RecipeTerraPlate> {

    @Override
    public RecipeTerraPlate fromJson(ResourceLocation id, JsonObject json) {
        int mana = GsonHelper.getAsInt(json, "mana");
        NonNullList<Ingredient> ingredients = NonNullList.create();
        for (JsonElement element : GsonHelper.getAsJsonArray(json, "ingredients")) {
            ingredients.add(Ingredient.fromJson(element));
        }
        ItemStack result = GatedShapelessRecipeSerializer.itemStackFromJson(
                GsonHelper.getAsJsonObject(json, "result"));
        return new GatedTerraPlateRecipe(id, mana, ingredients, result);
    }

    @Override
    public RecipeTerraPlate fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
        int mana = buffer.readVarInt();
        int size = buffer.readVarInt();
        NonNullList<Ingredient> ingredients = NonNullList.withSize(size, Ingredient.EMPTY);
        for (int index = 0; index < size; index++) {
            ingredients.set(index, Ingredient.fromNetwork(buffer));
        }
        ItemStack result = buffer.readItem();
        return new GatedTerraPlateRecipe(id, mana, ingredients, result);
    }

    @Override
    public void toNetwork(FriendlyByteBuf buffer, RecipeTerraPlate recipe) {
        buffer.writeVarInt(recipe.getMana());
        buffer.writeVarInt(recipe.getIngredients().size());
        for (Ingredient ingredient : recipe.getIngredients()) {
            ingredient.toNetwork(buffer);
        }
        buffer.writeItem(recipe.getResultItem(RegistryAccess.EMPTY));
    }
}
