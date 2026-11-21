package com.bandybus.infwhat.botania.compat.jei;

import com.bandybus.infwhat.botania.InfinityWhatBotania;
import com.bandybus.infwhat.botania.block.ModBlocks;
import com.bandybus.infwhat.botania.config.BotaniaAdditionConfig;
import com.bandybus.infwhat.botania.item.ModItems;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaRecipeCategoryUid;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.recipe.IRecipeManager;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.ICraftingRecipe;
import net.minecraft.item.crafting.RecipeManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.client.event.RecipesUpdatedEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import vazkii.botania.api.recipe.ITerraPlateRecipe;
import vazkii.botania.client.integration.jei.TerraPlateRecipeCategory;

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
        registration.addIngredientInfo(new ItemStack(ModItems.RUNE_TEMPLATE.get()),
                VanillaTypes.ITEM,
                new TranslationTextComponent("jei.infwhat_botania.rune_template.bind"),
                new TranslationTextComponent("jei.infwhat_botania.rune_template.produce",
                        Integer.valueOf(BotaniaAdditionConfig.runeTemplateManaCost())));

        registration.addIngredientInfo(new ItemStack(ModItems.GAIA_CHARM.get()),
                VanillaTypes.ITEM,
                new TranslationTextComponent("jei.infwhat_botania.gaia_charm.usage"));

        registration.addIngredientInfo(new ItemStack(ModBlocks.GAIA_BEACON.get().asItem()),
                VanillaTypes.ITEM,
                new TranslationTextComponent("jei.infwhat_botania.gaia_beacon.usage",
                        Integer.valueOf(BotaniaAdditionConfig.gaiaBeaconCapacity())));
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

        java.util.Optional<? extends net.minecraft.item.crafting.IRecipe<?>> gaia =
                recipes.byKey(GAIA_SPIRIT);
        if (gaia.isPresent() && gaia.get() instanceof ICraftingRecipe) {
            Object recipe = gaia.get();
            if (BotaniaAdditionConfig.gaiaSpiritRecipeEnabled()) {
                jei.unhideRecipe(recipe, VanillaRecipeCategoryUid.CRAFTING);
            } else {
                jei.hideRecipe(recipe, VanillaRecipeCategoryUid.CRAFTING);
            }
        }

        java.util.Optional<? extends net.minecraft.item.crafting.IRecipe<?>> terra =
                recipes.byKey(TERRASTEEL_SHORTCUT);
        if (terra.isPresent() && terra.get() instanceof ITerraPlateRecipe) {
            Object recipe = terra.get();
            if (BotaniaAdditionConfig.cheapTerrasteelRecipeEnabled()) {
                jei.unhideRecipe(recipe, TerraPlateRecipeCategory.UID);
            } else {
                jei.hideRecipe(recipe, TerraPlateRecipeCategory.UID);
            }
        }
    }
}
