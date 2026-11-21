package com.bandybus.infwhat.botania.block.entity;

import com.bandybus.infwhat.botania.config.BotaniaAdditionConfig;
import com.bandybus.infwhat.botania.device.DeviceState;
import com.bandybus.infwhat.botania.device.LinkEntry;
import com.bandybus.infwhat.botania.device.ManaNodeKind;
import com.bandybus.infwhat.botania.grid.ManaNetworkData;
import com.bandybus.infwhat.botania.util.ManaReceiverHelper;
import com.bandybus.infwhat.botania.util.Pos;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import vazkii.botania.api.mana.IManaPool;
import vazkii.botania.api.mana.IManaReceiver;

import java.util.ArrayList;
import java.util.List;

/**
 * Moves mana into and out of a network buffer.
 *
 * <p>A beacon accepts mana pushed in by linked wells, may pull from linked storage blocks, and
 * deposits its buffer into the network. It never invents mana.
 */
public class ManaBeaconTileEntity extends AbstractManaEndpointTileEntity implements IManaReceiver {

    private static final int MANA_BUFFER = 100000;
    private static final String TAG_BUFFER = "BufferedMana";

    private int bufferedMana;

    @Override
    protected ManaNodeKind nodeKind() {
        return ManaNodeKind.BEACON;
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
        if (!isEnabled() || !canOperate(worldObj)) {
            return;
        }

        long capacity = BotaniaAdditionConfig.maxPerChannel();
        if (bufferedMana > 0) {
            int keeps = BotaniaAdditionConfig.applyLoss(bufferedMana);
            long accepted = network.deposit(worldObj, self, keeps, capacity);
            int drained = (int) Math.min(bufferedMana, accepted + (bufferedMana - keeps));
            if (drained > 0) {
                bufferedMana -= drained;
                markDirty();
            }
        }
        if (bufferedMana >= MANA_BUFFER) {
            return;
        }

        int budget = effectiveRate();
        if (budget <= 0) {
            return;
        }
        List<Pos> sources = network.beaconInputs(worldObj, self);
        if (sources.isEmpty()) {
            return;
        }
        int remaining = sources.size();
        for (Pos target : sources) {
            if (budget <= 0 || bufferedMana >= MANA_BUFFER) {
                return;
            }
            int share = Math.max(1, budget / Math.max(1, remaining));
            remaining--;
            IManaReceiver receiver = ManaReceiverHelper.find(worldObj, target);
            if (!(receiver instanceof IManaPool)
                    || ManaReceiverHelper.isCreativePool(worldObj, target)) {
                continue;
            }
            int current = receiver.getCurrentMana();
            int space = MANA_BUFFER - bufferedMana;
            int extracted = Math.min(Math.min(share, space), Math.min(current, budget));
            if (extracted <= 0) {
                continue;
            }
            receiver.recieveMana(-extracted);
            int after = receiver.getCurrentMana();
            int removed = Math.min(extracted, Math.max(0, current - after));
            if (removed <= 0) {
                continue;
            }
            bufferedMana += removed;
            markDirty();
            budget -= removed;
        }
    }

    /** Per-tick input budget, clamped to the configured ceiling. */
    public int effectiveRate() {
        return Math.min(settings().effectiveRate(BotaniaAdditionConfig.manaPerTick()),
                BotaniaAdditionConfig.maxManaPerTick());
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
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setInteger(TAG_BUFFER, bufferedMana);
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        bufferedMana = Math.max(0, tag.getInteger(TAG_BUFFER));
    }

    @Override
    public int getCurrentMana() {
        return bufferedMana;
    }

    @Override
    public boolean isFull() {
        return bufferedMana >= MANA_BUFFER;
    }

    @Override
    public void recieveMana(int mana) {
        if (mana <= 0 || bufferedMana >= MANA_BUFFER) {
            return;
        }
        bufferedMana = Math.min(MANA_BUFFER, bufferedMana + mana);
        markDirty();
    }

    @Override
    public boolean canRecieveManaFromBursts() {
        return isEnabled();
    }
}
