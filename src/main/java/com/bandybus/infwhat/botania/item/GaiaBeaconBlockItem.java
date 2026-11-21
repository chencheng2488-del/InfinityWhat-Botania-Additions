package com.bandybus.infwhat.botania.item;

import com.bandybus.infwhat.botania.config.BotaniaAdditionConfig;
import net.minecraft.block.Block;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;

/** Block item for the Gaia Beacon with a config-aware tooltip. */
public class GaiaBeaconBlockItem extends BlockItem {

    public GaiaBeaconBlockItem(Block block, Item.Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable World level, List<ITextComponent> tooltip,
                                ITooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        if (!BotaniaAdditionConfig.gaiaBeaconEnabled()) {
            tooltip.add(new TranslationTextComponent("tooltip.infwhat_botania.gaia_beacon.disabled")
                    .withStyle(TextFormatting.RED));
        }
        tooltip.add(new TranslationTextComponent("tooltip.infwhat_botania.gaia_beacon.usage",
                Integer.valueOf(BotaniaAdditionConfig.gaiaBeaconManaCost()))
                .withStyle(TextFormatting.GRAY));
    }
}
