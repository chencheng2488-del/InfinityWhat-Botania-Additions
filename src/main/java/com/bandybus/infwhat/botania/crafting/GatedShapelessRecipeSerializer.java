package com.bandybus.infwhat.botania.crafting;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;

/** Reads {@link GatedShapelessRecipe} from JSON and the network. */
public class GatedShapelessRecipeSerializer implements RecipeSerializer<GatedShapelessRecipe> {

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
        ItemStack result = itemStackFromJson(GsonHelper.getAsJsonObject(json, "result"));
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
        buffer.writeItem(recipe.getResultItem(RegistryAccess.EMPTY));
    }

    /** Reads the plain {@code {"item": "...", "count": n}} result object used by our recipes. */
    static ItemStack itemStackFromJson(JsonObject json) {
        ResourceLocation id = new ResourceLocation(GsonHelper.getAsString(json, "item"));
        int count = GsonHelper.getAsInt(json, "count", 1);
        Item item = BuiltInRegistries.ITEM.get(id);
        return new ItemStack(item, count);
    }
}
