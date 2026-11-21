package com.bandybus.infwhat.botania.util;

import com.bandybus.infwhat.botania.InfinityWhatBotania;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import vazkii.botania.common.entity.EntityDoppleganger;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * Summons the Gaia Guardian without Botania's beacon, pylon or arena checks.
 *
 * <p>1.7.10 Botania exposes public setters for the source, hard mode, player count and mob-spawn
 * ticks, so only the attacker-name list needs reflection.
 */
public final class GaiaSummonHelper {

    private static final Field PLAYERS_WHO_ATTACKED = field("playersWhoAttacked");

    private GaiaSummonHelper() {
    }

    private static Field field(String name) {
        try {
            Field field = EntityDoppleganger.class.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Botania 1.7.10 field '" + name
                    + "' is missing; the Gaia helper cannot run", exception);
        }
    }

    public static boolean summon(EntityPlayer player, World world, int x, int y, int z,
                                 boolean hardMode) {
        if (world == null || world.isRemote || player == null) {
            return false;
        }
        if (world.difficultySetting == net.minecraft.world.EnumDifficulty.PEACEFUL) {
            return false;
        }
        List<?> existing = world.getEntitiesWithinAABB(EntityDoppleganger.class,
                net.minecraft.util.AxisAlignedBB.getBoundingBox(
                        x - 64, y - 64, z - 64, x + 65, y + 65, z + 65));
        for (Object object : existing) {
            if (object instanceof EntityDoppleganger
                    && ((EntityDoppleganger) object).isEntityAlive()) {
                return false;
            }
        }

        EntityDoppleganger guardian = new EntityDoppleganger(world);
        guardian.setLocationAndAngles(x + 0.5D, y + 3.0D, z + 0.5D, 0.0F, 0.0F);
        guardian.setInvulTime(160);
        guardian.setHealth(1.0F);
        guardian.setSource(x, y, z);
        guardian.setMobSpawnTicks(900);
        guardian.setHardMode(hardMode);
        int playerCount = Math.max(1, guardian.getPlayersAround().size());
        guardian.setPlayerCount(playerCount);

        float healthMultiplier = 1.0F;
        if (playerCount > 1) {
            healthMultiplier += playerCount * 0.25F;
        }
        guardian.getEntityAttribute(net.minecraft.entity.SharedMonsterAttributes.maxHealth)
                .setBaseValue(320.0F * healthMultiplier);
        // The constructor initialised health for the old maximum; heal to the scaled maximum so
        // the guardian does not enter the fight with one health point.
        guardian.setHealth(guardian.getMaxHealth());
        // 1.7.10 has no generic armor attribute; Botania's own hard-mode flag already changes the
        // guardian's combat behaviour, so no extra attribute is applied here.
        world.playSoundEffect(x + 0.5D, y + 3.0D, z + 0.5D, "mob.enderdragon.growl", 10.0F, 0.1F);
        world.spawnEntityInWorld(guardian);
        return true;
    }

    /** Clears Botania's minion-spawn grace period so the finishing hit can drop loot. */
    public static void clearMobSpawnTicks(EntityDoppleganger guardian) {
        guardian.setMobSpawnTicks(0);
    }

    /** Botania's attacker-name list used for kill credit and the no-armor advancement. */
    @SuppressWarnings("unchecked")
    public static List<String> playersWhoAttacked(EntityDoppleganger guardian) {
        try {
            return (List<String>) PLAYERS_WHO_ATTACKED.get(guardian);
        } catch (IllegalAccessException exception) {
            InfinityWhatBotania.LOGGER.error("Cannot read Gaia attacker list", exception);
            return new ArrayList<String>();
        }
    }
}
