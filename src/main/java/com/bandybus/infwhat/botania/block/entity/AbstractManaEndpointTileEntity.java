package com.bandybus.infwhat.botania.block.entity;

import com.bandybus.infwhat.botania.config.BotaniaAdditionConfig;
import com.bandybus.infwhat.botania.device.ManaDeviceHost;
import com.bandybus.infwhat.botania.device.ManaDeviceSettings;
import com.bandybus.infwhat.botania.device.ManaNodeKind;
import com.bandybus.infwhat.botania.grid.ManaNetworkData;
import com.bandybus.infwhat.botania.util.Pos;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Shared state for the mana beacon and the mana anchor.
 *
 * <p>There is no owner-visible channel name. The owner UUID only decides who may open the GUI;
 * the network itself is derived from explicit links plus the automatic scan.
 */
public abstract class AbstractManaEndpointTileEntity extends TileEntity implements ManaDeviceHost {

    private static final String TAG_OWNER = "Owner";
    private static final String TAG_ENABLED = "Enabled";
    private static final String TAG_AUTO_TARGETS = "AutoTargets";

    private UUID owner;
    private boolean enabled = true;
    private final ManaDeviceSettings settings = new ManaDeviceSettings();
    private final List<Pos> clientLinks = new ArrayList<Pos>();
    private final List<Pos> autoTargets = new ArrayList<Pos>();
    private boolean nodeRegistered;

    /** Which network node kind this endpoint registers as. */
    protected abstract ManaNodeKind nodeKind();

    public UUID getOwnerId() {
        return owner;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setOwner(UUID value) {
        if (owner == null && value != null) {
            owner = value;
            markDirty();
        }
    }

    public boolean isOwnedBy(UUID playerId) {
        return owner != null && owner.equals(playerId);
    }

    public boolean toggleEnabled() {
        enabled = !enabled;
        markUpdated();
        return enabled;
    }

    @Override
    public ManaDeviceSettings settings() {
        return settings;
    }

    @Override
    public boolean isMasterEnabled() {
        return enabled;
    }

    @Override
    public void setMasterEnabled(boolean value) {
        if (enabled != value) {
            enabled = value;
            markUpdated();
        }
    }

    @Override
    public List<Pos> clientLinks() {
        return clientLinks;
    }

    @Override
    public List<Pos> clientAutoTargets() {
        return autoTargets;
    }

    /** Server-side scan cache of nearby storage; also mirrored to the client through NBT. */
    protected List<Pos> autoTargets() {
        return autoTargets;
    }

    /** Runtime gate for a wireless endpoint. */
    public boolean canOperate(World world) {
        return owner != null
                && BotaniaAdditionConfig.wirelessEnabled()
                && BotaniaAdditionConfig.canOperate(world);
    }

    @Override
    public int reachX() {
        return 0;
    }

    @Override
    public int reachY() {
        return 0;
    }

    @Override
    public int reachZ() {
        return 0;
    }

    @Override
    public int offsetX() {
        return 0;
    }

    @Override
    public int offsetY() {
        return 0;
    }

    @Override
    public int offsetZ() {
        return 0;
    }

    @Override
    public boolean showRange() {
        return false;
    }

    /** Registers this block in the world network exactly once. */
    protected void ensureRegistered() {
        if (nodeRegistered || worldObj == null || worldObj.isRemote) {
            return;
        }
        ManaNetworkData data = ManaNetworkData.get(worldObj);
        if (data != null) {
            data.register(worldObj, Pos.of(this), nodeKind());
            nodeRegistered = true;
        }
    }

    @Override
    public void updateEntity() {
        if (!worldObj.isRemote) {
            ensureRegistered();
        }
    }

    /** Marks the tile dirty and forces a description packet so the client renderer updates. */
    protected void markUpdated() {
        markDirty();
        if (worldObj != null && !worldObj.isRemote) {
            worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        saveCommon(tag);
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        loadCommon(tag);
    }

    protected void saveCommon(NBTTagCompound tag) {
        tag.setBoolean(TAG_ENABLED, enabled);
        settings.save(tag);
        if (owner != null) {
            tag.setString(TAG_OWNER, owner.toString());
        }
        if (worldObj != null && !worldObj.isRemote) {
            ManaNetworkData.writeLinks(tag, worldObj, Pos.of(this));
        }
        ManaNetworkData.writePositions(tag, TAG_AUTO_TARGETS, autoTargets);
    }

    protected void loadCommon(NBTTagCompound tag) {
        enabled = !tag.hasKey(TAG_ENABLED) || tag.getBoolean(TAG_ENABLED);
        settings.load(tag);
        owner = null;
        if (tag.hasKey(TAG_OWNER)) {
            try {
                owner = UUID.fromString(tag.getString(TAG_OWNER));
            } catch (IllegalArgumentException ignored) {
                owner = null;
            }
        }
        clientLinks.clear();
        clientLinks.addAll(ManaNetworkData.readLinks(tag));
        autoTargets.clear();
        autoTargets.addAll(ManaNetworkData.readPositions(tag, TAG_AUTO_TARGETS));
    }

    @Override
    public Packet getDescriptionPacket() {
        NBTTagCompound tag = new NBTTagCompound();
        saveCommon(tag);
        return new S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, 1, tag);
    }

    @Override
    public void onDataPacket(NetworkManager network, S35PacketUpdateTileEntity packet) {
        loadCommon(packet.getNbtCompound());
    }
}
