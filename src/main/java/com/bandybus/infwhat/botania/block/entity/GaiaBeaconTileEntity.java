package com.bandybus.infwhat.botania.block.entity;

import com.bandybus.infwhat.botania.InfinityWhatBotania;
import com.bandybus.infwhat.botania.config.BotaniaAdditionConfig;
import com.bandybus.infwhat.botania.grid.ManaNetworkData;
import com.bandybus.infwhat.botania.util.GaiaSummonHelper;
import com.bandybus.infwhat.botania.util.ManaReceiverHelper;
import com.bandybus.infwhat.botania.util.Pos;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.DamageSource;
import vazkii.botania.api.mana.IManaPool;
import vazkii.botania.api.mana.IManaReceiver;
import vazkii.botania.common.entity.EntityDoppleganger;

import java.util.ArrayList;
import java.util.List;

/**
 * Finishes a Gaia fight once the guardian's boss bar is ready.
 *
 * <p>The beacon does not summon anything. Once a guardian in range has finished its spawn
 * animation ({@code getInvulTime() <= 0}), the beacon pays mana from a nearby pool or the
 * wireless network, registers every nearby player as a participant, and lands a player kill so
 * Botania's own drops, kill trigger and no-armor advancement run exactly like a normal kill.
 */
public class GaiaBeaconTileEntity extends TileEntity implements IManaReceiver {

    private static final int SCAN_INTERVAL = 10;
    private static final int FEEDBACK_INTERVAL = 40;
    private static final int SOURCE_RADIUS = 12;
    private static final String TAG_MANA = "Mana";

    private int ticks;
    private long nextFeedbackTick;
    private int mana;

    @Override
    public void updateEntity() {
        if (worldObj == null || worldObj.isRemote) {
            return;
        }
        if (!BotaniaAdditionConfig.gaiaBeaconEnabled()) {
            return;
        }
        if (++ticks < SCAN_INTERVAL) {
            return;
        }
        ticks = 0;
        finishFights();
    }

    private void finishFights() {
        AxisAlignedBB area = AxisAlignedBB.getBoundingBox(xCoord, yCoord, zCoord,
                xCoord + 1, yCoord + 1, zCoord + 1)
                .expand(BotaniaAdditionConfig.gaiaBeaconRadius(),
                        BotaniaAdditionConfig.gaiaBeaconRadius(),
                        BotaniaAdditionConfig.gaiaBeaconRadius());
        List<?> guardians = worldObj.getEntitiesWithinAABB(EntityDoppleganger.class, area);
        if (guardians.isEmpty()) {
            return;
        }
        List<EntityPlayer> players = new ArrayList<EntityPlayer>();
        for (Object object : worldObj.playerEntities) {
            if (object instanceof EntityPlayer) {
                EntityPlayer player = (EntityPlayer) object;
                if (player.isEntityAlive() && area.isVecInside(
                        net.minecraft.util.Vec3.createVectorHelper(
                                player.posX, player.posY, player.posZ))) {
                    players.add(player);
                }
            }
        }
        if (players.isEmpty()) {
            return;
        }
        EntityPlayer killer = nearest(players);
        int cost = BotaniaAdditionConfig.gaiaBeaconManaCost();
        for (Object object : guardians) {
            if (!(object instanceof EntityDoppleganger)) {
                continue;
            }
            EntityDoppleganger guardian = (EntityDoppleganger) object;
            if (!guardian.isEntityAlive()) {
                continue;
            }
            if (guardian.getInvulTime() > 0) {
                feedback(players, "message.infwhat_botania.gaia_beacon.waiting",
                        guardian.getInvulTime());
                continue;
            }
            if (!payMana(cost)) {
                InfinityWhatBotania.LOGGER.info(
                        "Gaia Beacon at {}, {}, {} needs {} mana for the guardian at {}, {}, {} "
                                + "but no pool or wireless network with enough mana is nearby",
                        xCoord, yCoord, zCoord, cost,
                        guardian.posX, guardian.posY, guardian.posZ);
                feedback(players, "message.infwhat_botania.gaia_beacon.no_mana", cost);
                return;
            }

            double x = guardian.posX;
            double y = guardian.posY + 1.0D;
            double z = guardian.posZ;
            addParticipants(guardian, players);
            InfinityWhatBotania.LOGGER.info(
                    "Gaia Beacon at {}, {}, {} finishing guardian (mana={})",
                    xCoord, yCoord, zCoord, cost);
            GaiaSummonHelper.clearMobSpawnTicks(guardian);
            guardian.setHealth(1.0F);
            guardian.attackEntityFrom(DamageSource.causePlayerDamage(killer), 32.0F);

            worldObj.playSoundEffect(xCoord + 0.5D, yCoord + 0.5D, zCoord + 0.5D,
                    "random.levelup", 1.0F, 0.6F);
            for (int index = 0; index < 24; index++) {
                worldObj.spawnParticle("magicCrit", x, y, z,
                        (worldObj.rand.nextDouble() - 0.5D) * 0.6D,
                        worldObj.rand.nextDouble() * 0.8D,
                        (worldObj.rand.nextDouble() - 0.5D) * 0.6D);
            }
            feedback(players, "message.infwhat_botania.gaia_beacon.finished");
        }
    }

