package com.bandybus.infwhat.botania.item;

import com.bandybus.infwhat.botania.config.BotaniaAdditionConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Block item for the Gaia Beacon with a config-aware tooltip. */
public class GaiaBeaconBlockItem extends BlockItem {

    public GaiaBeaconBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip,
                                TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        if (!BotaniaAdditionConfig.gaiaBeaconEnabled()) {
            tooltip.add(new net.minecraft.network.chat.TranslatableComponent("tooltip.infwhat_botania.gaia_beacon.disabled")
                    .withStyle(ChatFormatting.RED));
        }
        tooltip.add(new net.minecraft.network.chat.TranslatableComponent("tooltip.infwhat_botania.gaia_beacon.usage",
                BotaniaAdditionConfig.gaiaBeaconManaCost()).withStyle(ChatFormatting.GRAY));
    }
}
