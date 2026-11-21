package com.bandybus.infwhat.botania.compat.jade;

import com.bandybus.infwhat.botania.InfinityWhatBotania;
import com.bandybus.infwhat.botania.block.FloralOverclockBlock;
import com.bandybus.infwhat.botania.block.GaiaBeaconBlock;
import com.bandybus.infwhat.botania.block.ManaAnchorBlock;
import com.bandybus.infwhat.botania.block.ManaBeaconBlock;
import com.bandybus.infwhat.botania.block.ManaWellBlock;
import com.bandybus.infwhat.botania.block.RuneAcceleratorBlock;
import com.bandybus.infwhat.botania.block.TerrasteelAcceleratorBlock;
import com.bandybus.infwhat.botania.block.entity.AbstractAcceleratorBlockEntity;
import com.bandybus.infwhat.botania.block.entity.AbstractManaEndpointBlockEntity;
import com.bandybus.infwhat.botania.block.entity.FloralOverclockBlockEntity;
import com.bandybus.infwhat.botania.block.entity.GaiaBeaconBlockEntity;
import com.bandybus.infwhat.botania.block.entity.ManaAnchorBlockEntity;
import com.bandybus.infwhat.botania.block.entity.ManaBeaconBlockEntity;
import com.bandybus.infwhat.botania.block.entity.ManaWellBlockEntity;
import com.bandybus.infwhat.botania.block.entity.RuneAcceleratorBlockEntity;
import com.bandybus.infwhat.botania.block.entity.TerrasteelAcceleratorBlockEntity;
import com.bandybus.infwhat.botania.config.BotaniaAdditionConfig;
import mcp.mobius.waila.api.BlockAccessor;
import mcp.mobius.waila.api.IComponentProvider;
import mcp.mobius.waila.api.IServerDataProvider;
import mcp.mobius.waila.api.ITooltip;
import mcp.mobius.waila.api.IWailaClientRegistration;
import mcp.mobius.waila.api.IWailaCommonRegistration;
import mcp.mobius.waila.api.IWailaPlugin;
import mcp.mobius.waila.api.TooltipPosition;
import mcp.mobius.waila.api.WailaPlugin;
import mcp.mobius.waila.api.config.IPluginConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Jade HUD support for the add-on machines.
 *
 * <p>The server side packs the few values that are not synced to the client block entity anyway;
 * the client side renders them with the regular Jade tooltip. Jade is an optional dependency, so
 * this class is only ever loaded by Jade's own plugin scanner.
 */
