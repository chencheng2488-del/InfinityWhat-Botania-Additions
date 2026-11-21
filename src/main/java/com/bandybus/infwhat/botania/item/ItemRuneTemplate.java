package com.bandybus.infwhat.botania.item;

import com.bandybus.infwhat.botania.InfinityWhatBotania;
import com.bandybus.infwhat.botania.config.BotaniaAdditionConfig;
import com.bandybus.infwhat.botania.util.ManaReceiverHelper;
import com.bandybus.infwhat.botania.util.Pos;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import vazkii.botania.api.mana.IManaPool;
import vazkii.botania.api.mana.IManaReceiver;

/**
 * Reusable Rune Template.
 *
 * <p>Sneak-right-click with a Botania rune in the inventory to bind the template to that rune.
 * Normal right-click consumes one livingrock and mana from a nearby pool, then produces the bound
 * rune. The template itself is never consumed.
 */
public class ItemRuneTemplate extends Item {

    public static final String TAG_RUNE = "Rune";
    private static final int SEARCH_RADIUS = 8;
    private static final int RUNE_TYPES = 16;

    public ItemRuneTemplate() {
        this.setUnlocalizedName(InfinityWhatBotania.MODID + ".rune_template");
        this.setTextureName(ModItems.name("rune_template"));
        this.setMaxStackSize(1);
        this.setCreativeTab(com.bandybus.infwhat.botania.util.CreativeTabHelper.coreTab());
    }

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        handleUse(world, player, stack);
        return stack;
    }

    @Override
    public boolean onItemUse(ItemStack stack, EntityPlayer player, World world,
                             int x, int y, int z, int side,
                             float hitX, float hitY, float hitZ) {
        handleUse(world, player, stack);
        return true;
    }

    private static void handleUse(World world, EntityPlayer player, ItemStack template) {
        if (world.isRemote) {
            return;
        }
        if (!BotaniaAdditionConfig.runeTemplateEnabled()) {
            player.addChatMessage(new ChatComponentTranslation(
                    "message.infwhat_botania.rune_template.disabled"));
            return;
        }
        if (player.isSneaking()) {
            bind(player, template);
            return;
        }
        produce(world, player, template);
    }

    private static void bind(EntityPlayer player, ItemStack template) {
        int runeSlot = findRuneSlot(player);
        if (runeSlot < 0) {
            player.addChatMessage(new ChatComponentTranslation(
                    "message.infwhat_botania.rune_template.no_rune"));
            return;
        }
        ItemStack rune = player.inventory.mainInventory[runeSlot];
        int metadata = rune.getMetadata();
        String runeName = new ItemStack(vazkii.botania.common.item.ModItems.rune,
                1, metadata).getDisplayName();
        NBTTagCompound tag = template.getTagCompound();
        if (tag == null) {
            tag = new NBTTagCompound();
            template.setTagCompound(tag);
        }
        tag.setInteger(TAG_RUNE, metadata);
        rune.stackSize--;
        if (rune.stackSize <= 0) {
            player.inventory.mainInventory[runeSlot] = null;
        }
        player.inventory.markDirty();
        player.addChatMessage(new ChatComponentTranslation(
                "message.infwhat_botania.rune_template.bound", runeName));
    }

    private static void produce(World world, EntityPlayer player, ItemStack template) {
        int metadata = boundRune(template);
        if (metadata < 0 || metadata >= RUNE_TYPES) {
            player.addChatMessage(new ChatComponentTranslation(
                    "message.infwhat_botania.rune_template.empty"));
            return;
        }

        int livingrockSlot = findLivingrockSlot(player);
        if (livingrockSlot < 0) {
            player.addChatMessage(new ChatComponentTranslation(
                    "message.infwhat_botania.rune_template.no_stone"));
            return;
        }

        int cost = BotaniaAdditionConfig.runeTemplateManaCost();
        IManaPool pool = findPool(world, new Pos(
                net.minecraft.util.MathHelper.floor_double(player.posX),
                net.minecraft.util.MathHelper.floor_double(player.posY),
                net.minecraft.util.MathHelper.floor_double(player.posZ)), cost);
        if (pool == null) {
            player.addChatMessage(new ChatComponentTranslation(
                    "message.infwhat_botania.rune_template.no_pool"));
            return;
        }

        ItemStack stone = player.inventory.mainInventory[livingrockSlot];
        stone.stackSize--;
        if (stone.stackSize <= 0) {
            player.inventory.mainInventory[livingrockSlot] = null;
        }
        player.inventory.markDirty();
        pool.recieveMana(-cost);

        ItemStack result = new ItemStack(vazkii.botania.common.item.ModItems.rune,
                1, metadata);
        if (!player.inventory.addItemStackToInventory(result)) {
            player.dropPlayerItemWithRandomChoice(result, false);
        }
        world.playSoundAtEntity(player, "random.orb", 0.8F, 1.0F);
        player.addChatMessage(new ChatComponentTranslation(
                "message.infwhat_botania.rune_template.crafted", result.getDisplayName()));
    }

    private static int boundRune(ItemStack template) {
        NBTTagCompound tag = template.getTagCompound();
        return tag == null || !tag.hasKey(TAG_RUNE) ? -1 : tag.getInteger(TAG_RUNE);
    }

    private static int findRuneSlot(EntityPlayer player) {
        for (int slot = 0; slot < player.inventory.mainInventory.length; slot++) {
            ItemStack stack = player.inventory.mainInventory[slot];
            if (stack != null && stack.getItem() == vazkii.botania.common.item.ModItems.rune
                    && stack.getMetadata() >= 0 && stack.getMetadata() < RUNE_TYPES) {
                return slot;
            }
        }
        return -1;
    }

    private static int findLivingrockSlot(EntityPlayer player) {
        Item livingrock = Item.getItemFromBlock(vazkii.botania.common.block.ModBlocks.livingrock);
        if (livingrock == null) {
            return -1;
        }
        for (int slot = 0; slot < player.inventory.mainInventory.length; slot++) {
            ItemStack stack = player.inventory.mainInventory[slot];
            if (stack != null && stack.getItem() == livingrock && stack.getMetadata() == 0) {
                return slot;
            }
        }
        return -1;
    }

    private static IManaPool findPool(World world, Pos center, int cost) {
        for (int dx = -SEARCH_RADIUS; dx <= SEARCH_RADIUS; dx++) {
            for (int dy = -SEARCH_RADIUS; dy <= SEARCH_RADIUS; dy++) {
                for (int dz = -SEARCH_RADIUS; dz <= SEARCH_RADIUS; dz++) {
                    Pos pos = new Pos(center.x + dx, center.y + dy, center.z + dz);
                    if (!world.blockExists(pos.x, pos.y, pos.z)) {
                        continue;
                    }
                    IManaReceiver receiver = ManaReceiverHelper.find(world, pos);
                    IManaPool pool = receiver instanceof IManaPool ? (IManaPool) receiver : null;
                    if (pool != null && pool.getCurrentMana() >= cost) {
                        return pool;
                    }
                }
            }
        }
        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void addInformation(ItemStack stack, EntityPlayer player, java.util.List tooltip,
                               boolean advanced) {
        super.addInformation(stack, player, tooltip, advanced);
        if (!BotaniaAdditionConfig.runeTemplateEnabled()) {
            tooltip.add(StatCollector.translateToLocal(
                    "tooltip.infwhat_botania.rune_template.disabled"));
        }
        int metadata = boundRune(stack);
        if (metadata < 0 || metadata >= RUNE_TYPES) {
            tooltip.add(StatCollector.translateToLocal(
                    "tooltip.infwhat_botania.rune_template.empty"));
        } else {
            tooltip.add(StatCollector.translateToLocalFormatted(
                    "tooltip.infwhat_botania.rune_template.bound",
                    new ItemStack(vazkii.botania.common.item.ModItems.rune, 1, metadata)
                            .getDisplayName()));
        }
        tooltip.add(StatCollector.translateToLocalFormatted(
                "tooltip.infwhat_botania.rune_template.usage",
                BotaniaAdditionConfig.runeTemplateManaCost()));
    }
}

