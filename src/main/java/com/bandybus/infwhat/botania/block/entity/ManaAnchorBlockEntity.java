package com.bandybus.infwhat.botania.block.entity;

import com.bandybus.infwhat.botania.config.BotaniaAdditionConfig;
import com.bandybus.infwhat.botania.device.DeviceRanges;
import com.bandybus.infwhat.botania.device.DeviceState;
import com.bandybus.infwhat.botania.device.LinkEntry;
import com.bandybus.infwhat.botania.device.ManaNodeKind;
import com.bandybus.infwhat.botania.grid.ManaNetworkData;
import com.bandybus.infwhat.botania.util.ManaAutoTargets;
import com.bandybus.infwhat.botania.util.ManaReceiverHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import vazkii.botania.api.mana.ManaPool;
import vazkii.botania.api.mana.ManaReceiver;
import vazkii.botania.common.block.mana.ManaPoolBlock;

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
public class ManaAnchorBlockEntity extends AbstractManaEndpointBlockEntity {

    private static final int SCAN_INTERVAL = 20;
    private static final int DEFAULT_REACH = 1;

    private int scanCooldown;

    public ManaAnchorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MANA_ANCHOR, pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state,
                                  ManaAnchorBlockEntity anchor) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        if (!BotaniaAdditionConfig.wirelessEnabled()
                || !BotaniaAdditionConfig.canOperate(serverLevel)) {
            return;
        }
        ManaNetworkData.get(serverLevel).validateLinks(serverLevel, pos);
        if (anchor.scanCooldown-- <= 0) {
            anchor.scan(serverLevel);
            anchor.scanCooldown = SCAN_INTERVAL;
        }
        if (!anchor.isEnabled() || !anchor.canOperate(serverLevel)) {
            return;
        }

        int perTick = anchor.effectiveRate();
        if (perTick <= 0) {
            return;
        }
        ManaNetworkData network = ManaNetworkData.get(serverLevel);
        List<BlockPos> targets = network.anchorOutputs(serverLevel, pos);
        if (targets.isEmpty()) {
            return;
        }

        List<ResolvedTarget> resolved = new ArrayList<>();
        long totalSpace = 0L;
        for (BlockPos targetPos : targets) {
            ManaReceiver receiver = ManaReceiverHelper.find(serverLevel, targetPos, null);
            if (receiver == null || receiver.isFull() || !receiver.canReceiveManaFromBursts()) {
                continue;
            }
            boolean exact = receiver instanceof ManaPool;
            int space;
            if (exact) {
                ManaPool pool = (ManaPool) receiver;
                space = Math.max(0, pool.getMaxMana() - pool.getCurrentMana());
            } else {
                // Unknown receiver types have no max-mana contract. Give them at most one tick's
                // budget; their accepted amount is not measurable, so it is never refunded.
                space = perTick;
            }
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
        long withdrawn = network.withdraw(serverLevel, pos, wanted);
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
            target.receiver.receiveMana(amount);
            int after = target.receiver.getCurrentMana();
            int accepted = Math.min(amount, Math.max(0, after - before));
            if (target.exact) {
                // Mana pools have a precise current/max contract, so a shortfall is refunded.
                remaining -= accepted;
            } else {
                // For an unknown receiver we cannot distinguish "consumed instantly" from
                // "rejected". Treat the whole amount as consumed; refunding it could duplicate
                // mana when the receiver used it immediately.
                remaining -= amount;
            }
        }
        if (remaining > 0) {
            network.deposit(serverLevel, pos, remaining, BotaniaAdditionConfig.maxPerChannel());
        }
    }

    private void scan(ServerLevel level) {
        List<BlockPos> candidates = new ArrayList<>();
        for (BlockPos candidate : DeviceRanges.box(worldPosition,
                settings().reachX(DEFAULT_REACH), settings().reachY(DEFAULT_REACH),
                settings().reachZ(DEFAULT_REACH),
                settings().offsetX(), settings().offsetY(), settings().offsetZ())) {
            if (candidate.equals(worldPosition) || !level.hasChunkAt(candidate)
                    || isCreativePool(level, candidate)
                    || !settings().isTargetEnabled(candidate)) {
                continue;
            }
            ManaReceiver receiver = ManaReceiverHelper.find(level, candidate, null);
            if (receiver != null && ManaAutoTargets.isTarget(level.getBlockEntity(candidate))) {
                candidates.add(candidate.immutable());
            }
        }

        ManaNetworkData network = ManaNetworkData.get(level);
        for (BlockPos existing : new ArrayList<>(autoTargets())) {
            if (!candidates.contains(existing)) {
                network.autoDisconnect(level, worldPosition, existing);
            }
        }

        List<BlockPos> linked = new ArrayList<>();
        for (BlockPos candidate : candidates) {
            if (network.hasDirectEdge(level, worldPosition, candidate)) {
                if (network.isAutoEdge(level, worldPosition, candidate)) {
                    linked.add(candidate);
                }
                continue;
            }
            ManaNetworkData.ConnectResult result =
                    network.autoConnect(level, worldPosition, candidate);
            if ((result.result() == ManaNetworkData.Result.OK
                    || result.result() == ManaNetworkData.Result.ALREADY)
                    && network.isAutoEdge(level, worldPosition, candidate)) {
                linked.add(candidate);
            }
        }

        if (!linked.equals(autoTargets())) {
            autoTargets().clear();
            autoTargets().addAll(linked);
            setChanged();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    private static boolean isCreativePool(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos).getBlock() instanceof ManaPoolBlock pool
                && pool.variant == ManaPoolBlock.Variant.CREATIVE;
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
    public DeviceState createState(ServerLevel level) {
        ManaNetworkData network = ManaNetworkData.get(level);
        List<LinkEntry> links = network.links(level, worldPosition);
        return new DeviceState(settings().rate(), effectiveRate(), isEnabled(),
                network.buffer(level, worldPosition), BotaniaAdditionConfig.maxPerChannel(),
                network.networkName(level, worldPosition),
                reachX(), reachY(), reachZ(),
                offsetX(), offsetY(), offsetZ(), showRange(), links);
    }

    @Override
    public void setLevel(Level level) {
        super.setLevel(level);
        if (level instanceof ServerLevel serverLevel) {
            ManaNetworkData.get(serverLevel).register(serverLevel, worldPosition, ManaNodeKind.ANCHOR);
        }
    }

    private record ResolvedTarget(ManaReceiver receiver, int space, boolean exact) {
    }
}
