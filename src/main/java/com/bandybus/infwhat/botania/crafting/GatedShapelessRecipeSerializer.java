package com.bandybus.infwhat.botania.crafting;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.JSONUtils;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.crafting.CraftingHelper;

/** Reads {@link GatedShapelessRecipe} from JSON and the network. */
public class GatedShapelessRecipeSerializer
        extends net.minecraftforge.registries.ForgeRegistryEntry<IRecipeSerializer<?>>
        implements IRecipeSerializer<GatedShapelessRecipe> {

    @Override
    public GatedShapelessRecipe fromJson(ResourceLocation id, JsonObject json) {
        NonNullList<Ingredient> ingredients = NonNullList.create();
        for (JsonElement element : JSONUtils.getAsJsonArray(json, "ingredients")) {
            Ingredient ingredient = Ingredient.fromJson(element);
            if (!ingredient.isEmpty()) {
                ingredients.add(ingredient);
            }
        }
        String group = JSONUtils.getAsString(json, "group", "");
        ItemStack result = CraftingHelper.getItemStack(
                JSONUtils.getAsJsonObject(json, "result"), true);
        return new GatedShapelessRecipe(id, group, result, ingredients);
    }

    @Override
    public GatedShapelessRecipe fromNetwork(ResourceLocation id, PacketBuffer buffer) {
        String group = buffer.readUtf(32767);
        int size = buffer.readVarInt();
        NonNullList<Ingredient> ingredients = NonNullList.withSize(size, Ingredient.EMPTY);
        for (int index = 0; index < size; index++) {
            ingredients.set(index, Ingredient.fromNetwork(buffer));
        }
        ItemStack result = buffer.readItem();
        return new GatedShapelessRecipe(id, group, result, ingredients);
    }

    @Override
    public void toNetwork(PacketBuffer buffer, GatedShapelessRecipe recipe) {
        buffer.writeUtf(recipe.getGroup());
        buffer.writeVarInt(recipe.getIngredients().size());
        for (Ingredient ingredient : recipe.getIngredients()) {
            ingredient.toNetwork(buffer);
        }
        buffer.writeItem(recipe.getResultItem());
    }
}
