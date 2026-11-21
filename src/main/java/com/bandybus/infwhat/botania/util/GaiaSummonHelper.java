package com.bandybus.infwhat.botania.util;

import com.bandybus.infwhat.botania.InfinityWhatBotania;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemGroup;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import vazkii.botania.common.entity.EntityDoppleganger;
import vazkii.botania.common.entity.ModEntities;

import java.lang.reflect.Field;
import java.util.List;
import java.util.UUID;

/**
 * Summons the Gaia Guardian without Botania's beacon, pylon or arena checks.
 *
 * <p>Botania's {@code spawn} method runs its structure validation first, so the charm recreates
 * the small setup block that follows those checks. All private field names below are verified
 * against Botania 1.16.5-420; if Botania ever renames them the helper fails loudly instead of
 * silently summoning a guardian with a broken arena.
 */
public final class GaiaSummonHelper {

    private static final Field SOURCE = field("source");
    private static final Field MOB_SPAWN_TICKS = field("mobSpawnTicks");
    private static final Field HARD_MODE = field("hardMode");
    private static final Field PLAYER_COUNT = field("playerCount");
    private static final Field PLAYERS_WHO_ATTACKED = field("playersWhoAttacked");

    private GaiaSummonHelper() {
    }

    private static Field field(String name) {
        try {
            Field field = EntityDoppleganger.class.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Botania 1.16.5 field '" + name
                    + "' is missing; the Gaia helper cannot run", exception);
        }
    }

    public static boolean summon(PlayerEntity player, World world, BlockPos pos, boolean hardMode) {
        if (world == null || world.isClientSide || player == null) {
            return false;
        }
        if (world.getDifficulty() == net.minecraft.world.Difficulty.PEACEFUL) {
            return false;
        }
        // Botania refuses a second guardian in the same arena; keep that rule for the charm.
        List<EntityDoppleganger> existing = world.getEntitiesOfClass(EntityDoppleganger.class,
                new net.minecraft.util.math.AxisAlignedBB(pos).inflate(64.0D),
                entity -> entity.isAlive());
        if (!existing.isEmpty()) {
            return false;
        }

        EntityDoppleganger guardian = ModEntities.DOPPLEGANGER.create(world);
        if (guardian == null) {
            return false;
        }
        double x = pos.getX() + 0.5D;
        double y = pos.getY() + 3.0D;
        double z = pos.getZ() + 0.5D;
        guardian.setPos(x, y, z);
        guardian.setInvulTime(160);
        guardian.setHealth(1.0F);
        set(SOURCE, guardian, pos);
        set(MOB_SPAWN_TICKS, guardian, 900);
        set(HARD_MODE, guardian, hardMode);
        int playerCount = Math.max(1, guardian.getPlayersAround().size());
        set(PLAYER_COUNT, guardian, playerCount);

        float healthMultiplier = 1.0F;
        if (playerCount > 1) {
            healthMultiplier += playerCount * 0.25F;
        }
        guardian.getAttribute(net.minecraft.entity.ai.attributes.Attributes.MAX_HEALTH)
                .setBaseValue(320.0F * healthMultiplier);
        if (hardMode) {
            guardian.getAttribute(net.minecraft.entity.ai.attributes.Attributes.ARMOR)
                    .setBaseValue(15.0D);
        }
        guardian.playSound(net.minecraft.util.SoundEvents.ENDER_DRAGON_GROWL, 10.0F, 0.1F);
        guardian.finalizeSpawn((net.minecraft.world.server.ServerWorld) world,
                world.getCurrentDifficultyAt(guardian.blockPosition()),
                net.minecraft.entity.SpawnReason.EVENT, null, null);
        ((net.minecraft.world.server.ServerWorld) world).addFreshEntity(guardian);
        return true;
    }

    /** Clears Botania's minion-spawn grace period so the finishing hit can drop loot. */
    public static void clearMobSpawnTicks(EntityDoppleganger guardian) {
        set(MOB_SPAWN_TICKS, guardian, 0);
    }

    /** Botania's attacker list used for kill credit and the no-armor advancement. */
    @SuppressWarnings("unchecked")
    public static List<UUID> playersWhoAttacked(EntityDoppleganger guardian) {
        try {
            return (List<UUID>) PLAYERS_WHO_ATTACKED.get(guardian);
        } catch (IllegalAccessException exception) {
            InfinityWhatBotania.LOGGER.error("Cannot read Gaia attacker list", exception);
            return java.util.Collections.emptyList();
        }
    }

    private static void set(Field field, EntityDoppleganger guardian, Object value) {
        try {
            field.set(guardian, value);
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("Cannot set Gaia field " + field.getName(), exception);
        }
    }
}
