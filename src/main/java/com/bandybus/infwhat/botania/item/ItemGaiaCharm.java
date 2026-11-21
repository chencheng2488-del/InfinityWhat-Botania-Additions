package com.bandybus.infwhat.botania.item;

import com.bandybus.infwhat.botania.InfinityWhatBotania;
import com.bandybus.infwhat.botania.config.BotaniaAdditionConfig;
import com.bandybus.infwhat.botania.util.GaiaSummonHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.MathHelper;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

/**
 * Summons the Gaia Guardian without building Botania's beacon and Gaia Pylon structure.
 *
 * <p>The boss is still created by Botania's own entity, so health scaling, boss bar, music, drops
 * and the hard mode behaviour stay identical. Only the structure validation is skipped.
 */
public class ItemGaiaCharm extends Item {

    public ItemGaiaCharm() {
        this.setUnlocalizedName(InfinityWhatBotania.MODID + ".gaia_charm");
        this.setTextureName(ModItems.name("gaia_charm"));
        this.setMaxStackSize(16);
        this.setCreativeTab(com.bandybus.infwhat.botania.util.CreativeTabHelper.coreTab());
    }

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        if (world.isRemote) {
            return stack;
        }
        if (!BotaniaAdditionConfig.gaiaCharmEnabled()) {
            player.addChatMessage(new ChatComponentTranslation(
                    "message.infwhat_botania.gaia_charm.disabled"));
            return stack;
        }

        boolean hardMode = BotaniaAdditionConfig.gaiaCharmHardMode();
        int x = MathHelper.floor_double(player.posX);
        int y = MathHelper.floor_double(player.posY);
        int z = MathHelper.floor_double(player.posZ);
        if (!GaiaSummonHelper.summon(player, world, x, y, z, hardMode)) {
            player.addChatMessage(new ChatComponentTranslation(
                    "message.infwhat_botania.gaia_charm.failed"));
            return stack;
        }

        if (BotaniaAdditionConfig.gaiaCharmConsumes()
                && !player.capabilities.isCreativeMode) {
            stack.stackSize--;
        }
        player.addChatMessage(new ChatComponentTranslation(hardMode
                ? "message.infwhat_botania.gaia_charm.summoned_hard"
                : "message.infwhat_botania.gaia_charm.summoned"));
        return stack;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void addInformation(ItemStack stack, EntityPlayer player, java.util.List tooltip,
                               boolean advanced) {
        super.addInformation(stack, player, tooltip, advanced);
        if (!BotaniaAdditionConfig.gaiaCharmEnabled()) {
            tooltip.add(StatCollector.translateToLocal(
                    "tooltip.infwhat_botania.gaia_charm.disabled"));
        }
        tooltip.add(StatCollector.translateToLocal(
                "tooltip.infwhat_botania.gaia_charm.usage"));
        tooltip.add(StatCollector.translateToLocal(BotaniaAdditionConfig.gaiaCharmHardMode()
                ? "tooltip.infwhat_botania.gaia_charm.hard"
                : "tooltip.infwhat_botania.gaia_charm.normal"));
        tooltip.add(StatCollector.translateToLocal(BotaniaAdditionConfig.gaiaCharmConsumes()
                ? "tooltip.infwhat_botania.gaia_charm.consumed"
                : "tooltip.infwhat_botania.gaia_charm.reusable"));
    }
}

