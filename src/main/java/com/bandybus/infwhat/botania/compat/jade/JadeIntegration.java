package com.bandybus.infwhat.botania.compat.jade;

import com.bandybus.infwhat.botania.InfinityWhatBotania;
import com.bandybus.infwhat.botania.block.FloralOverclockBlock;
import com.bandybus.infwhat.botania.block.GaiaBeaconBlock;
import com.bandybus.infwhat.botania.block.ManaAnchorBlock;
import com.bandybus.infwhat.botania.block.ManaBeaconBlock;
import com.bandybus.infwhat.botania.block.ManaWellBlock;
import com.bandybus.infwhat.botania.block.RuneAcceleratorBlock;
import com.bandybus.infwhat.botania.block.TerrasteelAcceleratorBlock;
import com.bandybus.infwhat.botania.block.entity.AbstractAcceleratorTileEntity;
import com.bandybus.infwhat.botania.block.entity.AbstractManaEndpointTileEntity;
import com.bandybus.infwhat.botania.block.entity.FloralOverclockTileEntity;
import com.bandybus.infwhat.botania.block.entity.GaiaBeaconTileEntity;
import com.bandybus.infwhat.botania.block.entity.ManaAnchorTileEntity;
import com.bandybus.infwhat.botania.block.entity.ManaBeaconTileEntity;
import com.bandybus.infwhat.botania.block.entity.ManaWellTileEntity;
import com.bandybus.infwhat.botania.block.entity.RuneAcceleratorTileEntity;
import com.bandybus.infwhat.botania.block.entity.TerrasteelAcceleratorTileEntity;
import com.bandybus.infwhat.botania.config.BotaniaAdditionConfig;
import mcp.mobius.waila.api.IComponentProvider;
import mcp.mobius.waila.api.IDataAccessor;
import mcp.mobius.waila.api.IPluginConfig;
import mcp.mobius.waila.api.IRegistrar;
import mcp.mobius.waila.api.IServerDataProvider;
import mcp.mobius.waila.api.IWailaPlugin;
import mcp.mobius.waila.api.TooltipPosition;
import mcp.mobius.waila.api.WailaPlugin;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;

import java.util.List;

/**
 * Jade HUD support for the add-on machines.
 *
 * <p>The server side packs the few values that are not synced to the client block entity anyway;
 * the client side renders them with the regular Jade tooltip. Jade is an optional dependency, so
 * this class is only ever loaded by Jade's own plugin scanner.
 */
