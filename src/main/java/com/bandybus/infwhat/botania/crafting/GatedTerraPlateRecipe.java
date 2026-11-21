package com.bandybus.infwhat.botania.crafting;

import com.bandybus.infwhat.botania.config.BotaniaAdditionConfig;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import vazkii.botania.common.crafting.RecipeTerraPlate;

/**
 * Terrestrial Agglomeration Plate recipe that only matches while its config switch is enabled.
 *
 * <p>Extending Botania's own recipe keeps the plate's item validation and mana handling intact;
 * only the match check is gated.
 */
public class GatedTerraPlateRecipe extends RecipeTerraPlate {

    public GatedTerraPlateRecipe(ResourceLocation id, int mana,
                                 NonNullList<Ingredient> ingredients, ItemStack result) {
        super(id, mana, ingredients, result);
    }

    @Override
    public boolean matches(IInventory container, World level) {
        return BotaniaAdditionConfig.cheapTerrasteelRecipeEnabled()
                && super.matches(container, level);
    }

    @Override
    public IRecipeSerializer<RecipeTerraPlate> getSerializer() {
        return ModRecipeSerializers.GATED_TERRA_PLATE.get();
    }
}
