package com.bandybus.infwhat.botania.block.entity;

import com.bandybus.infwhat.botania.config.BotaniaAdditionConfig;
import com.bandybus.infwhat.botania.grid.ManaNetworkData;
import com.bandybus.infwhat.botania.mixin.GaiaGuardianEntityAccessor;
import com.bandybus.infwhat.botania.util.ManaReceiverHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.slf4j.Logger;
import vazkii.botania.api.mana.ManaPool;
import vazkii.botania.api.mana.ManaReceiver;
import vazkii.botania.common.entity.GaiaGuardianEntity;

import com.mojang.logging.LogUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Finishes a Gaia fight once the guardian's boss bar is ready.
 *
 * <p>The beacon does not summon anything. Once a guardian in range has finished its spawn
 * animation ({@code invulTime <= 0}), the beacon pays mana from a nearby pool, registers every
 * nearby player as a participant, and runs Botania's own death routine. Because the death goes
 * through {@link GaiaGuardianEntity#die}, drops, the kill trigger and the no-armor advancement
 * are handled by Botania exactly like a normal kill.
 */
public class GaiaBeaconBlockEntity extends BlockEntity implements ManaReceiver {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int SCAN_INTERVAL = 10;
    private static final int FEEDBACK_INTERVAL = 40;
    private static final int SOURCE_RADIUS = 12;
    private static final String TAG_MANA = "Mana";

    private int ticks;
    private long nextFeedbackTick;
    private int mana;

    public GaiaBeaconBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GAIA_BEACON.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state,
                                  GaiaBeaconBlockEntity beacon) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        if (!BotaniaAdditionConfig.gaiaBeaconEnabled()) {
            return;
        }
        if (++beacon.ticks < SCAN_INTERVAL) {
            return;
        }
        beacon.ticks = 0;
        beacon.finishFights(serverLevel);
    }

    private void finishFights(ServerLevel level) {
        AABB area = new AABB(worldPosition).inflate(BotaniaAdditionConfig.gaiaBeaconRadius());
        List<GaiaGuardianEntity> guardians = level.getEntitiesOfClass(GaiaGuardianEntity.class, area,
                guardian -> guardian.isAlive() && !guardian.isDeadOrDying());
        if (guardians.isEmpty()) {
            return;
        }
        List<Player> players = level.getEntitiesOfClass(Player.class, area,
                player -> player.isAlive() && !player.isSpectator());
        if (players.isEmpty()) {
            return;
        }
        Player killer = nearest(players);
        int cost = BotaniaAdditionConfig.gaiaBeaconManaCost();
        for (GaiaGuardianEntity guardian : guardians) {
            if (guardian.getInvulTime() > 0) {
                feedback(level, players, "message.infwhat_botania.gaia_beacon.waiting",
                        guardian.getInvulTime());
                continue;
            }
            if (!payMana(level, cost)) {
                LOGGER.info("Gaia Beacon at {} needs {} mana for the guardian at {} but no pool "
                        + "or wireless network with enough mana is nearby", worldPosition, cost,
                        guardian.blockPosition());
                feedback(level, players, "message.infwhat_botania.gaia_beacon.no_mana", cost);
                return;
            }

            double x = guardian.getX();
            double y = guardian.getY() + 1.0D;
            double z = guardian.getZ();
            addParticipants(guardian, players);
            guardian.trueKiller = killer;
            LOGGER.info("Gaia Beacon at {} finishing guardian at {} (hardMode={}, mana={})",
                    worldPosition, guardian.blockPosition(), guardian.isHardMode(), cost);
            // Go through hurt instead of die so vanilla runs the full death sequence (removal,
            // boss bar cleanup, death animation). Botania caps each hit at 32 damage.
            // Botania also returns an empty loot table while mobSpawnTicks is positive, so it is
            // cleared first to keep the normal drops.
            GaiaGuardianEntityAccessor accessor = (GaiaGuardianEntityAccessor) guardian;
            accessor.infwhat$setMobSpawnTicks(0);
            guardian.setHealth(1.0F);
            guardian.hurt(level.damageSources().playerAttack(killer), 32.0F);

            level.playSound(null, worldPosition, SoundEvents.BEACON_DEACTIVATE,
                    SoundSource.BLOCKS, 1.0F, 0.6F);
            level.sendParticles(ParticleTypes.END_ROD, x, y, z, 24, 0.6D, 0.8D, 0.6D, 0.05D);
            feedback(level, players, "message.infwhat_botania.gaia_beacon.finished");
        }
    }

    private void feedback(ServerLevel level, List<Player> players, String key, Object... args) {
        if (level.getGameTime() < nextFeedbackTick) {
            return;
        }
        nextFeedbackTick = level.getGameTime() + FEEDBACK_INTERVAL;
        Component message = Component.translatable(key, args);
        for (Player player : players) {
            player.displayClientMessage(message, true);
        }
    }

    private Player nearest(List<Player> players) {
        Player nearest = players.get(0);
        double best = nearest.distanceToSqr(worldPosition.getX() + 0.5D,
                worldPosition.getY() + 0.5D, worldPosition.getZ() + 0.5D);
        for (int index = 1; index < players.size(); index++) {
            Player candidate = players.get(index);
            double distance = candidate.distanceToSqr(worldPosition.getX() + 0.5D,
                    worldPosition.getY() + 0.5D, worldPosition.getZ() + 0.5D);
            if (distance < best) {
                best = distance;
                nearest = candidate;
            }
        }
        return nearest;
    }

    private static void addParticipants(GaiaGuardianEntity guardian, List<Player> players) {
        List<UUID> attacked = ((GaiaGuardianEntityAccessor) guardian).infwhat$playersWhoAttacked();
        for (Player player : players) {
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
    private boolean payMana(ServerLevel level, int cost) {
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
        List<BlockPos> devices = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            if (!level.hasChunkAt(pos)) {
                continue;
            }
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof ManaWellBlockEntity
                    || blockEntity instanceof ManaBeaconBlockEntity
                    || blockEntity instanceof ManaAnchorBlockEntity) {
                devices.add(pos.immutable());
            }
            if (ManaReceiverHelper.find(level, pos, null) instanceof ManaPool pool
                    && pool.getCurrentMana() >= cost) {
                pool.receiveMana(-cost);
                return true;
            }
        }
        if (devices.isEmpty()) {
            return false;
        }
        ManaNetworkData network = ManaNetworkData.get(level);
        for (BlockPos device : devices) {
            if (network.buffer(level, device) >= cost) {
                network.withdraw(level, device, cost);
                return true;
            }
        }
        return false;
    }

    // ---- mana storage (filled by the Mana Well / Mana Anchor like a pool) ----

    @Override
    public Level getManaReceiverLevel() {
        return level;
    }

    @Override
    public BlockPos getManaReceiverPos() {
        return worldPosition;
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
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt(TAG_MANA, mana);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        mana = Math.max(0, tag.getInt(TAG_MANA));
    }
}
