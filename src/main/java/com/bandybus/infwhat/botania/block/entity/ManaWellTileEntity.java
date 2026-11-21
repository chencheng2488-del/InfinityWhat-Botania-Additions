package com.bandybus.infwhat.botania.block.entity;

import com.bandybus.infwhat.botania.config.BotaniaAdditionConfig;
import com.bandybus.infwhat.botania.device.DeviceRanges;
import com.bandybus.infwhat.botania.device.DeviceState;
import com.bandybus.infwhat.botania.device.LinkEntry;
import com.bandybus.infwhat.botania.device.ManaDeviceHost;
import com.bandybus.infwhat.botania.device.ManaDeviceSettings;
import com.bandybus.infwhat.botania.device.ManaNodeKind;
import com.bandybus.infwhat.botania.grid.ManaNetworkData;
import com.bandybus.infwhat.botania.util.ManaAutoTargets;
import com.bandybus.infwhat.botania.util.ManaReceiverHelper;
import com.bandybus.infwhat.botania.util.Pos;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import vazkii.botania.api.mana.IManaReceiver;

import java.util.ArrayList;
import java.util.List;

/**
 * Finite, configurable mana source.
 *
 * <p>The well pushes its per-tick budget towards explicitly linked receivers and towards mana
 * pools and working infusion devices inside its scan box. The scan box is the same box drawn by
 * the client when "Show range" is on.
 */
public class ManaWellTileEntity extends TileEntity implements ManaDeviceHost {

    private static final String TAG_ENABLED = "Enabled";
    private static final String TAG_AUTO_TARGETS = "AutoTargets";
    private static final int SCAN_INTERVAL = 20;

    private final ManaDeviceSettings settings = new ManaDeviceSettings();
    private final List<Pos> clientLinks = new ArrayList<Pos>();
    private final List<Pos> autoTargets = new ArrayList<Pos>();
    private boolean enabled = true;
    private int scanCooldown;
    private boolean nodeRegistered;

    @Override
    public void updateEntity() {
        if (worldObj == null || worldObj.isRemote) {
            return;
        }
        ensureRegistered();
        if (!BotaniaAdditionConfig.manaWellEnabled()
                || !BotaniaAdditionConfig.canOperate(worldObj)) {
            return;
        }
        ManaNetworkData network = ManaNetworkData.get(worldObj);
        if (network == null) {
            return;
        }
        Pos self = Pos.of(this);
        network.validateLinks(worldObj, self);
        if (scanCooldown-- <= 0) {
            scan(network);
            scanCooldown = SCAN_INTERVAL;
        }
        if (!enabled) {
            return;
        }
        pushMana(network);
    }

    private void ensureRegistered() {
        if (nodeRegistered) {
            return;
        }
        ManaNetworkData data = ManaNetworkData.get(worldObj);
        if (data != null) {
            data.register(worldObj, Pos.of(this), ManaNodeKind.WELL);
            nodeRegistered = true;
        }
    }

    private void scan(ManaNetworkData network) {
        Pos self = Pos.of(this);
        List<Pos> candidates = new ArrayList<Pos>();
        for (Pos candidate : DeviceRanges.box(self,
                settings.reachX(BotaniaAdditionConfig.radius()),
                settings.reachY(BotaniaAdditionConfig.radius()),
                settings.reachZ(BotaniaAdditionConfig.radius()),
                settings.offsetX(), settings.offsetY(), settings.offsetZ())) {
            if (candidate.equals(self) || !worldObj.blockExists(candidate.x, candidate.y, candidate.z)
                    || ManaReceiverHelper.isCreativePool(worldObj, candidate)
                    || !settings.isTargetEnabled(candidate)) {
                continue;
            }
            IManaReceiver receiver = ManaReceiverHelper.find(worldObj, candidate);
            TileEntity tileEntity = worldObj.getTileEntity(candidate.x, candidate.y, candidate.z);
            if (receiver != null && ManaAutoTargets.isTarget(tileEntity)) {
                candidates.add(candidate);
            }
        }

        for (Pos existing : new ArrayList<Pos>(autoTargets)) {
            if (!candidates.contains(existing)) {
                network.autoDisconnect(worldObj, self, existing);
            }
        }

        List<Pos> linked = new ArrayList<Pos>();
        for (Pos candidate : candidates) {
            if (network.hasDirectEdge(worldObj, self, candidate)) {
                if (network.isAutoEdge(worldObj, self, candidate)) {
                    linked.add(candidate);
                }
                continue;
            }
            ManaNetworkData.ConnectResult result = network.autoConnect(worldObj, self, candidate);
            if ((result.result() == ManaNetworkData.Result.OK
                    || result.result() == ManaNetworkData.Result.ALREADY)
                    && network.isAutoEdge(worldObj, self, candidate)) {
                linked.add(candidate);
            }
        }

        if (!linked.equals(autoTargets)) {
            autoTargets.clear();
            autoTargets.addAll(linked);
            markUpdated();
        }
    }

