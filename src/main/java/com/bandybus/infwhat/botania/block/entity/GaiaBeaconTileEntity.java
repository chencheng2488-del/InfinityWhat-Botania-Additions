package com.bandybus.infwhat.botania.block.entity;

import com.bandybus.infwhat.botania.config.BotaniaAdditionConfig;
import com.bandybus.infwhat.botania.grid.ManaNetworkData;
import com.bandybus.infwhat.botania.util.GaiaSummonHelper;
import com.bandybus.infwhat.botania.util.ManaReceiverHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.server.ServerWorld;
import vazkii.botania.api.mana.IManaPool;
import vazkii.botania.api.mana.IManaReceiver;
import vazkii.botania.common.entity.EntityDoppleganger;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Finishes a Gaia fight once the guardian's boss bar is ready.
 *
 * <p>The beacon does not summon anything. Once a guardian in range has finished its spawn
 * animation ({@code invulTime <= 0}), the beacon pays mana from a nearby pool, registers every
 * nearby player as a participant, and runs Botania's own death routine. Because the death goes
 * through {@link EntityDoppleganger#hurt}, drops, the kill trigger and the no-armor advancement
 * are handled by Botania exactly like a normal kill.
 */
public class GaiaBeaconTileEntity extends TileEntity implements ITickableTileEntity, IManaReceiver {

    private static final int SCAN_INTERVAL = 10;
    private static final int FEEDBACK_INTERVAL = 40;
    private static final int SOURCE_RADIUS = 12;
    private static final String TAG_MANA = "Mana";

    private int ticks;
    private long nextFeedbackTick;
    private int mana;

    public GaiaBeaconTileEntity() {
        super(ModBlockEntities.GAIA_BEACON.get());
    }

    @Override
    public void tick() {
        if (level == null || level.isClientSide) {
            return;
        }
        if (!BotaniaAdditionConfig.gaiaBeaconEnabled()) {
            return;
        }
        if (++ticks < SCAN_INTERVAL) {
            return;
        }
        ticks = 0;
        finishFights((ServerWorld) level);
    }

    private void finishFights(ServerWorld serverLevel) {
        AxisAlignedBB area = new AxisAlignedBB(worldPosition)
                .inflate(BotaniaAdditionConfig.gaiaBeaconRadius());
        List<EntityDoppleganger> guardians = level.getEntitiesOfClass(EntityDoppleganger.class,
                area, guardian -> guardian.isAlive());
        if (guardians.isEmpty()) {
            return;
        }
        List<PlayerEntity> players = level.getEntitiesOfClass(PlayerEntity.class, area,
                player -> player.isAlive() && !player.isSpectator());
        if (players.isEmpty()) {
            return;
        }
        PlayerEntity killer = nearest(players);
        int cost = BotaniaAdditionConfig.gaiaBeaconManaCost();
        for (EntityDoppleganger guardian : guardians) {
            if (guardian.getInvulTime() > 0) {
                feedback(players, "message.infwhat_botania.gaia_beacon.waiting",
                        Integer.valueOf(guardian.getInvulTime()));
                continue;
            }
            if (!payMana(serverLevel, cost)) {
                com.bandybus.infwhat.botania.InfinityWhatBotania.LOGGER.info(
                        "Gaia Beacon at {} needs {} mana for the guardian at {} but no pool "
                                + "or wireless network with enough mana is nearby",
                        worldPosition, Integer.valueOf(cost), guardian.blockPosition());
                feedback(players, "message.infwhat_botania.gaia_beacon.no_mana",
                        Integer.valueOf(cost));
                return;
            }

            double x = guardian.getX();
            double y = guardian.getY() + 1.0D;
            double z = guardian.getZ();
            addParticipants(guardian, players);
            guardian.trueKiller = killer;
            com.bandybus.infwhat.botania.InfinityWhatBotania.LOGGER.info(
                    "Gaia Beacon at {} finishing guardian at {} (mana={})",
                    worldPosition, guardian.blockPosition(), Integer.valueOf(cost));
            // Go through hurt instead of die so vanilla runs the full death sequence (removal,
            // boss bar cleanup, death animation). Botania caps each hit at 32 damage.
            // Botania also returns an empty loot table while mobSpawnTicks is positive, so it is
            // cleared first to keep the normal drops.
            GaiaSummonHelper.clearMobSpawnTicks(guardian);
            guardian.setHealth(1.0F);
            guardian.hurt(DamageSource.playerAttack(killer), 32.0F);

            level.playSound(null, worldPosition, SoundEvents.BEACON_DEACTIVATE,
                    SoundCategory.BLOCKS, 1.0F, 0.6F);
            serverLevel.sendParticles(ParticleTypes.END_ROD, x, y, z, 24,
                    0.6D, 0.8D, 0.6D, 0.05D);
            feedback(players, "message.infwhat_botania.gaia_beacon.finished");
        }
    }

    private void feedback(List<PlayerEntity> players, String key, Object... args) {
        if (level.getGameTime() < nextFeedbackTick) {
            return;
        }
        nextFeedbackTick = level.getGameTime() + FEEDBACK_INTERVAL;
        ITextComponent message = new TranslationTextComponent(key, args);
        for (PlayerEntity player : players) {
            player.displayClientMessage(message, true);
        }
    }

    private PlayerEntity nearest(List<PlayerEntity> players) {
        PlayerEntity nearest = players.get(0);
        double best = nearest.distanceToSqr(worldPosition.getX() + 0.5D,
                worldPosition.getY() + 0.5D, worldPosition.getZ() + 0.5D);
        for (int index = 1; index < players.size(); index++) {
            PlayerEntity candidate = players.get(index);
            double distance = candidate.distanceToSqr(worldPosition.getX() + 0.5D,
                    worldPosition.getY() + 0.5D, worldPosition.getZ() + 0.5D);
            if (distance < best) {
                best = distance;
                nearest = candidate;
            }
        }
        return nearest;
    }

    private static void addParticipants(EntityDoppleganger guardian, List<PlayerEntity> players) {
        List<UUID> attacked = GaiaSummonHelper.playersWhoAttacked(guardian);
        for (PlayerEntity player : players) {
            UUID id = player.getUUID();
            if (!attacked.contains(id)) {
                attacked.add(id);
            }
        }
    }

    /**
     * Pays from a real mana pool first and falls back to the wireless network buffer.
     *
     * <p>The wireless fallback lets a Mana Well feed a Mana Beacon somewhere else and this
     * beacon consume from the same channel without an intermediate pool.
     */
    private boolean payMana(ServerWorld serverLevel, int cost) {
        if (cost <= 0) {
            return true;
        }
        if (mana >= cost) {
            mana -= cost;
            setChanged();
            return true;
        }
        BlockPos min = worldPosition.offset(-SOURCE_RADIUS, -SOURCE_RADIUS, -SOURCE_RADIUS);
        BlockPos max = worldPosition.offset(SOURCE_RADIUS, SOURCE_RADIUS, SOURCE_RADIUS);
        List<BlockPos> devices = new ArrayList<BlockPos>();
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            if (!level.isLoaded(pos)) {
                continue;
            }
            TileEntity tileEntity = level.getBlockEntity(pos);
            if (tileEntity instanceof ManaWellTileEntity
                    || tileEntity instanceof ManaBeaconTileEntity
                    || tileEntity instanceof ManaAnchorTileEntity) {
                devices.add(pos.immutable());
            }
            IManaReceiver receiver = ManaReceiverHelper.find(level, pos);
            if (receiver instanceof IManaPool && receiver.getCurrentMana() >= cost) {
                receiver.receiveMana(-cost);
                return true;
            }
        }
        if (devices.isEmpty()) {
            return false;
        }
        ManaNetworkData network = ManaNetworkData.get(level);
        if (network == null) {
            return false;
        }
        for (BlockPos device : devices) {
            if (network.buffer(serverLevel, device) >= cost) {
                network.withdraw(serverLevel, device, cost);
                return true;
            }
        }
        return false;
    }

    // ---- mana storage (filled by the Mana Well / Mana Anchor like a pool) ----

    @Override
    public int getCurrentMana() {
        return mana;
    }

    @Override
    public boolean isFull() {
        return mana >= capacity();
    }

    @Override
    public void receiveMana(int amount) {
        mana = Math.max(0, Math.min(capacity(), mana + amount));
        setChanged();
    }

    @Override
    public boolean canReceiveManaFromBursts() {
        return !isFull();
    }

    private static int capacity() {
        return Math.max(0, BotaniaAdditionConfig.gaiaBeaconCapacity());
    }

    @Override
    public CompoundNBT save(CompoundNBT tag) {
        super.save(tag);
        tag.putInt(TAG_MANA, mana);
        return tag;
    }

    @Override
    public void load(net.minecraft.block.BlockState state, CompoundNBT tag) {
        super.load(state, tag);
        mana = Math.max(0, tag.getInt(TAG_MANA));
    }
}