@WailaPlugin(InfinityWhatBotania.MOD_ID)
public class JadeIntegration implements IWailaPlugin, IComponentProvider,
        IServerDataProvider<BlockEntity> {

    private static final String KIND = "InfwhatKind";
    private static final String ENABLED = "InfwhatEnabled";
    private static final String FEATURE = "InfwhatFeature";
    private static final String RATE = "InfwhatRate";
    private static final String COOLDOWN = "InfwhatCooldown";
    private static final String MANA = "InfwhatMana";
    private static final String CAPACITY = "InfwhatCapacity";

    private static final String KIND_WELL = "well";
    private static final String KIND_BEACON = "beacon";
    private static final String KIND_ANCHOR = "anchor";
    private static final String KIND_RUNE = "rune";
    private static final String KIND_TERRASTEEL = "terrasteel";
    private static final String KIND_FLORAL = "floral";
    private static final String KIND_GAIA = "gaia";

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(this, ManaWellBlockEntity.class);
        registration.registerBlockDataProvider(this, ManaBeaconBlockEntity.class);
        registration.registerBlockDataProvider(this, ManaAnchorBlockEntity.class);
        registration.registerBlockDataProvider(this, RuneAcceleratorBlockEntity.class);
        registration.registerBlockDataProvider(this, TerrasteelAcceleratorBlockEntity.class);
        registration.registerBlockDataProvider(this, FloralOverclockBlockEntity.class);
        registration.registerBlockDataProvider(this, GaiaBeaconBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerComponentProvider(this, TooltipPosition.BODY, ManaWellBlock.class);
        registration.registerComponentProvider(this, TooltipPosition.BODY, ManaBeaconBlock.class);
        registration.registerComponentProvider(this, TooltipPosition.BODY, ManaAnchorBlock.class);
        registration.registerComponentProvider(this, TooltipPosition.BODY,
                RuneAcceleratorBlock.class);
        registration.registerComponentProvider(this, TooltipPosition.BODY,
                TerrasteelAcceleratorBlock.class);
        registration.registerComponentProvider(this, TooltipPosition.BODY,
                FloralOverclockBlock.class);
        registration.registerComponentProvider(this, TooltipPosition.BODY, GaiaBeaconBlock.class);
    }

    @Override
    public void appendServerData(CompoundTag tag, ServerPlayer player, Level level,
                                 BlockEntity blockEntity, boolean remote) {
        if (blockEntity instanceof ManaWellBlockEntity) {
            ManaWellBlockEntity well = (ManaWellBlockEntity) blockEntity;
            tag.putString(KIND, KIND_WELL);
            tag.putBoolean(ENABLED, well.isMasterEnabled());
            tag.putInt(RATE, well.effectiveRate());
            return;
        }
        if (blockEntity instanceof ManaBeaconBlockEntity) {
            tag.putString(KIND, KIND_BEACON);
            tag.putBoolean(ENABLED,
                    ((AbstractManaEndpointBlockEntity) blockEntity).isEnabled());
            return;
        }
        if (blockEntity instanceof ManaAnchorBlockEntity) {
            tag.putString(KIND, KIND_ANCHOR);
            tag.putBoolean(ENABLED,
                    ((AbstractManaEndpointBlockEntity) blockEntity).isEnabled());
            return;
        }
        if (blockEntity instanceof RuneAcceleratorBlockEntity) {
            appendAccelerator(tag, (RuneAcceleratorBlockEntity) blockEntity, KIND_RUNE);
            return;
        }
        if (blockEntity instanceof TerrasteelAcceleratorBlockEntity) {
            appendAccelerator(tag, (TerrasteelAcceleratorBlockEntity) blockEntity,
                    KIND_TERRASTEEL);
            return;
        }
        if (blockEntity instanceof FloralOverclockBlockEntity) {
            appendAccelerator(tag, (FloralOverclockBlockEntity) blockEntity, KIND_FLORAL);
            return;
        }
        if (blockEntity instanceof GaiaBeaconBlockEntity) {
            GaiaBeaconBlockEntity beacon = (GaiaBeaconBlockEntity) blockEntity;
            tag.putString(KIND, KIND_GAIA);
            tag.putInt(MANA, beacon.getCurrentMana());
            tag.putInt(CAPACITY, BotaniaAdditionConfig.gaiaBeaconCapacity());
        }
    }

    private static void appendAccelerator(CompoundTag tag,
                                          AbstractAcceleratorBlockEntity accelerator,
                                          String kind) {
        tag.putString(KIND, kind);
        tag.putBoolean(ENABLED, accelerator.isEnabled());
        tag.putBoolean(FEATURE, accelerator.isFeatureEnabled());
        tag.putInt(RATE, accelerator.effectiveRate());
        tag.putInt(COOLDOWN, accelerator.effectiveCooldownMultiplier());
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();
        String kind = data.getString(KIND);
        if (kind.isEmpty()) {
            return;
        }
        if (KIND_GAIA.equals(kind)) {
            tooltip.add(new TranslatableComponent("jade.infwhat_botania.gaia.mana",
                    Integer.valueOf(data.getInt(MANA)),
                    Integer.valueOf(data.getInt(CAPACITY))));
            return;
        }
        tooltip.add(stateLine(data.getBoolean(ENABLED)));
        if (data.contains(FEATURE) && !data.getBoolean(FEATURE)) {
            tooltip.add(new TranslatableComponent("jade.infwhat_botania.feature_disabled"));
            return;
        }
        if (KIND_WELL.equals(kind)) {
            tooltip.add(new TranslatableComponent("jade.infwhat_botania.well.rate",
                    Integer.valueOf(data.getInt(RATE))));
        } else if (KIND_RUNE.equals(kind) || KIND_TERRASTEEL.equals(kind)) {
            tooltip.add(new TranslatableComponent("jade.infwhat_botania.accelerator.rate",
                    Integer.valueOf(data.getInt(RATE))));
            tooltip.add(new TranslatableComponent("jade.infwhat_botania.accelerator.cooldown",
                    Integer.valueOf(data.getInt(COOLDOWN))));
        } else if (KIND_FLORAL.equals(kind)) {
            tooltip.add(new TranslatableComponent(
                    "jade.infwhat_botania.accelerator.multiplier",
                    Integer.valueOf(data.getInt(RATE))));
            tooltip.add(new TranslatableComponent("jade.infwhat_botania.accelerator.cooldown",
                    Integer.valueOf(data.getInt(COOLDOWN))));
        }
    }

    private static Component stateLine(boolean enabled) {
        return new TranslatableComponent(enabled
                ? "jade.infwhat_botania.enabled" : "jade.infwhat_botania.disabled");
    }
}
