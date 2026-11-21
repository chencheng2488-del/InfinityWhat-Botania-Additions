package com.bandybus.infwhat.botania.block.tile;

import com.bandybus.infwhat.botania.config.BotaniaAdditionConfig;
import com.bandybus.infwhat.botania.device.AcceleratorState;
import com.bandybus.infwhat.botania.device.AcceleratorTarget;
import com.bandybus.infwhat.botania.device.DeviceRanges;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

/**
 * Common logic for the three ranged accelerators.
 *
 * <p>The block periodically scans a box around itself for a concrete Botania tile entity and
 * then acts on it once per tick. The target list is a cache; each tick re-checks that the target
 * still exists and is still the expected type, so breaking the target cannot leave a stale entry.
 *
 * <p>Rate, reach, offset and the range-visibility flag are stored per block. A reach of
 * {@code -1} means "use the config default on that axis"; the GUI always writes concrete values
 * after the player applies them.
 */
public abstract class AbstractAcceleratorTileEntity extends TileEntity implements ITickable {

	private static final String TAG_ENABLED = "Enabled";
	private static final String TAG_RATE = "Rate";
	private static final String TAG_REACH_X = "ReachX";
	private static final String TAG_REACH_Y = "ReachY";
	private static final String TAG_REACH_Z = "ReachZ";
	private static final String TAG_OFFSET_X = "OffsetX";
	private static final String TAG_OFFSET_Y = "OffsetY";
	private static final String TAG_OFFSET_Z = "OffsetZ";
	private static final String TAG_SHOW_RANGE = "ShowRange";
	private static final String TAG_COOLDOWN = "CooldownMultiplier";
	private static final String TAG_LEGACY_RADIUS = "Radius";
	private static final int SCAN_INTERVAL = 20;
	private static final int MAX_TARGETS = 64;

	private boolean enabled = true;
	private int rate = -1;
	private int cooldownMultiplier = -1;
	private int reachX = -1;
	private int reachY = -1;
	private int reachZ = -1;
	private int offsetX;
	private int offsetY;
	private int offsetZ;
	private boolean showRange = true;
	private int scanCooldown;
	private final List<BlockPos> targets = new ArrayList<BlockPos>();

	protected AbstractAcceleratorTileEntity() {
	}

	protected abstract boolean featureEnabled();

	protected abstract boolean accepts(TileEntity target);

	protected abstract void accelerate(TileEntity target);

	protected abstract int defaultRate();

	protected abstract int minRate();

	protected abstract int maxRate();

	protected abstract int defaultRadius();

	/** Cooldown multiplier used while the per-block value is unset. */
	protected int defaultCooldownMultiplier() {
		return 1;
	}

	@Override
	public void update() {
		if (world == null || world.isRemote) {
			return;
		}
		tickAccelerator();
	}

	public int effectiveCooldownMultiplier() {
		int fallback = defaultCooldownMultiplier();
		int value = cooldownMultiplier < 0 ? fallback : cooldownMultiplier;
		return MathHelper.clamp(value, 1, Integer.MAX_VALUE);
	}

	public void setCooldownMultiplier(int value) {
		cooldownMultiplier = MathHelper.clamp(value, 1, Integer.MAX_VALUE);
		markDirty();
	}

	public int effectiveRate() {
		int fallback = defaultRate();
		int value = rate < 0 ? fallback : rate;
		return MathHelper.clamp(value, minRate(), maxRate());
	}

	public int reachX() {
		return MathHelper.clamp(reachX < 0 ? defaultRadius() : reachX, 0, maxReach());
	}

	public int reachY() {
		return MathHelper.clamp(reachY < 0 ? defaultRadius() : reachY, 0, maxReach());
	}

	public int reachZ() {
		return MathHelper.clamp(reachZ < 0 ? defaultRadius() : reachZ, 0, maxReach());
	}

	public int offsetX() {
		return MathHelper.clamp(offsetX, -maxOffset(), maxOffset());
	}

	public int offsetY() {
		return MathHelper.clamp(offsetY, -maxOffset(), maxOffset());
	}

	public int offsetZ() {
		return MathHelper.clamp(offsetZ, -maxOffset(), maxOffset());
	}

	public boolean showRange() {
		return showRange;
	}

	public void setRate(int value) {
		rate = MathHelper.clamp(value, minRate(), maxRate());
		markDirty();
	}

	public void setRange(int reachX, int reachY, int reachZ,
						 int offsetX, int offsetY, int offsetZ, boolean showRange) {
		this.reachX = MathHelper.clamp(reachX, 0, maxReach());
		this.reachY = MathHelper.clamp(reachY, 0, maxReach());
		this.reachZ = MathHelper.clamp(reachZ, 0, maxReach());
		this.offsetX = MathHelper.clamp(offsetX, -maxOffset(), maxOffset());
		this.offsetY = MathHelper.clamp(offsetY, -maxOffset(), maxOffset());
		this.offsetZ = MathHelper.clamp(offsetZ, -maxOffset(), maxOffset());
		this.showRange = showRange;
		markDirty();
	}

	private static int maxReach() {
		return Math.min(DeviceRanges.MAX_REACH, BotaniaAdditionConfig.acceleratorMaxReach());
	}

