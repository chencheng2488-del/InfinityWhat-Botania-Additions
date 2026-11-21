package com.bandybus.infwhat.botania.block.entity;

import com.bandybus.infwhat.botania.config.BotaniaAdditionConfig;
import com.bandybus.infwhat.botania.device.DeviceRanges;
import com.bandybus.infwhat.botania.device.DeviceState;
import com.bandybus.infwhat.botania.device.LinkEntry;
import com.bandybus.infwhat.botania.device.ManaNodeKind;
import com.bandybus.infwhat.botania.grid.ManaNetworkData;
import com.bandybus.infwhat.botania.util.ManaAutoTargets;
import com.bandybus.infwhat.botania.util.ManaReceiverHelper;
import com.bandybus.infwhat.botania.util.Pos;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import vazkii.botania.api.mana.IManaPool;
import vazkii.botania.api.mana.IManaReceiver;

import java.util.ArrayList;
import java.util.List;

/**
 * Withdraws mana from the network buffer and pushes it into linked storage blocks and into mana
 * pools inside its scan box.
 *
 * <p>The anchor first calculates how much room its neighbours have, then withdraws at most that
 * amount. Any mana a neighbour refuses is returned to the network, so a full pool cannot silently
 * delete the buffer.
 */
public class ManaAnchorTileEntity extends AbstractManaEndpointTileEntity {

    private static final int SCAN_INTERVAL = 20;
    private static final int DEFAULT_REACH = 1;

    private int scanCooldown;

    @Override
    protected ManaNodeKind nodeKind() {
        return ManaNodeKind.ANCHOR;
    }

    @Override
    public void updateEntity() {
        super.updateEntity();
        if (worldObj == null || worldObj.isRemote) {
            return;
        }
        if (!BotaniaAdditionConfig.wirelessEnabled()
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
        if (!isEnabled() || !canOperate(worldObj)) {
            return;
        }

        int perTick = effectiveRate();
        if (perTick <= 0) {
            return;
        }
        List<Pos> targets = network.anchorOutputs(worldObj, self);
        if (targets.isEmpty()) {
            return;
        }

        List<ResolvedTarget> resolved = new ArrayList<ResolvedTarget>();
        long totalSpace = 0L;
        for (Pos targetPos : targets) {
            IManaReceiver receiver = ManaReceiverHelper.find(worldObj, targetPos);
            if (receiver == null || receiver.isFull() || !receiver.canRecieveManaFromBursts()) {
                continue;
            }
            boolean exact = receiver instanceof IManaPool;
            int space = exact ? ManaReceiverHelper.space(receiver) : perTick;
            if (space <= 0) {
                continue;
            }
            resolved.add(new ResolvedTarget(receiver, space, exact));
            totalSpace += space;
        }
        if (resolved.isEmpty() || totalSpace <= 0L) {
            return;
        }

        int wanted = (int) Math.min(perTick, totalSpace);
        long withdrawn = network.withdraw(worldObj, self, wanted);
        if (withdrawn <= 0L) {
            return;
        }

        int remaining = (int) withdrawn;
        int remainingTargets = resolved.size();
        for (ResolvedTarget target : resolved) {
            if (remaining <= 0) {
                break;
            }
            int share = Math.max(1, remaining / Math.max(1, remainingTargets));
            int amount = Math.min(Math.min(share, target.space), remaining);
            remainingTargets--;
            if (amount <= 0) {
                continue;
            }
            int before = target.receiver.getCurrentMana();
            target.receiver.recieveMana(amount);
            int after = target.receiver.getCurrentMana();
            int accepted = Math.min(amount, Math.max(0, after - before));
            if (target.exact) {
                remaining -= accepted;
            } else {
                // Unknown receivers may consume instantly; refunding would duplicate mana.
                remaining -= amount;
            }
        }
        if (remaining > 0) {
            network.deposit(worldObj, self, remaining, BotaniaAdditionConfig.maxPerChannel());
        }
    }

    private void scan(ManaNetworkData network) {
        Pos self = Pos.of(this);
        List<Pos> candidates = new ArrayList<Pos>();
        for (Pos candidate : DeviceRanges.box(self,
                settings().reachX(DEFAULT_REACH), settings().reachY(DEFAULT_REACH),
                settings().reachZ(DEFAULT_REACH),
                settings().offsetX(), settings().offsetY(), settings().offsetZ())) {
            if (candidate.equals(self) || !worldObj.blockExists(candidate.x, candidate.y, candidate.z)
                    || ManaReceiverHelper.isCreativePool(worldObj, candidate)
                    || !settings().isTargetEnabled(candidate)) {
                continue;
            }
            IManaReceiver receiver = ManaReceiverHelper.find(worldObj, candidate);
            TileEntity tileEntity = worldObj.getTileEntity(candidate.x, candidate.y, candidate.z);
            if (receiver != null && ManaAutoTargets.isTarget(tileEntity)) {
                candidates.add(candidate);
            }
        }

        for (Pos existing : new ArrayList<Pos>(autoTargets())) {
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

        if (!linked.equals(autoTargets())) {
            autoTargets().clear();
            autoTargets().addAll(linked);
            markUpdated();
        }
    }

    /** Per-tick output budget, clamped to the configured ceiling. */
    public int effectiveRate() {
        return Math.min(settings().effectiveRate(BotaniaAdditionConfig.manaPerTick()),
                BotaniaAdditionConfig.maxManaPerTick());
    }

    @Override
    public int reachX() {
        return settings().reachX(DEFAULT_REACH);
    }

    @Override
    public int reachY() {
        return settings().reachY(DEFAULT_REACH);
    }

    @Override
    public int reachZ() {
        return settings().reachZ(DEFAULT_REACH);
    }

    @Override
    public int offsetX() {
        return settings().offsetX();
    }

    @Override
    public int offsetY() {
        return settings().offsetY();
    }

    @Override
    public int offsetZ() {
        return settings().offsetZ();
    }

    @Override
    public boolean showRange() {
        return settings().showRange();
    }

    @Override
    public DeviceState createState(World world) {
        ManaNetworkData network = ManaNetworkData.get(world);
        Pos self = Pos.of(this);
        List<LinkEntry> links = network == null
                ? new ArrayList<LinkEntry>() : network.links(world, self);
        return new DeviceState(settings().rate(), effectiveRate(), isEnabled(),
                network == null ? 0L : network.buffer(world, self),
                BotaniaAdditionConfig.maxPerChannel(),
                network == null ? "" : network.networkName(world, self),
                reachX(), reachY(), reachZ(), offsetX(), offsetY(), offsetZ(), showRange(), links);
    }

    @Override
    public net.minecraft.util.AxisAlignedBB getRenderBoundingBox() {
        int radius = Math.max(1, Math.max(reachX(), reachZ()) + 1);
        return net.minecraft.util.AxisAlignedBB.getBoundingBox(xCoord, yCoord, zCoord,
                xCoord + 1, yCoord + 1, zCoord + 1).expand(radius, reachY() + 1, radius);
    }

    private static final class ResolvedTarget {

        private final IManaReceiver receiver;
        private final int space;
        private final boolean exact;

        private ResolvedTarget(IManaReceiver receiver, int space, boolean exact) {
            this.receiver = receiver;
            this.space = space;
            this.exact = exact;
        }
    }
}
