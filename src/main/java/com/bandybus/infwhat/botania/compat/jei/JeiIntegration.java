package com.bandybus.infwhat.botania.compat.jei;

import com.bandybus.infwhat.botania.InfinityWhatBotania;
import com.bandybus.infwhat.botania.block.ModBlocks;
import com.bandybus.infwhat.botania.config.BotaniaAdditionConfig;
import com.bandybus.infwhat.botania.item.ModItems;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.recipe.IRecipeManager;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraftforge.client.event.RecipesUpdatedEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import vazkii.botania.api.recipe.TerrestrialAgglomerationRecipe;
import vazkii.botania.client.integration.jei.TerrestrialAgglomerationRecipeCategory;

import java.util.List;

/**
 * Hides the gated recipes from JEI while their config switch is off.
 *
 * <p>The recipes stay registered on purpose (datapack removal and the crafting book keep working),
 * so JEI would otherwise list recipes that can never match. Hiding is re-applied after every
 * recipe sync because JEI rebuilds its list at that point.
 */
@JeiPlugin
public class JeiIntegration implements IModPlugin {

    private static final ResourceLocation PLUGIN_UID =
            new ResourceLocation(InfinityWhatBotania.MOD_ID, "jei_plugin");
    private static final ResourceLocation GAIA_SPIRIT =
            new ResourceLocation(InfinityWhatBotania.MOD_ID, "gaia_spirit");
    private static final ResourceLocation TERRASTEEL_SHORTCUT =
            new ResourceLocation(InfinityWhatBotania.MOD_ID, "terrasteel_shortcut");

    private IJeiRuntime runtime;
    private boolean registered;
    private boolean pending;

    @Override
    public ResourceLocation getPluginUid() {
        return PLUGIN_UID;
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        // The template is not an ingredient of any other recipe, so JEI's "uses" page would be
        // empty. An info page documents binding and production instead.
        registration.addIngredientInfo(ModItems.RUNE_TEMPLATE.get(),
                Component.translatable("jei.infwhat_botania.rune_template.bind"),
                Component.translatable("jei.infwhat_botania.rune_template.produce",
                        BotaniaAdditionConfig.runeTemplateManaCost()));

        registration.addIngredientInfo(ModItems.GAIA_CHARM.get(),
                Component.translatable("jei.infwhat_botania.gaia_charm.usage"));

        registration.addIngredientInfo(ModBlocks.GAIA_BEACON.get().asItem(),
                Component.translatable("jei.infwhat_botania.gaia_beacon.usage",
                        BotaniaAdditionConfig.gaiaBeaconCapacity()));
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime runtime) {
        this.runtime = runtime;
        if (!registered) {
            registered = true;
            MinecraftForge.EVENT_BUS.register(this);
        }
        pending = true;
    }

    @SubscribeEvent
    public void onRecipesUpdated(RecipesUpdatedEvent event) {
        pending = true;
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !pending) {
            return;
        }
        pending = false;
        applyVisibility();
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
