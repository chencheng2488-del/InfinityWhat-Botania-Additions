package com.bandybus.infwhat.botania.recipes;

import com.bandybus.infwhat.botania.block.ModBlocks;
import com.bandybus.infwhat.botania.config.BotaniaAdditionConfig;
import com.bandybus.infwhat.botania.item.ModItems;
import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

/**
 * Crafting recipes for the add-on.
 *
 * <p>1.7.10 has no JSON recipes and no Terrestrial Agglomeration Plate recipe API, so the two
 * gated shortcuts use registered crafting-table recipes instead. They only exist while their
 * config switch is enabled (both default to off), which keeps the gate identical to the modern
 * releases.
 */
public final class ModRecipes {

    private static final int MANA_POWDER = 23;
    private static final int MANA_DIAMOND = 2;
    private static final int MANA_PEARL = 1;
    private static final int LIVINGWOOD_TWIG = 3;
    private static final int TERRASTEEL = 4;
    private static final int LIFE_ESSENCE = 5;
    private static final int ELEMENTIUM = 7;
    private static final int PIXIE_DUST = 8;
    private static final int RUNE_MANA = 8;

    private ModRecipes() {
    }

    public static void register() {
        ItemStack livingrock = new ItemStack(
                vazkii.botania.common.block.ModBlocks.livingrock, 1, 0);
        ItemStack livingwood = new ItemStack(
                vazkii.botania.common.block.ModBlocks.livingwood, 1, 0);
        ItemStack powder = resource(MANA_POWDER);
        ItemStack diamond = resource(MANA_DIAMOND);
        ItemStack pearl = resource(MANA_PEARL);
        ItemStack terrasteel = resource(TERRASTEEL);
        ItemStack lifeEssence = resource(LIFE_ESSENCE);

        GameRegistry.addRecipe(new ItemStack(ModBlocks.MANA_WELL), "LGL", "GMG", "LGL",
                'L', livingrock, 'G', powder, 'M', diamond);
        GameRegistry.addRecipe(new ItemStack(ModBlocks.MANA_BEACON), "LPL", "GDG", "LPL",
                'L', livingrock, 'P', pearl, 'G', powder, 'D', diamond);
        GameRegistry.addRecipe(new ItemStack(ModBlocks.MANA_ANCHOR), "LGL", "PDP", "LGL",
                'L', livingrock, 'G', powder, 'P', pearl, 'D', diamond);
        GameRegistry.addRecipe(new ItemStack(ModBlocks.RUNE_ACCELERATOR), "LML", "MRM", "LML",
                'L', livingrock, 'M', resource(0), 'R', rune(RUNE_MANA));
        GameRegistry.addRecipe(new ItemStack(ModBlocks.TERRASTEEL_ACCELERATOR),
                "LTL", "TDT", "LTL", 'L', livingrock, 'T', terrasteel, 'D', diamond);
        GameRegistry.addRecipe(new ItemStack(ModBlocks.FLORAL_OVERCLOCK), "LRL", "RDR", "LRL",
                'L', livingwood, 'R', new ItemStack(Items.redstone), 'D', diamond);
        GameRegistry.addRecipe(new ItemStack(ModBlocks.GAIA_BEACON), "TGT", "GDG", "TGT",
                'T', terrasteel, 'G', lifeEssence, 'D', diamond);
        GameRegistry.addRecipe(new ItemStack(ModItems.RUNE_TEMPLATE), "LPL", "PMP", "LPL",
                'L', livingrock, 'P', new ItemStack(Items.paper), 'M', diamond);
        GameRegistry.addRecipe(new ItemStack(ModItems.GAIA_CHARM), " G ", "GDG", " G ",
                'G', lifeEssence, 'D', diamond);

        GameRegistry.addShapelessRecipe(new ItemStack(ModItems.CHANNEL_LINKER),
                resource(LIVINGWOOD_TWIG), powder, new ItemStack(Items.ender_pearl));
        GameRegistry.addShapelessRecipe(new ItemStack(ModItems.EVERLASTING_FERTILIZER),
                new ItemStack(vazkii.botania.common.item.ModItems.fertilizer),
                diamond, new ItemStack(Items.dye, 1, 15));

        if (BotaniaAdditionConfig.gaiaSpiritRecipeEnabled()) {
            GameRegistry.addShapelessRecipe(lifeEssence.copy(), terrasteel,
                    resource(ELEMENTIUM), resource(PIXIE_DUST), resource(PIXIE_DUST), diamond);
        }
        if (BotaniaAdditionConfig.cheapTerrasteelRecipeEnabled()) {
            // Platform equivalent: 1.7.10 has no terra plate recipe registry.
            GameRegistry.addShapelessRecipe(terrasteel.copy(),
                    new ItemStack(Items.iron_ingot), new ItemStack(Items.diamond), diamond);
        }
    }

    private static ItemStack resource(int metadata) {
        return new ItemStack(vazkii.botania.common.item.ModItems.manaResource, 1, metadata);
    }

    private static ItemStack rune(int metadata) {
        return new ItemStack(vazkii.botania.common.item.ModItems.rune, 1, metadata);
    }
}