	private static int maxOffset() {
		return Math.min(DeviceRanges.MAX_OFFSET, BotaniaAdditionConfig.acceleratorMaxOffset());
	}

	protected void tickAccelerator() {
		if (!enabled || !featureEnabled()) {
			return;
		}
		if (scanCooldown-- <= 0) {
			scan();
			scanCooldown = SCAN_INTERVAL;
		}
		for (BlockPos target : targets) {
			if (!world.isBlockLoaded(target)) {
				continue;
			}
			TileEntity tileEntity = world.getTileEntity(target);
			if (tileEntity != null && accepts(tileEntity)) {
				accelerate(tileEntity);
			}
		}
	}

	private void scan() {
		targets.clear();
		for (BlockPos candidate : DeviceRanges.box(pos, reachX(), reachY(), reachZ(),
				offsetX(), offsetY(), offsetZ())) {
			if (candidate.equals(pos) || !world.isBlockLoaded(candidate)) {
				continue;
			}
			TileEntity tileEntity = world.getTileEntity(candidate);
			if (tileEntity != null && accepts(tileEntity)) {
				targets.add(candidate.toImmutable());
			}
		}
	}

	/** Snapshot for the accelerator GUI. */
	public AcceleratorState createState(World worldIn) {
		List<AcceleratorTarget> visibleTargets = new ArrayList<AcceleratorTarget>();
		for (BlockPos target : targets) {
			if (!worldIn.isBlockLoaded(target)) {
				continue;
			}
			visibleTargets.add(new AcceleratorTarget(target,
					com.bandybus.infwhat.botania.util.BlockLabels.of(worldIn, target)));
			if (visibleTargets.size() >= MAX_TARGETS) {
				break;
			}
		}
		return new AcceleratorState(enabled, featureEnabled(), effectiveRate(),
				effectiveCooldownMultiplier(), reachX(), reachY(), reachZ(),
				offsetX(), offsetY(), offsetZ(), showRange, visibleTargets);
	}

	public boolean isEnabled() {
		return enabled;
	}

	public boolean isFeatureEnabled() {
		return featureEnabled();
	}

	public boolean toggleEnabled() {
		enabled = !enabled;
		markDirty();
		return enabled;
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound tag) {
		super.writeToNBT(tag);
		tag.setBoolean(TAG_ENABLED, enabled);
		tag.setInteger(TAG_RATE, rate);
		tag.setInteger(TAG_REACH_X, reachX);
		tag.setInteger(TAG_REACH_Y, reachY);
		tag.setInteger(TAG_REACH_Z, reachZ);
		tag.setInteger(TAG_OFFSET_X, offsetX);
		tag.setInteger(TAG_OFFSET_Y, offsetY);
		tag.setInteger(TAG_OFFSET_Z, offsetZ);
		tag.setBoolean(TAG_SHOW_RANGE, showRange);
		tag.setInteger(TAG_COOLDOWN, cooldownMultiplier);
		return tag;
	}

	@Override
	public void readFromNBT(NBTTagCompound tag) {
		super.readFromNBT(tag);
		enabled = !tag.hasKey(TAG_ENABLED) || tag.getBoolean(TAG_ENABLED);
		int storedRate = tag.hasKey(TAG_RATE) ? tag.getInteger(TAG_RATE) : -1;
		rate = storedRate < 0 ? -1 : MathHelper.clamp(storedRate, minRate(), maxRate());
		int legacyRadius = tag.hasKey(TAG_LEGACY_RADIUS)
				? tag.getInteger(TAG_LEGACY_RADIUS) : -1;
		reachX = tag.hasKey(TAG_REACH_X) ? tag.getInteger(TAG_REACH_X) : legacyRadius;
		reachY = tag.hasKey(TAG_REACH_Y) ? tag.getInteger(TAG_REACH_Y) : legacyRadius;
		reachZ = tag.hasKey(TAG_REACH_Z) ? tag.getInteger(TAG_REACH_Z) : legacyRadius;
		offsetX = tag.hasKey(TAG_OFFSET_X) ? tag.getInteger(TAG_OFFSET_X) : 0;
		offsetY = tag.hasKey(TAG_OFFSET_Y) ? tag.getInteger(TAG_OFFSET_Y) : 0;
		offsetZ = tag.hasKey(TAG_OFFSET_Z) ? tag.getInteger(TAG_OFFSET_Z) : 0;
		showRange = !tag.hasKey(TAG_SHOW_RANGE) || tag.getBoolean(TAG_SHOW_RANGE);
		cooldownMultiplier = tag.hasKey(TAG_COOLDOWN) ? tag.getInteger(TAG_COOLDOWN) : -1;
	}

	@Override
	public NBTTagCompound getUpdateTag() {
		return writeToNBT(new NBTTagCompound());
	}

	@Override
	public SPacketUpdateTileEntity getUpdatePacket() {
		return new SPacketUpdateTileEntity(pos, 3, getUpdateTag());
	}

	@Override
	public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity packet) {
		readFromNBT(packet.getNbtCompound());
	}

	@Override
	public AxisAlignedBB getRenderBoundingBox() {
		int radius = Math.max(1, Math.max(reachX(), reachZ()) + 1);
		return new AxisAlignedBB(pos).grow(radius, reachY() + 1, radius);
	}
}
