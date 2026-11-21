package com.bandybus.infwhat.botania.block.tile;

import com.bandybus.infwhat.botania.InfinityWhatBotania;
import com.bandybus.infwhat.botania.config.BotaniaAdditionConfig;
import com.bandybus.infwhat.botania.grid.ManaNetworkData;
import com.bandybus.infwhat.botania.util.GaiaSummonHelper;
import com.google.common.base.Predicate;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ITickable;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import vazkii.botania.api.mana.IManaReceiver;
import vazkii.botania.common.block.tile.mana.TilePool;
import vazkii.botania.common.entity.EntityDoppleganger;

import java.util.ArrayList;
import java.util.List;

/**
 * Finishes a Gaia fight once the guardian's boss bar is ready.
 *
 * <p>The beacon does not summon anything. Once a guardian in range has finished its spawn
 * animation ({@code invulTime <= 0}), the beacon pays mana from a nearby pool, registers every
 * nearby player as a participant, and runs Botania's own death routine. Because the finishing hit
 * goes through {@code attackEntityFrom}, drops, the kill trigger and the no-armor advancement are
 * handled by Botania exactly like a normal kill.
 */
public class GaiaBeaconTileEntity extends TileEntity implements IManaReceiver, ITickable {

	private static final int SCAN_INTERVAL = 10;
	private static final int FEEDBACK_INTERVAL = 40;
	private static final int SOURCE_RADIUS = 12;
	private static final String TAG_MANA = "Mana";

	private int ticks;
	private long nextFeedbackTick;
	private int mana;

	public GaiaBeaconTileEntity() {
	}

