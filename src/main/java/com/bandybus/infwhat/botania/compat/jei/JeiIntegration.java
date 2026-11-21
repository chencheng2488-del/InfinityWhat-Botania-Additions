package com.bandybus.infwhat.botania.compat.jei;

import com.bandybus.infwhat.botania.InfinityWhatBotania;
import com.bandybus.infwhat.botania.block.ModBlocks;
import com.bandybus.infwhat.botania.config.BotaniaAdditionConfig;
import com.bandybus.infwhat.botania.item.ModItems;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.recipe.IRecipeManager;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeManager;
import vazkii.botania.api.recipe.TerrestrialAgglomerationRecipe;
import vazkii.botania.client.integration.jei.TerrestrialAgglomerationRecipeCategory;

import java.util.List;

/**
 * Hides the gated recipes from JEI while their config switch is off.
 *
 * <p>The recipes stay registered on purpose (datapack removal and the crafting book keep working),
 * so JEI would otherwise list recipes that can never match. Hiding is re-applied periodically
 * because JEI rebuilds its list whenever a recipe sync arrives.
 */
public class JeiIntegration implements IModPlugin {

    private static final ResourceLocation PLUGIN_UID =
            new ResourceLocation(InfinityWhatBotania.MOD_ID, "jei_plugin");
    private static final ResourceLocation GAIA_SPIRIT =
            new ResourceLocation(InfinityWhatBotania.MOD_ID, "gaia_spirit");
    private static final ResourceLocation TERRASTEEL_SHORTCUT =
            new ResourceLocation(InfinityWhatBotania.MOD_ID, "terrasteel_shortcut");

    private IJeiRuntime runtime;
    private int tickCounter;

    @Override
    public ResourceLocation getPluginUid() {
        return PLUGIN_UID;
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        // The template is not an ingredient of any other recipe, so JEI's "uses" page would be
        // empty. An info page documents binding and production instead.
        registration.addIngredientInfo(new ItemStack(ModItems.RUNE_TEMPLATE),
                VanillaTypes.ITEM_STACK,
                Component.translatable("jei.infwhat_botania.rune_template.bind"),
                Component.translatable("jei.infwhat_botania.rune_template.produce",
                        BotaniaAdditionConfig.runeTemplateManaCost()));

        registration.addIngredientInfo(new ItemStack(ModItems.GAIA_CHARM),
                VanillaTypes.ITEM_STACK,
                Component.translatable("jei.infwhat_botania.gaia_charm.usage"));

        registration.addIngredientInfo(new ItemStack(ModBlocks.GAIA_BEACON.asItem()),
                VanillaTypes.ITEM_STACK,
                Component.translatable("jei.infwhat_botania.gaia_beacon.usage",
                        BotaniaAdditionConfig.gaiaBeaconCapacity()));
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime runtime) {
        this.runtime = runtime;
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (++tickCounter < 20) {
                return;
            }
            tickCounter = 0;
            applyVisibility();
        });
    }

    private void applyVisibility() {
        Minecraft minecraft = Minecraft.getInstance();
        if (runtime == null || minecraft.level == null) {
            return;
        }
        RecipeManager recipes = minecraft.level.getRecipeManager();
        IRecipeManager jei = runtime.getRecipeManager();

        if (BotaniaAdditionConfig.gaiaSpiritRecipeEnabled()) {
            recipes.byKey(GAIA_SPIRIT).ifPresent(recipe -> {
                if (recipe instanceof CraftingRecipe crafting) {
                    jei.unhideRecipes(RecipeTypes.CRAFTING, List.of(crafting));
                }
            });
        } else {
            recipes.byKey(GAIA_SPIRIT).ifPresent(recipe -> {
                if (recipe instanceof CraftingRecipe crafting) {
                    jei.hideRecipes(RecipeTypes.CRAFTING, List.of(crafting));
                }
            });
        }

        if (BotaniaAdditionConfig.cheapTerrasteelRecipeEnabled()) {
            recipes.byKey(TERRASTEEL_SHORTCUT).ifPresent(recipe -> {
                if (recipe instanceof TerrestrialAgglomerationRecipe terra) {
                    jei.unhideRecipes(TerrestrialAgglomerationRecipeCategory.TYPE, List.of(terra));
                }
            });
        } else {
            recipes.byKey(TERRASTEEL_SHORTCUT).ifPresent(recipe -> {
                if (recipe instanceof TerrestrialAgglomerationRecipe terra) {
                    jei.hideRecipes(TerrestrialAgglomerationRecipeCategory.TYPE, List.of(terra));
                }
            });
        }
    }
}
