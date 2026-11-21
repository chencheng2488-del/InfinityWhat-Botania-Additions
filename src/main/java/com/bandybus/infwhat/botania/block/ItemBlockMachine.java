package com.bandybus.infwhat.botania.block;

import com.bandybus.infwhat.botania.device.AcceleratorKind;
import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.block.Block;
import net.minecraft.item.ItemBlock;

/** Block items used by every add-on block; they carry the tooltip from the block lang key. */
public class ItemBlockMachine extends ItemBlock {

    public ItemBlockMachine(Block block) {
        super(block);
        this.setMaxStackSize(1);
        this.setHasSubtypes(false);
    }

    @Override
    public int getMetadata(int damage) {
        return damage & 3;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void addInformation(net.minecraft.item.ItemStack stack,
                               net.minecraft.entity.player.EntityPlayer player,
                               java.util.List tooltip, boolean advanced) {
        super.addInformation(stack, player, tooltip, advanced);
        String key = getUnlocalizedName(stack) + ".tooltip";
        String text = net.minecraft.util.StatCollector.translateToLocal(key);
        if (text != null && !text.equals(key)) {
            tooltip.addAll(java.util.Arrays.asList(text.split("\n")));
        }
    }

    /** Convenience factory used by {@link ModBlocks}. */
    public static ItemBlock create(Block block) {
        return new ItemBlockMachine(block);
    }

    /** Kept so the accelerator kind stays part of the public block API surface. */
    public static AcceleratorKind kindOf(Block block) {
        return block instanceof BlockAccelerator ? ((BlockAccelerator) block).kind() : null;
    }
}