	@Override
	public void update() {
		if (world == null || world.isRemote) {
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
		int radius = BotaniaAdditionConfig.gaiaBeaconRadius();
		AxisAlignedBB area = new AxisAlignedBB(pos).grow(radius);
		List<EntityDoppleganger> guardians = world.getEntitiesWithinAABB(
				EntityDoppleganger.class, area, new Predicate<EntityDoppleganger>() {
					@Override
					public boolean apply(EntityDoppleganger guardian) {
						return guardian.isEntityAlive();
					}
				});
		if (guardians.isEmpty()) {
			return;
		}
		List<EntityPlayer> players = world.getEntitiesWithinAABB(EntityPlayer.class, area,
				new Predicate<EntityPlayer>() {
					@Override
					public boolean apply(EntityPlayer player) {
						return player.isEntityAlive() && !player.isSpectator();
					}
				});
		if (players.isEmpty()) {
			return;
		}
		EntityPlayer killer = nearest(players);
		int cost = BotaniaAdditionConfig.gaiaBeaconManaCost();
		for (EntityDoppleganger guardian : guardians) {
			if (guardian.getInvulTime() > 0) {
				feedback(players, "message.infwhat_botania.gaia_beacon.waiting",
						guardian.getInvulTime());
				continue;
			}
			if (!payMana(cost)) {
				InfinityWhatBotania.LOGGER.info(
						"Gaia Beacon at {} needs {} mana for the guardian at {} but no pool "
								+ "or wireless network with enough mana is nearby",
						pos, cost, guardian.getPosition());
				feedback(players, "message.infwhat_botania.gaia_beacon.no_mana", cost);
				return;
			}

			double x = guardian.posX;
			double y = guardian.posY + 1.0D;
			double z = guardian.posZ;
			GaiaSummonHelper.addParticipants(guardian, players);
			guardian.trueKiller = killer;
			InfinityWhatBotania.LOGGER.info(
					"Gaia Beacon at {} finishing guardian at {} (mana={})",
					pos, guardian.getPosition(), cost);
			// Go through attackEntityFrom instead of death so vanilla runs the full death
			// sequence (removal, boss bar cleanup, death animation). Botania caps each hit at 32.
			// Botania also returns an empty loot table while mobSpawnTicks is positive, so it is
			// cleared first to keep the normal drops.
			GaiaSummonHelper.clearMobSpawnTicks(guardian);
			guardian.setHealth(1.0F);
			guardian.attackEntityFrom(DamageSource.causePlayerDamage(killer), 32.0F);

			world.playSound(null, pos, SoundEvents.BLOCK_END_GATEWAY_SPAWN,
					SoundCategory.BLOCKS, 1.0F, 0.6F);
			if (world instanceof net.minecraft.world.WorldServer) {
				((net.minecraft.world.WorldServer) world).spawnParticle(
						EnumParticleTypes.END_ROD, x, y, z, 24, 0.6D, 0.8D, 0.6D, 0.05D);
			}
			feedback(players, "message.infwhat_botania.gaia_beacon.finished");
		}
	}

	private void feedback(List<EntityPlayer> players, String key, Object... arguments) {
		if (world.getTotalWorldTime() < nextFeedbackTick) {
			return;
		}
		nextFeedbackTick = world.getTotalWorldTime() + FEEDBACK_INTERVAL;
		TextComponentTranslation message = new TextComponentTranslation(key, arguments);
		for (EntityPlayer player : players) {
			player.sendStatusMessage(message, true);
		}
	}

	private EntityPlayer nearest(List<EntityPlayer> players) {
		EntityPlayer nearest = players.get(0);
		double best = nearest.getDistanceSq(pos.getX() + 0.5D,
				pos.getY() + 0.5D, pos.getZ() + 0.5D);
		for (int index = 1; index < players.size(); index++) {
			EntityPlayer candidate = players.get(index);
			double distance = candidate.getDistanceSq(pos.getX() + 0.5D,
					pos.getY() + 0.5D, pos.getZ() + 0.5D);
			if (distance < best) {
				best = distance;
				nearest = candidate;
			}
		}
		return nearest;
	}

	/**
	 * Pays from a real mana pool first and falls back to the wireless network buffer.
	 *
	 * <p>The wireless fallback lets a Mana Well feed a Mana Beacon somewhere else and this
	 * beacon consume from the same channel without an intermediate pool.
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
		BlockPos min = pos.add(-SOURCE_RADIUS, -SOURCE_RADIUS, -SOURCE_RADIUS);
		BlockPos max = pos.add(SOURCE_RADIUS, SOURCE_RADIUS, SOURCE_RADIUS);
		List<BlockPos> devices = new ArrayList<BlockPos>();
		for (BlockPos candidate : BlockPos.getAllInBoxMutable(min, max)) {
			if (!world.isBlockLoaded(candidate)) {
				continue;
			}
			TileEntity tileEntity = world.getTileEntity(candidate);
			if (tileEntity instanceof ManaWellTileEntity
					|| tileEntity instanceof ManaBeaconTileEntity
					|| tileEntity instanceof ManaAnchorTileEntity) {
				devices.add(candidate.toImmutable());
			}
			if (tileEntity instanceof TilePool) {
				TilePool pool = (TilePool) tileEntity;
				if (pool.getCurrentMana() >= cost) {
					pool.recieveMana(-cost);
					return true;
				}
			}
		}
		if (devices.isEmpty()) {
			return false;
		}
		ManaNetworkData network = ManaNetworkData.get(world);
		if (network == null) {
			return false;
		}
		for (BlockPos device : devices) {
			if (network.buffer(world, device) >= cost) {
				network.withdraw(world, device, cost);
				return true;
			}
		}
		return false;
	}

	// ---- mana storage (filled by the Mana Well / Mana Anchor like a pool) ----

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

	@Override
	public int getCurrentMana() {
		return mana;
	}

	private static int capacity() {
		return Math.max(0, BotaniaAdditionConfig.gaiaBeaconCapacity());
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound tag) {
		super.writeToNBT(tag);
		tag.setInteger(TAG_MANA, mana);
		return tag;
	}

	@Override
	public void readFromNBT(NBTTagCompound tag) {
		super.readFromNBT(tag);
		mana = Math.max(0, tag.getInteger(TAG_MANA));
	}
}
