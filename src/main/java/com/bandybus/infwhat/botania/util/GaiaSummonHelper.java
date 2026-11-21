package com.bandybus.infwhat.botania.util;

import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.World;
import vazkii.botania.common.entity.EntityDoppleganger;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

/**
 * Summons the Gaia Guardian without Botania's beacon, pylon or arena checks.
 *
 * <p>Botania's {@code spawn} method insists on a beacon, four Gaia Pylons and a clear arena. The
 * 1.12.2 build of this add-on has no mixin bootstrap, so the spawn routine is reproduced here with
 * the exact same entity configuration: 160 ticks of spawn invulnerability, 900 minion-spawn ticks,
 * 320 health per nearby player, hard-mode armor and the dragon growl. The guardian itself, its AI,
 * boss bar, drops and advancement logic remain Botania's own code.
 */
public final class GaiaSummonHelper {

	private static final int SPAWN_TICKS = 160;
	private static final int MOB_SPAWN_TICKS = 900;
	private static final float MAX_HP = 320F;
	private static final float SEARCH_RANGE = 15F;

	private static final Field FIELD_SOURCE = Reflect.field(EntityDoppleganger.class, "source");
	private static final Field FIELD_MOB_SPAWN_TICKS =
			Reflect.field(EntityDoppleganger.class, "mobSpawnTicks");
	private static final Field FIELD_HARD_MODE = Reflect.field(EntityDoppleganger.class, "hardMode");
	private static final Field FIELD_PLAYER_COUNT =
			Reflect.field(EntityDoppleganger.class, "playerCount");
	private static final Field FIELD_ATTACKERS =
			Reflect.field(EntityDoppleganger.class, "playersWhoAttacked");
	private static final Method METHOD_INITIAL_SPAWN = Reflect.methodByParameters(
			EntityDoppleganger.class, DifficultyInstance.class, IEntityLivingData.class);

	public static boolean summon(EntityPlayer player, World world, BlockPos pos, boolean hardMode) {
		if (world == null || world.isRemote || player == null) {
			return false;
		}
		if (world.getDifficulty() == EnumDifficulty.PEACEFUL) {
			player.sendStatusMessage(new TextComponentTranslation("botaniamisc.peacefulNoob"), true);
			return false;
		}
		if (!guardiansAround(world, pos).isEmpty()) {
			return false;
		}

		EntityDoppleganger guardian = new EntityDoppleganger(world);
		guardian.setPosition(pos.getX() + 0.5D, pos.getY() + 3.0D, pos.getZ() + 0.5D);
		guardian.setInvulTime(SPAWN_TICKS);
		guardian.setHealth(1.0F);
		Reflect.set(guardian, FIELD_SOURCE, pos);
		Reflect.set(guardian, FIELD_MOB_SPAWN_TICKS, MOB_SPAWN_TICKS);
		Reflect.set(guardian, FIELD_HARD_MODE, hardMode);

		int playerCount = guardian.getPlayersAround().size();
		Reflect.set(guardian, FIELD_PLAYER_COUNT, playerCount);
		guardian.getAttributeMap().getAttributeInstance(SharedMonsterAttributes.MAX_HEALTH)
				.setBaseValue(MAX_HP * Math.max(1, playerCount));
		if (hardMode) {
			guardian.getAttributeMap().getAttributeInstance(SharedMonsterAttributes.ARMOR)
					.setBaseValue(15.0D);
		}

		guardian.playSound(SoundEvents.ENTITY_ENDERDRAGON_GROWL, 10.0F, 0.1F);
		Reflect.invoke(METHOD_INITIAL_SPAWN, guardian,
				world.getDifficultyForLocation(new BlockPos(guardian)), null);
		world.spawnEntity(guardian);
		return true;
	}

	/** Botania's own guardian count check, reproduced so we never stack two fights. */
	public static List<EntityDoppleganger> guardiansAround(World world, BlockPos source) {
		return world.getEntitiesWithinAABB(EntityDoppleganger.class,
				new AxisAlignedBB(source.getX() + 0.5D - SEARCH_RANGE,
						source.getY() + 0.5D - SEARCH_RANGE,
						source.getZ() + 0.5D - SEARCH_RANGE,
						source.getX() + 0.5D + SEARCH_RANGE,
						source.getY() + 0.5D + SEARCH_RANGE,
						source.getZ() + 0.5D + SEARCH_RANGE));
	}

	/**
	 * Botania returns an empty loot table while this counter is positive (the minion spawn phase),
	 * so the beacon clears it before the finishing hit.
	 */
	public static void clearMobSpawnTicks(EntityDoppleganger guardian) {
		Reflect.set(guardian, FIELD_MOB_SPAWN_TICKS, 0);
	}

	/** Adds every nearby player to Botania's own kill-credit list. */
	public static void addParticipants(EntityDoppleganger guardian, List<EntityPlayer> players) {
		if (FIELD_ATTACKERS == null || guardian == null) {
			return;
		}
		try {
			@SuppressWarnings("unchecked")
			List<UUID> attacked = (List<UUID>) FIELD_ATTACKERS.get(guardian);
			if (attacked == null) {
				return;
			}
			for (EntityPlayer player : players) {
				UUID id = player.getUniqueID();
				if (!attacked.contains(id)) {
					attacked.add(id);
				}
			}
		} catch (IllegalAccessException exception) {
			// The field was made accessible in Reflect.field; this cannot normally happen.
		}
	}

	private GaiaSummonHelper() {
	}
}