    private void pushMana(ManaNetworkData network) {
        int budget = effectiveRate();
        if (budget <= 0) {
            return;
        }
        List<Pos> targets = network.wellOutputs(worldObj, Pos.of(this));
        if (targets.isEmpty()) {
            return;
        }
        int remaining = targets.size();
        for (Pos target : targets) {
            if (budget <= 0) {
                return;
            }
            int share = Math.max(1, budget / Math.max(1, remaining));
            remaining--;
            IManaReceiver receiver = ManaReceiverHelper.find(worldObj, target);
            if (receiver == null || receiver.isFull() || !receiver.canRecieveManaFromBursts()) {
                continue;
            }
            int space = ManaReceiverHelper.space(receiver);
            int amount = Math.min(Math.min(share, space), budget);
            if (amount > 0) {
                receiver.recieveMana(amount);
                budget -= amount;
            }
        }
    }

    public boolean toggleEnabled() {
        enabled = !enabled;
        markUpdated();
        return enabled;
    }

    /** Per-tick output budget, clamped to the configured ceiling. */
    public int effectiveRate() {
        int fallback = BotaniaAdditionConfig.manaPerTick();
        return Math.min(settings.effectiveRate(fallback), BotaniaAdditionConfig.maxManaPerTick());
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

    @Override
    public int reachX() {
        return settings.reachX(BotaniaAdditionConfig.radius());
    }

    @Override
    public int reachY() {
        return settings.reachY(BotaniaAdditionConfig.radius());
    }

    @Override
    public int reachZ() {
        return settings.reachZ(BotaniaAdditionConfig.radius());
    }

    @Override
    public int offsetX() {
        return settings.offsetX();
    }

    @Override
    public int offsetY() {
        return settings.offsetY();
    }

    @Override
    public int offsetZ() {
        return settings.offsetZ();
    }

    @Override
    public boolean showRange() {
        return settings.showRange();
    }

    @Override
    public DeviceState createState(World world) {
        ManaNetworkData network = ManaNetworkData.get(world);
        List<LinkEntry> links = network == null
                ? new ArrayList<LinkEntry>() : network.links(world, Pos.of(this));
        return new DeviceState(settings.rate(), effectiveRate(), enabled, 0L, 0L,
                network == null ? "" : network.networkName(world, Pos.of(this)),
                reachX(), reachY(), reachZ(), offsetX(), offsetY(), offsetZ(), showRange(), links);
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

    private void saveCommon(NBTTagCompound tag) {
        tag.setBoolean(TAG_ENABLED, enabled);
        settings.save(tag);
        if (worldObj != null && !worldObj.isRemote) {
            ManaNetworkData.writeLinks(tag, worldObj, Pos.of(this));
        }
        ManaNetworkData.writePositions(tag, TAG_AUTO_TARGETS, autoTargets);
    }

    private void loadCommon(NBTTagCompound tag) {
        enabled = !tag.hasKey(TAG_ENABLED) || tag.getBoolean(TAG_ENABLED);
        settings.load(tag);
        clientLinks.clear();
        clientLinks.addAll(ManaNetworkData.readLinks(tag));
        autoTargets.clear();
        autoTargets.addAll(ManaNetworkData.readPositions(tag, TAG_AUTO_TARGETS));
    }

    private void markUpdated() {
        markDirty();
        if (worldObj != null && !worldObj.isRemote) {
            worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
        }
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

    @Override
    public net.minecraft.util.AxisAlignedBB getRenderBoundingBox() {
        int radius = Math.max(1, Math.max(reachX(), reachZ()) + 1);
        return net.minecraft.util.AxisAlignedBB.getBoundingBox(xCoord, yCoord, zCoord,
                xCoord + 1, yCoord + 1, zCoord + 1).expand(radius, reachY() + 1, radius);
    }
}
