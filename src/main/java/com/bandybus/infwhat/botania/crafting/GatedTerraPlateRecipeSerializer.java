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
import vazkii.botania.common.crafting.RecipeTerraPlate;

/** Reads {@link GatedTerraPlateRecipe} from JSON and the network. */
public class GatedTerraPlateRecipeSerializer
        extends net.minecraftforge.registries.ForgeRegistryEntry<IRecipeSerializer<?>>
        implements IRecipeSerializer<RecipeTerraPlate> {

    @Override
    public RecipeTerraPlate fromJson(ResourceLocation id, JsonObject json) {
        int mana = JSONUtils.getAsInt(json, "mana");
        NonNullList<Ingredient> ingredients = NonNullList.create();
        for (JsonElement element : JSONUtils.getAsJsonArray(json, "ingredients")) {
            ingredients.add(Ingredient.fromJson(element));
        }
        ItemStack result = CraftingHelper.getItemStack(
                JSONUtils.getAsJsonObject(json, "result"), true);
        return new GatedTerraPlateRecipe(id, mana, ingredients, result);
    }

    @Override
    public RecipeTerraPlate fromNetwork(ResourceLocation id, PacketBuffer buffer) {
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
    public void toNetwork(PacketBuffer buffer, RecipeTerraPlate recipe) {
        buffer.writeVarInt(recipe.getMana());
        buffer.writeVarInt(recipe.getIngredients().size());
        for (Ingredient ingredient : recipe.getIngredients()) {
            ingredient.toNetwork(buffer);
        }
        buffer.writeItem(recipe.getResultItem());
    }
}
