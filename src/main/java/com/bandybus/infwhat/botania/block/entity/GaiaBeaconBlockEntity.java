package com.bandybus.infwhat.botania.block.entity;

import com.bandybus.infwhat.botania.InfinityWhatBotania;
import com.bandybus.infwhat.botania.config.BotaniaAdditionConfig;
import com.bandybus.infwhat.botania.grid.ManaNetworkData;
import com.bandybus.infwhat.botania.util.GaiaSummonHelper;
import com.bandybus.infwhat.botania.util.ManaReceiverHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import vazkii.botania.api.BotaniaForgeCapabilities;
import vazkii.botania.api.mana.IManaPool;
import vazkii.botania.api.mana.IManaReceiver;
import vazkii.botania.common.entity.EntityDoppleganger;

import javax.annotation.Nullable;
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
public class GaiaBeaconBlockEntity extends BlockEntity implements IManaReceiver {

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

    private void finishFights(ServerLevel serverLevel) {
        AABB area = new AABB(worldPosition)
                .inflate(BotaniaAdditionConfig.gaiaBeaconRadius());
        List<EntityDoppleganger> guardians = level.getEntitiesOfClass(EntityDoppleganger.class,
                area, guardian -> guardian.isAlive());
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
        for (EntityDoppleganger guardian : guardians) {
            if (guardian.getInvulTime() > 0) {
                feedback(players, "message.infwhat_botania.gaia_beacon.waiting",
                        guardian.getInvulTime());
                continue;
            }
            if (!payMana(serverLevel, cost)) {
                InfinityWhatBotania.LOGGER.info(
                        "Gaia Beacon at {} needs {} mana for the guardian at {} but no pool "
                                + "or wireless network with enough mana is nearby",
                        worldPosition, cost, guardian.blockPosition());
                feedback(players, "message.infwhat_botania.gaia_beacon.no_mana", cost);
                return;
            }

            double x = guardian.getX();
            double y = guardian.getY() + 1.0D;
            double z = guardian.getZ();
            addParticipants(guardian, players);
            guardian.trueKiller = killer;
            InfinityWhatBotania.LOGGER.info(
                    "Gaia Beacon at {} finishing guardian at {} (mana={})",
                    worldPosition, guardian.blockPosition(), cost);
            GaiaSummonHelper.clearMobSpawnTicks(guardian);
            guardian.setHealth(1.0F);
            guardian.hurt(DamageSource.playerAttack(killer), 32.0F);

            level.playSound(null, worldPosition, SoundEvents.BEACON_DEACTIVATE,
                    SoundSource.BLOCKS, 1.0F, 0.6F);
            serverLevel.sendParticles(ParticleTypes.END_ROD, x, y, z, 24,
                    0.6D, 0.8D, 0.6D, 0.05D);
            feedback(players, "message.infwhat_botania.gaia_beacon.finished");
        }
    }

    private void feedback(List<Player> players, String key, Object... args) {
        if (level.getGameTime() < nextFeedbackTick) {
            return;
        }
        nextFeedbackTick = level.getGameTime() + FEEDBACK_INTERVAL;
        Component message = new TranslatableComponent(key, args);
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

    private static void addParticipants(EntityDoppleganger guardian, List<Player> players) {
        List<UUID> attacked = GaiaSummonHelper.playersWhoAttacked(guardian);
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
    private boolean payMana(ServerLevel serverLevel, int cost) {
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
            if (!level.isLoaded(pos)) {
                continue;
            }
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof ManaWellBlockEntity
                    || blockEntity instanceof ManaBeaconBlockEntity
                    || blockEntity instanceof ManaAnchorBlockEntity) {
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
        ManaNetworkData network = ManaNetworkData.get(serverLevel);
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

    /** Expose the receiver through Botania's Forge capability so spreaders can bind to it. */
    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction side) {
        if (capability == BotaniaForgeCapabilities.MANA_RECEIVER) {
            return LazyOptional.of(() -> this).cast();
        }
        return super.getCapability(capability, side);
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
