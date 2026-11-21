package com.bandybus.infwhat.botania.block.tile;

import com.bandybus.infwhat.botania.config.BotaniaAdditionConfig;
import com.bandybus.infwhat.botania.device.ManaDeviceHost;
import com.bandybus.infwhat.botania.device.ManaDeviceSettings;
import com.bandybus.infwhat.botania.grid.ManaNetworkData;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Shared state for the two wireless endpoints.
 *
 * <p>There is no owner-visible channel name. The owner UUID is only used to decide who may open
 * the GUI; the network itself is derived from explicit links.
 */
public abstract class AbstractManaEndpointTileEntity extends TileEntity implements ManaDeviceHost {

	private static final String TAG_OWNER = "Owner";
	private static final String TAG_ENABLED = "Enabled";
	private static final String TAG_AUTO_TARGETS = "AutoTargets";

	private UUID owner;
	private boolean enabled = true;
	private final ManaDeviceSettings settings = new ManaDeviceSettings();
	private final List<BlockPos> clientLinks = new ArrayList<BlockPos>();
	private final List<BlockPos> autoTargets = new ArrayList<BlockPos>();

	protected AbstractManaEndpointTileEntity() {
	}

	public UUID getOwnerId() {
		return owner;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setOwner(UUID owner) {
		if (this.owner == null && owner != null) {
			this.owner = owner;
			markDirty();
		}
	}

	public boolean isOwnedBy(UUID playerId) {
		return owner != null && owner.equals(playerId);
	}

	public boolean toggleEnabled() {
		enabled = !enabled;
		markDirty();
		return enabled;
	}

	@Override
	public ManaDeviceSettings settings() {
		return settings;
	}

	@Override
	public boolean isMasterEnabled() {
		return enabled;
	}

	@Override
	public void setMasterEnabled(boolean value) {
		if (enabled != value) {
			enabled = value;
			markDirty();
		}
	}

	@Override
	public List<BlockPos> clientLinks() {
		return clientLinks;
	}

	@Override
	public List<BlockPos> clientAutoTargets() {
		return autoTargets;
	}

	/** Server-side scan cache of nearby mana pools; also mirrored to the client through NBT. */
	protected List<BlockPos> autoTargets() {
		return autoTargets;
	}

	@Override
	public int reachX() {
		return 0;
	}

	@Override
	public int reachY() {
		return 0;
	}

	@Override
	public int reachZ() {
		return 0;
	}

	@Override
	public int offsetX() {
		return 0;
	}

	@Override
	public int offsetY() {
		return 0;
	}

	@Override
	public int offsetZ() {
		return 0;
	}

	@Override
	public boolean showRange() {
		return false;
	}

	/** Full runtime gate for a wireless endpoint. */
	public boolean canOperate(World world) {
		return owner != null
				&& BotaniaAdditionConfig.wirelessEnabled()
				&& BotaniaAdditionConfig.canOperate(world);
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound tag) {
		super.writeToNBT(tag);
		tag.setBoolean(TAG_ENABLED, enabled);
		settings.save(tag);
		if (owner != null) {
			tag.setString(TAG_OWNER + "Most", Long.toString(owner.getMostSignificantBits()));
			tag.setString(TAG_OWNER + "Least", Long.toString(owner.getLeastSignificantBits()));
		}
		if (world != null && !world.isRemote) {
			ManaNetworkData.writeLinks(tag, world, pos);
		}
		ManaNetworkData.writePositions(tag, TAG_AUTO_TARGETS, autoTargets);
		return tag;
	}

	@Override
	public void readFromNBT(NBTTagCompound tag) {
		super.readFromNBT(tag);
		enabled = !tag.hasKey(TAG_ENABLED) || tag.getBoolean(TAG_ENABLED);
		settings.load(tag);
		owner = readOwner(tag);
		clientLinks.clear();
		clientLinks.addAll(ManaNetworkData.readLinks(tag));
		autoTargets.clear();
		autoTargets.addAll(ManaNetworkData.readPositions(tag, TAG_AUTO_TARGETS));
	}

	private static UUID readOwner(NBTTagCompound tag) {
		if (!tag.hasKey(TAG_OWNER + "Most") || !tag.hasKey(TAG_OWNER + "Least")) {
			return null;
		}
		try {
			return new UUID(Long.parseLong(tag.getString(TAG_OWNER + "Most")),
					Long.parseLong(tag.getString(TAG_OWNER + "Least")));
		} catch (NumberFormatException exception) {
			return null;
		}
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
		return new AxisAlignedBB(pos).grow(1.0D);
	}
}
