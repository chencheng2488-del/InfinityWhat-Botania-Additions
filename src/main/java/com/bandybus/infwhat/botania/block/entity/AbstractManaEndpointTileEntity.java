package com.bandybus.infwhat.botania.block.entity;

import com.bandybus.infwhat.botania.config.BotaniaAdditionConfig;
import com.bandybus.infwhat.botania.device.ManaDeviceHost;
import com.bandybus.infwhat.botania.device.ManaDeviceSettings;
import com.bandybus.infwhat.botania.grid.ManaNetworkData;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SUpdateTileEntityPacket;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.server.ServerWorld;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Shared state for the two wireless endpoints.
 *
 * <p>There is no owner-visible channel name anymore. The owner UUID is only used to decide who may
 * open the GUI; the network itself is derived from explicit links.
 */
public abstract class AbstractManaEndpointTileEntity extends TileEntity implements ManaDeviceHost {

    private static final String TAG_OWNER = "Owner";
    private static final String TAG_ENABLED = "Enabled";
    private static final String TAG_AUTO_TARGETS = "AutoTargets";

    @Nullable
    private UUID owner;
    private boolean enabled = true;
    private final ManaDeviceSettings settings = new ManaDeviceSettings();
    private final List<BlockPos> clientLinks = new ArrayList<BlockPos>();
    private final List<BlockPos> autoTargets = new ArrayList<BlockPos>();

    protected AbstractManaEndpointTileEntity(TileEntityType<?> type) {
        super(type);
    }

    @Nullable
    public UUID getOwnerId() {
        return owner;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setOwner(@Nullable UUID owner) {
        if (this.owner == null && owner != null) {
            this.owner = owner;
            setChanged();
        }
    }

    public boolean isOwnedBy(UUID playerId) {
        return owner != null && owner.equals(playerId);
    }

    public boolean toggleEnabled() {
        enabled = !enabled;
        setChanged();
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
            setChanged();
        }
    }

    @Override
    public List<BlockPos> clientLinks() {
        return clientLinks;
    }

    @Override
    public List<BlockPos> clientAutoTargets() {
        return autoTargets;
    }

    /** Server-side scan cache of nearby mana pools; also mirrored to the client through NBT. */
    protected List<BlockPos> autoTargets() {
        return autoTargets;
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

    /** Full runtime gate for a wireless endpoint. */
    public boolean canOperate(ServerWorld level) {
        return owner != null
                && BotaniaAdditionConfig.wirelessEnabled()
                && BotaniaAdditionConfig.canOperate(level);
    }

    @Override
    public void load(net.minecraft.block.BlockState state, CompoundNBT tag) {
        super.load(state, tag);
        enabled = !tag.contains(TAG_ENABLED) || tag.getBoolean(TAG_ENABLED);
        settings.load(tag);
        owner = tag.hasUUID(TAG_OWNER) ? tag.getUUID(TAG_OWNER) : null;
        clientLinks.clear();
        clientLinks.addAll(ManaNetworkData.readLinks(tag));
        autoTargets.clear();
        autoTargets.addAll(ManaNetworkData.readPositions(tag, TAG_AUTO_TARGETS));
    }

    @Override
    public CompoundNBT save(CompoundNBT tag) {
        super.save(tag);
        tag.putBoolean(TAG_ENABLED, enabled);
        settings.save(tag);
        if (owner != null) {
            tag.putUUID(TAG_OWNER, owner);
        }
        if (level instanceof ServerWorld) {
            ManaNetworkData.writeLinks(tag, (ServerWorld) level, worldPosition);
        }
        ManaNetworkData.writePositions(tag, TAG_AUTO_TARGETS, autoTargets);
        return tag;
    }

    @Override
    public CompoundNBT getUpdateTag() {
        return save(new CompoundNBT());
    }

    @Override
    public void handleUpdateTag(net.minecraft.block.BlockState state, CompoundNBT tag) {
        load(state, tag);
    }

    @Override
    public SUpdateTileEntityPacket getUpdatePacket() {
        return new SUpdateTileEntityPacket(worldPosition, 3, getUpdateTag());
    }

    @Override
    public void onDataPacket(NetworkManager net, SUpdateTileEntityPacket packet) {
        load(getBlockState(), packet.getTag());
    }
}
