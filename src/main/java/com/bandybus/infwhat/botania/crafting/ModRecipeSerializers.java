package com.bandybus.infwhat.botania.crafting;

import com.bandybus.infwhat.botania.InfinityWhatBotania;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import vazkii.botania.common.crafting.RecipeTerraPlate;

/** Recipe serializers used by the optional progression-skipping recipes. */
public final class ModRecipeSerializers {

    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, InfinityWhatBotania.MOD_ID);

    public static final RegistryObject<RecipeSerializer<GatedShapelessRecipe>> GATED_SHAPELESS =
            SERIALIZERS.register("gated_shapeless", GatedShapelessRecipeSerializer::new);

    public static final RegistryObject<RecipeSerializer<RecipeTerraPlate>> GATED_TERRA_PLATE =
            SERIALIZERS.register("gated_terra_plate", GatedTerraPlateRecipeSerializer::new);

    private ModRecipeSerializers() {
    }

    public static void register(IEventBus eventBus) {
        SERIALIZERS.register(eventBus);
    }
}
