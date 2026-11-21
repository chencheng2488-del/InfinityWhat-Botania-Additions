package com.bandybus.infwhat.botania.compat.waila;

import com.bandybus.infwhat.botania.block.BlockAccelerator;
import com.bandybus.infwhat.botania.block.BlockGaiaBeacon;
import com.bandybus.infwhat.botania.block.BlockManaDevice;
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
import mcp.mobius.waila.api.IWailaConfigHandler;
import mcp.mobius.waila.api.IWailaDataAccessor;
import mcp.mobius.waila.api.IWailaDataProvider;
import mcp.mobius.waila.api.IWailaRegistrar;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

import java.util.List;

/**
 * Waila HUD support for the 1.7.10 release (the Jade equivalent for this Minecraft version).
 *
 * <p>Waila ships the packed NBT to the client, where the body provider renders the values. This
 * class is only loaded through Waila's IMC callback, so a missing Waila cannot break startup.
 */
public class WailaIntegration implements IWailaDataProvider {

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

    /** Entry point referenced by the "register" IMC message in the mod pre-init. */
    public static void register(IWailaRegistrar registrar) {
        WailaIntegration provider = new WailaIntegration();
        registrar.registerBodyProvider(provider, BlockManaDevice.class);
        registrar.registerBodyProvider(provider, BlockAccelerator.class);
        registrar.registerBodyProvider(provider, BlockGaiaBeacon.class);

        registrar.registerNBTProvider(provider, ManaWellTileEntity.class);
        registrar.registerNBTProvider(provider, ManaBeaconTileEntity.class);
        registrar.registerNBTProvider(provider, ManaAnchorTileEntity.class);
        registrar.registerNBTProvider(provider, RuneAcceleratorTileEntity.class);
        registrar.registerNBTProvider(provider, TerrasteelAcceleratorTileEntity.class);
        registrar.registerNBTProvider(provider, FloralOverclockTileEntity.class);
        registrar.registerNBTProvider(provider, GaiaBeaconTileEntity.class);
    }

    @Override
    public ItemStack getWailaStack(IWailaDataAccessor accessor, IWailaConfigHandler config) {
        return null;
    }

    @Override
    public List<String> getWailaHead(ItemStack itemStack, List<String> currenttip,
                                     IWailaDataAccessor accessor, IWailaConfigHandler config) {
        return currenttip;
    }

    @Override
    public List<String> getWailaBody(ItemStack itemStack, List<String> currenttip,
                                     IWailaDataAccessor accessor, IWailaConfigHandler config) {
        NBTTagCompound data = accessor.getNBTData();
        String kind = data.getString(KIND);
        if (kind.isEmpty()) {
            return currenttip;
        }
        if (KIND_GAIA.equals(kind)) {
            currenttip.add(StatCollector.translateToLocalFormatted(
                    "waila.infwhat_botania.gaia.mana",
                    data.getInteger(MANA), data.getInteger(CAPACITY)));
            return currenttip;
        }
        currenttip.add(StatCollector.translateToLocal(
                data.getBoolean(ENABLED)
                        ? "waila.infwhat_botania.enabled"
                        : "waila.infwhat_botania.disabled"));
        if (data.hasKey(FEATURE) && !data.getBoolean(FEATURE)) {
            currenttip.add(StatCollector.translateToLocal(
                    "waila.infwhat_botania.feature_disabled"));
            return currenttip;
        }
        if (KIND_WELL.equals(kind)) {
            currenttip.add(StatCollector.translateToLocalFormatted(
                    "waila.infwhat_botania.well.rate", data.getInteger(RATE)));
        } else if (KIND_RUNE.equals(kind) || KIND_TERRASTEEL.equals(kind)) {
            currenttip.add(StatCollector.translateToLocalFormatted(
                    "waila.infwhat_botania.accelerator.rate", data.getInteger(RATE)));
            currenttip.add(StatCollector.translateToLocalFormatted(
                    "waila.infwhat_botania.accelerator.cooldown", data.getInteger(COOLDOWN)));
        } else if (KIND_FLORAL.equals(kind)) {
            currenttip.add(StatCollector.translateToLocalFormatted(
                    "waila.infwhat_botania.accelerator.multiplier", data.getInteger(RATE)));
            currenttip.add(StatCollector.translateToLocalFormatted(
                    "waila.infwhat_botania.accelerator.cooldown", data.getInteger(COOLDOWN)));
        }
        return currenttip;
    }

    @Override
    public List<String> getWailaTail(ItemStack itemStack, List<String> currenttip,
                                     IWailaDataAccessor accessor, IWailaConfigHandler config) {
        return currenttip;
    }

    @Override
    public NBTTagCompound getNBTData(EntityPlayerMP player, TileEntity tileEntity,
                                     NBTTagCompound tag, World world, int x, int y, int z) {
        if (tileEntity instanceof ManaWellTileEntity) {
            ManaWellTileEntity well = (ManaWellTileEntity) tileEntity;
            tag.setString(KIND, KIND_WELL);
            tag.setBoolean(ENABLED, well.isMasterEnabled());
            tag.setInteger(RATE, well.effectiveRate());
            return tag;
        }
        if (tileEntity instanceof ManaBeaconTileEntity) {
            tag.setString(KIND, KIND_BEACON);
            tag.setBoolean(ENABLED, ((AbstractManaEndpointTileEntity) tileEntity).isEnabled());
            return tag;
        }
        if (tileEntity instanceof ManaAnchorTileEntity) {
            tag.setString(KIND, KIND_ANCHOR);
            tag.setBoolean(ENABLED, ((AbstractManaEndpointTileEntity) tileEntity).isEnabled());
            return tag;
        }
        if (tileEntity instanceof RuneAcceleratorTileEntity) {
            appendAccelerator(tag, (RuneAcceleratorTileEntity) tileEntity, KIND_RUNE);
            return tag;
        }
        if (tileEntity instanceof TerrasteelAcceleratorTileEntity) {
            appendAccelerator(tag, (TerrasteelAcceleratorTileEntity) tileEntity,
                    KIND_TERRASTEEL);
            return tag;
        }
        if (tileEntity instanceof FloralOverclockTileEntity) {
            appendAccelerator(tag, (FloralOverclockTileEntity) tileEntity, KIND_FLORAL);
            return tag;
        }
        if (tileEntity instanceof GaiaBeaconTileEntity) {
            GaiaBeaconTileEntity beacon = (GaiaBeaconTileEntity) tileEntity;
            tag.setString(KIND, KIND_GAIA);
            tag.setInteger(MANA, beacon.getCurrentMana());
            tag.setInteger(CAPACITY, BotaniaAdditionConfig.gaiaBeaconCapacity());
        }
        return tag;
    }

    private static void appendAccelerator(NBTTagCompound tag,
                                          AbstractAcceleratorTileEntity accelerator,
                                          String kind) {
        tag.setString(KIND, kind);
        tag.setBoolean(ENABLED, accelerator.isEnabled());
        tag.setBoolean(FEATURE, accelerator.isFeatureEnabled());
        tag.setInteger(RATE, accelerator.effectiveRate());
        tag.setInteger(COOLDOWN, accelerator.effectiveCooldownMultiplier());
    }
}
