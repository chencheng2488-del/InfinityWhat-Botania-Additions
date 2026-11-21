package com.bandybus.infwhat.botania.crafting;

import com.bandybus.infwhat.botania.InfinityWhatBotania;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import vazkii.botania.common.crafting.RecipeTerraPlate;

/** Recipe serializers used by the optional progression-skipping recipes. */
public final class ModRecipeSerializers {

    public static RecipeSerializer<GatedShapelessRecipe> GATED_SHAPELESS;
    public static RecipeSerializer<RecipeTerraPlate> GATED_TERRA_PLATE;

    public static void register() {
        GATED_SHAPELESS = Registry.register(BuiltInRegistries.RECIPE_SERIALIZER,
                new ResourceLocation(InfinityWhatBotania.MOD_ID, "gated_shapeless"),
                new GatedShapelessRecipeSerializer());
        GATED_TERRA_PLATE = Registry.register(BuiltInRegistries.RECIPE_SERIALIZER,
                new ResourceLocation(InfinityWhatBotania.MOD_ID, "gated_terra_plate"),
                new GatedTerraPlateRecipeSerializer());
    }

    private ModRecipeSerializers() {
    }
}