@WailaPlugin(InfinityWhatBotania.MOD_ID)
public class JadeIntegration implements IWailaPlugin, IComponentProvider,
        IServerDataProvider<TileEntity> {

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
    public void register(IRegistrar registrar) {
        registrar.registerComponentProvider(this, TooltipPosition.BODY, ManaWellBlock.class);
        registrar.registerComponentProvider(this, TooltipPosition.BODY, ManaBeaconBlock.class);
        registrar.registerComponentProvider(this, TooltipPosition.BODY, ManaAnchorBlock.class);
        registrar.registerComponentProvider(this, TooltipPosition.BODY, RuneAcceleratorBlock.class);
        registrar.registerComponentProvider(this, TooltipPosition.BODY,
                TerrasteelAcceleratorBlock.class);
        registrar.registerComponentProvider(this, TooltipPosition.BODY, FloralOverclockBlock.class);
        registrar.registerComponentProvider(this, TooltipPosition.BODY, GaiaBeaconBlock.class);

        registrar.registerBlockDataProvider(this, ManaWellTileEntity.class);
        registrar.registerBlockDataProvider(this, ManaBeaconTileEntity.class);
        registrar.registerBlockDataProvider(this, ManaAnchorTileEntity.class);
        registrar.registerBlockDataProvider(this, RuneAcceleratorTileEntity.class);
        registrar.registerBlockDataProvider(this, TerrasteelAcceleratorTileEntity.class);
        registrar.registerBlockDataProvider(this, FloralOverclockTileEntity.class);
        registrar.registerBlockDataProvider(this, GaiaBeaconTileEntity.class);
    }

    @Override
    public void appendServerData(CompoundNBT tag, ServerPlayerEntity player, World world,
                                 TileEntity tileEntity) {
        if (tileEntity instanceof ManaWellTileEntity) {
            ManaWellTileEntity well = (ManaWellTileEntity) tileEntity;
            tag.putString(KIND, KIND_WELL);
            tag.putBoolean(ENABLED, well.isMasterEnabled());
            tag.putInt(RATE, well.effectiveRate());
            return;
        }
        if (tileEntity instanceof ManaBeaconTileEntity) {
            tag.putString(KIND, KIND_BEACON);
            tag.putBoolean(ENABLED, ((AbstractManaEndpointTileEntity) tileEntity).isEnabled());
            return;
        }
        if (tileEntity instanceof ManaAnchorTileEntity) {
            tag.putString(KIND, KIND_ANCHOR);
            tag.putBoolean(ENABLED, ((AbstractManaEndpointTileEntity) tileEntity).isEnabled());
            return;
        }
        if (tileEntity instanceof RuneAcceleratorTileEntity) {
            appendAccelerator(tag, (RuneAcceleratorTileEntity) tileEntity, KIND_RUNE);
            return;
        }
        if (tileEntity instanceof TerrasteelAcceleratorTileEntity) {
            appendAccelerator(tag, (TerrasteelAcceleratorTileEntity) tileEntity,
                    KIND_TERRASTEEL);
            return;
        }
        if (tileEntity instanceof FloralOverclockTileEntity) {
            appendAccelerator(tag, (FloralOverclockTileEntity) tileEntity, KIND_FLORAL);
            return;
        }
        if (tileEntity instanceof GaiaBeaconTileEntity) {
            GaiaBeaconTileEntity beacon = (GaiaBeaconTileEntity) tileEntity;
            tag.putString(KIND, KIND_GAIA);
            tag.putInt(MANA, beacon.getCurrentMana());
            tag.putInt(CAPACITY, BotaniaAdditionConfig.gaiaBeaconCapacity());
        }
    }

    private static void appendAccelerator(CompoundNBT tag, AbstractAcceleratorTileEntity accelerator,
                                          String kind) {
        tag.putString(KIND, kind);
        tag.putBoolean(ENABLED, accelerator.isEnabled());
        tag.putBoolean(FEATURE, accelerator.isFeatureEnabled());
        tag.putInt(RATE, accelerator.effectiveRate());
        tag.putInt(COOLDOWN, accelerator.effectiveCooldownMultiplier());
    }

    @Override
    public void appendBody(List<ITextComponent> tooltip, IDataAccessor accessor,
                           IPluginConfig config) {
        CompoundNBT data = accessor.getServerData();
        String kind = data.getString(KIND);
        if (kind.isEmpty()) {
            return;
        }
        if (KIND_GAIA.equals(kind)) {
            tooltip.add(new TranslationTextComponent("jade.infwhat_botania.gaia.mana",
                    Integer.valueOf(data.getInt(MANA)),
                    Integer.valueOf(data.getInt(CAPACITY))));
            return;
        }
        tooltip.add(stateLine(data.getBoolean(ENABLED)));
        if (data.contains(FEATURE) && !data.getBoolean(FEATURE)) {
            tooltip.add(new TranslationTextComponent("jade.infwhat_botania.feature_disabled"));
            return;
        }
        if (KIND_WELL.equals(kind)) {
            tooltip.add(new TranslationTextComponent("jade.infwhat_botania.well.rate",
                    Integer.valueOf(data.getInt(RATE))));
        } else if (KIND_RUNE.equals(kind) || KIND_TERRASTEEL.equals(kind)) {
            tooltip.add(new TranslationTextComponent("jade.infwhat_botania.accelerator.rate",
                    Integer.valueOf(data.getInt(RATE))));
            tooltip.add(new TranslationTextComponent("jade.infwhat_botania.accelerator.cooldown",
                    Integer.valueOf(data.getInt(COOLDOWN))));
        } else if (KIND_FLORAL.equals(kind)) {
            tooltip.add(new TranslationTextComponent(
                    "jade.infwhat_botania.accelerator.multiplier",
                    Integer.valueOf(data.getInt(RATE))));
            tooltip.add(new TranslationTextComponent("jade.infwhat_botania.accelerator.cooldown",
                    Integer.valueOf(data.getInt(COOLDOWN))));
        }
    }

    private static ITextComponent stateLine(boolean enabled) {
        return new TranslationTextComponent(enabled
                ? "jade.infwhat_botania.enabled" : "jade.infwhat_botania.disabled");
    }
}
