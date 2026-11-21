package com.bandybus.infwhat.botania.crafting;

import com.bandybus.infwhat.botania.InfinityWhatBotania;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import vazkii.botania.common.crafting.RecipeTerraPlate;

/** Recipe serializers used by the optional progression-skipping recipes. */
public final class ModRecipeSerializers {

    public static final DeferredRegister<IRecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS,
                    InfinityWhatBotania.MOD_ID);

    public static final RegistryObject<IRecipeSerializer<GatedShapelessRecipe>> GATED_SHAPELESS =
            SERIALIZERS.register("gated_shapeless", GatedShapelessRecipeSerializer::new);

    public static final RegistryObject<IRecipeSerializer<RecipeTerraPlate>> GATED_TERRA_PLATE =
            SERIALIZERS.register("gated_terra_plate", GatedTerraPlateRecipeSerializer::new);

    private ModRecipeSerializers() {
    }

    public static void register(IEventBus eventBus) {
        SERIALIZERS.register(eventBus);
    }
}