    private void feedback(List<EntityPlayer> players, String key, Object... args) {
        if (worldObj.getTotalWorldTime() < nextFeedbackTick) {
            return;
        }
        nextFeedbackTick = worldObj.getTotalWorldTime() + FEEDBACK_INTERVAL;
        ChatComponentTranslation message = new ChatComponentTranslation(key, args);
        for (EntityPlayer player : players) {
            player.addChatMessage(message);
        }
    }

    private EntityPlayer nearest(List<EntityPlayer> players) {
        EntityPlayer nearest = players.get(0);
        double best = nearest.getDistanceSq(xCoord + 0.5D, yCoord + 0.5D, zCoord + 0.5D);
        for (int index = 1; index < players.size(); index++) {
            EntityPlayer candidate = players.get(index);
            double distance = candidate.getDistanceSq(
                    xCoord + 0.5D, yCoord + 0.5D, zCoord + 0.5D);
            if (distance < best) {
                best = distance;
                nearest = candidate;
            }
        }
        return nearest;
    }

    private static void addParticipants(EntityDoppleganger guardian,
                                        List<EntityPlayer> players) {
        List<String> attacked = GaiaSummonHelper.playersWhoAttacked(guardian);
        for (EntityPlayer player : players) {
            String name = player.getCommandSenderName();
            if (name != null && !attacked.contains(name)) {
                attacked.add(name);
            }
        }
    }

    /**
     * Pays from a real mana pool first and falls back to the wireless network buffer.
     */
    private boolean payMana(int cost) {
        if (cost <= 0) {
            return true;
        }
        if (mana >= cost) {
            mana -= cost;
            markDirty();
            return true;
        }
        List<Pos> devices = new ArrayList<Pos>();
        for (int dx = -SOURCE_RADIUS; dx <= SOURCE_RADIUS; dx++) {
            for (int dy = -SOURCE_RADIUS; dy <= SOURCE_RADIUS; dy++) {
                for (int dz = -SOURCE_RADIUS; dz <= SOURCE_RADIUS; dz++) {
                    int x = xCoord + dx;
                    int y = yCoord + dy;
                    int z = zCoord + dz;
                    if (!worldObj.blockExists(x, y, z)) {
                        continue;
                    }
                    TileEntity tileEntity = worldObj.getTileEntity(x, y, z);
                    if (tileEntity instanceof ManaWellTileEntity
                            || tileEntity instanceof ManaBeaconTileEntity
                            || tileEntity instanceof ManaAnchorTileEntity) {
                        devices.add(new Pos(x, y, z));
                    }
                    IManaReceiver receiver = ManaReceiverHelper.find(
                            worldObj, new Pos(x, y, z));
                    if (receiver instanceof IManaPool
                            && !ManaReceiverHelper.isCreativePool(worldObj, new Pos(x, y, z))
                            && receiver.getCurrentMana() >= cost) {
                        receiver.recieveMana(-cost);
                        return true;
                    }
                }
            }
        }
        if (devices.isEmpty()) {
            return false;
        }
        ManaNetworkData network = ManaNetworkData.get(worldObj);
        if (network == null) {
            return false;
        }
        for (Pos device : devices) {
            if (network.buffer(worldObj, device) >= cost) {
                network.withdraw(worldObj, device, cost);
                return true;
            }
        }
        return false;
    }

    @Override
    public int getCurrentMana() {
        return mana;
    }

    @Override
    public boolean isFull() {
        return mana >= capacity();
    }

    @Override
    public void recieveMana(int amount) {
        mana = Math.max(0, Math.min(capacity(), mana + amount));
        markDirty();
    }

    @Override
    public boolean canRecieveManaFromBursts() {
        return !isFull();
    }

    private static int capacity() {
        return Math.max(0, BotaniaAdditionConfig.gaiaBeaconCapacity());
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setInteger(TAG_MANA, mana);
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        mana = Math.max(0, tag.getInteger(TAG_MANA));
    }
}
