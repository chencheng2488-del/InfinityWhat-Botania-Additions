package com.bandybus.infwhat.botania.item;

import com.bandybus.infwhat.botania.InfinityWhatBotania;
import com.bandybus.infwhat.botania.config.BotaniaAdditionConfig;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import vazkii.botania.common.block.ModBlocks;

import java.util.ArrayList;
import java.util.List;

/**
 * Reusable floral fertilizer.
 *
 * <p>Places Botania flowers in the same area as Botania's own fertilizer, but never consumes the
 * item. 1.7.10 Botania stores all 16 mystical colours in one block with a metadata value.
 */
public class ItemEverlastingFertilizer extends Item {

    private static final int RANGE = 3;

    private IIcon baseIcon;
    private IIcon overlayIcon;

    public ItemEverlastingFertilizer() {
        this.setUnlocalizedName(InfinityWhatBotania.MODID + ".everlasting_fertilizer");
        this.setTextureName(ModItems.name("everlasting_fertilizer_overlay"));
        this.setMaxStackSize(1);
        this.setCreativeTab(com.bandybus.infwhat.botania.util.CreativeTabHelper.coreTab());
    }

    @Override
    public boolean onItemUse(ItemStack stack, EntityPlayer player, World world,
                             int x, int y, int z, int side,
                             float hitX, float hitY, float hitZ) {
        if (!BotaniaAdditionConfig.everlastingFertilizerEnabled()) {
            return false;
        }
        if (world.isRemote) {
            for (int index = 0; index < 15; index++) {
                double px = x - RANGE + world.rand.nextInt(RANGE * 2 + 1) + Math.random();
                double py = y + 1 + Math.random();
                double pz = z - RANGE + world.rand.nextInt(RANGE * 2 + 1) + Math.random();
                world.spawnParticle("happyVillager", px, py, pz, 0.0D, 0.05D, 0.0D);
            }
            return true;
        }

        List<int[]> validPositions = new ArrayList<int[]>();
        for (int dx = -4; dx <= 2; dx++) {
            for (int dz = -4; dz <= 2; dz++) {
                for (int dy = 2; dy >= -2; dy--) {
                    int px = x + dx + 1;
                    int py = y + dy + 1;
                    int pz = z + dz + 1;
                    if (!world.isAirBlock(px, py, pz)) {
                        continue;
                    }
                    if (world.provider.isHellWorld && py >= 255) {
                        continue;
                    }
                    if (ModBlocks.flower.canBlockStay(world, px, py, pz)) {
                        validPositions.add(new int[]{px, py, pz});
                    }
                }
            }
        }

        int attempts = Math.min(validPositions.size(), world.rand.nextBoolean() ? 3 : 4);
        for (int index = 0; index < attempts; index++) {
            int[] chosen = validPositions.remove(world.rand.nextInt(validPositions.size()));
            world.setBlock(chosen[0], chosen[1], chosen[2], ModBlocks.flower,
                    world.rand.nextInt(16), 3);
        }
        return true;
    }

    @Override
    public boolean requiresMultipleRenderPasses() {
        return true;
    }

    @Override
    public int getRenderPasses(int metadata) {
        return 2;
    }

    @Override
    public IIcon getIconFromDamageForRenderPass(int damage, int pass) {
        return pass == 0 ? baseIcon : overlayIcon;
    }

    @Override
    public IIcon getIconFromDamage(int damage) {
        return baseIcon;
    }

    @Override
    public void registerIcons(IIconRegister register) {
        baseIcon = register.registerIcon("botania:fertilizer");
        overlayIcon = register.registerIcon(ModItems.name("everlasting_fertilizer_overlay"));
    }

    @Override
    @SuppressWarnings("unchecked")
    public void addInformation(ItemStack stack, EntityPlayer player, java.util.List tooltip,
                               boolean advanced) {
        super.addInformation(stack, player, tooltip, advanced);
        tooltip.add(StatCollector.translateToLocal(
                "tooltip.infwhat_botania.everlasting_fertilizer"));
    }
}

