package com.bandybus.infwhat.botania.compat.jei;

import com.bandybus.infwhat.botania.InfinityWhatBotania;
import com.bandybus.infwhat.botania.block.ModBlocks;
import com.bandybus.infwhat.botania.config.BotaniaAdditionConfig;
import com.bandybus.infwhat.botania.crafting.GatedManaInfusionRecipe;
import com.bandybus.infwhat.botania.crafting.ModRecipes;
import com.bandybus.infwhat.botania.item.ModItems;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.IModRegistry;
import mezz.jei.api.IRecipeRegistry;
import mezz.jei.api.IJeiRuntime;
import mezz.jei.api.JEIPlugin;
import mezz.jei.api.ingredients.VanillaTypes;
import mezz.jei.api.recipe.IRecipeWrapper;
import mezz.jei.api.recipe.VanillaRecipeCategoryUid;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import vazkii.botania.client.integration.jei.manapool.ManaPoolRecipeCategory;

/**
 * Hides the gated recipes from JEI while their config switch is off.
 *
 * <p>The recipes stay registered on purpose (the gate lives in {@code matches}), so JEI would
 * otherwise list recipes that can never match. Visibility is re-applied periodically because JEI
 * rebuilds its list whenever a recipe sync arrives.
 */
@JEIPlugin
public class JeiIntegration implements IModPlugin {

	private static final ResourceLocation GAIA_SPIRIT =
			new ResourceLocation(InfinityWhatBotania.MOD_ID, "gaia_spirit");

	private IJeiRuntime runtime;
	private int tickCounter;
	private boolean busRegistered;

	@Override
	public void register(IModRegistry registry) {
		// The template is not an ingredient of any other recipe, so JEI's "uses" page would be
		// empty. An info page documents binding and production instead.
		registry.addIngredientInfo(new ItemStack(ModItems.RUNE_TEMPLATE), VanillaTypes.ITEM,
				"jei.infwhat_botania.rune_template.bind",
				I18n.format("jei.infwhat_botania.rune_template.produce",
						BotaniaAdditionConfig.runeTemplateManaCost()));

		registry.addIngredientInfo(new ItemStack(ModItems.GAIA_CHARM), VanillaTypes.ITEM,
				"jei.infwhat_botania.gaia_charm.usage");

		registry.addIngredientInfo(new ItemStack(ModBlocks.GAIA_BEACON), VanillaTypes.ITEM,
				I18n.format("jei.infwhat_botania.gaia_beacon.usage",
						BotaniaAdditionConfig.gaiaBeaconCapacity()));
	}

	@Override
	public void onRuntimeAvailable(IJeiRuntime runtime) {
		this.runtime = runtime;
		if (!busRegistered) {
			busRegistered = true;
			MinecraftForge.EVENT_BUS.register(this);
		}
		applyVisibility();
	}

	@SubscribeEvent
	public void onClientTick(TickEvent.ClientTickEvent event) {
		if (event.phase != TickEvent.Phase.END) {
			return;
		}
		if (++tickCounter < 20) {
			return;
		}
		tickCounter = 0;
		applyVisibility();
	}

	private void applyVisibility() {
		if (runtime == null || Minecraft.getMinecraft().world == null) {
			return;
		}
		IRecipeRegistry recipes = runtime.getRecipeRegistry();

		IRecipe gaiaSpirit = ForgeRegistries.RECIPES.getValue(GAIA_SPIRIT);
		if (gaiaSpirit != null) {
			IRecipeWrapper wrapper = recipes.getRecipeWrapper(gaiaSpirit,
					VanillaRecipeCategoryUid.CRAFTING);
			if (wrapper != null) {
				if (BotaniaAdditionConfig.gaiaSpiritRecipeEnabled()) {
					recipes.unhideRecipe(wrapper, VanillaRecipeCategoryUid.CRAFTING);
				} else {
					recipes.hideRecipe(wrapper, VanillaRecipeCategoryUid.CRAFTING);
				}
			}
		}

		GatedManaInfusionRecipe shortcut = ModRecipes.terrasteelShortcut();
		if (shortcut != null) {
			IRecipeWrapper wrapper = recipes.getRecipeWrapper(shortcut,
					ManaPoolRecipeCategory.UID);
			if (wrapper != null) {
				if (BotaniaAdditionConfig.cheapTerrasteelRecipeEnabled()) {
					recipes.unhideRecipe(wrapper, ManaPoolRecipeCategory.UID);
				} else {
					recipes.hideRecipe(wrapper, ManaPoolRecipeCategory.UID);
				}
			}
		}
	}
}
