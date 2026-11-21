package com.bandybus.infwhat.botania.network;

import com.bandybus.infwhat.botania.InfinityWhatBotania;
import com.bandybus.infwhat.botania.block.tile.AbstractAcceleratorTileEntity;
import com.bandybus.infwhat.botania.block.tile.AbstractManaEndpointTileEntity;
import com.bandybus.infwhat.botania.block.tile.ManaAnchorTileEntity;
import com.bandybus.infwhat.botania.block.tile.ManaWellTileEntity;
import com.bandybus.infwhat.botania.device.AcceleratorState;
import com.bandybus.infwhat.botania.device.AcceleratorTarget;
import com.bandybus.infwhat.botania.device.DeviceState;
import com.bandybus.infwhat.botania.device.LinkEntry;
import com.bandybus.infwhat.botania.device.ManaDeviceHost;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;
import vazkii.botania.api.internal.VanillaPacketDispatcher;

import java.util.ArrayList;
import java.util.List;

/** Packets for the device GUI plus the server-side handlers. */
public final class ModNetwork {

	public static final SimpleNetworkWrapper INSTANCE =
			NetworkRegistry.INSTANCE.newSimpleChannel(InfinityWhatBotania.MOD_ID);

	private static int nextId;

	public static void register() {
		INSTANCE.registerMessage(new ServerHandler<RequestState>() {
			@Override
			void handle(RequestState message, EntityPlayerMP player) {
				message.handle(player);
			}
		}, RequestState.class, nextId++, Side.SERVER);
		INSTANCE.registerMessage(new ServerHandler<SetRate>() {
			@Override
			void handle(SetRate message, EntityPlayerMP player) {
				message.handle(player);
			}
		}, SetRate.class, nextId++, Side.SERVER);
		INSTANCE.registerMessage(new ServerHandler<SetRange>() {
			@Override
			void handle(SetRange message, EntityPlayerMP player) {
				message.handle(player);
			}
		}, SetRange.class, nextId++, Side.SERVER);
		INSTANCE.registerMessage(new ServerHandler<ClearExcluded>() {
			@Override
			void handle(ClearExcluded message, EntityPlayerMP player) {
				message.handle(player);
			}
		}, ClearExcluded.class, nextId++, Side.SERVER);
		INSTANCE.registerMessage(new ServerHandler<ToggleEnabled>() {
			@Override
			void handle(ToggleEnabled message, EntityPlayerMP player) {
				message.handle(player);
			}
		}, ToggleEnabled.class, nextId++, Side.SERVER);
		INSTANCE.registerMessage(new ServerHandler<AcceleratorRequestState>() {
			@Override
			void handle(AcceleratorRequestState message, EntityPlayerMP player) {
				message.handle(player);
			}
		}, AcceleratorRequestState.class, nextId++, Side.SERVER);
		INSTANCE.registerMessage(new ServerHandler<AcceleratorToggleEnabled>() {
			@Override
			void handle(AcceleratorToggleEnabled message, EntityPlayerMP player) {
				message.handle(player);
			}
		}, AcceleratorToggleEnabled.class, nextId++, Side.SERVER);
		INSTANCE.registerMessage(new ServerHandler<AcceleratorSetSettings>() {
			@Override
			void handle(AcceleratorSetSettings message, EntityPlayerMP player) {
				message.handle(player);
			}
		}, AcceleratorSetSettings.class, nextId++, Side.SERVER);

		INSTANCE.registerMessage(new IMessageHandler<StateMessage, IMessage>() {
			@Override
			public IMessage onMessage(StateMessage message, MessageContext ctx) {
				InfinityWhatBotania.proxy.handleDeviceState(message);
				return null;
			}
		}, StateMessage.class, nextId++, Side.CLIENT);
		INSTANCE.registerMessage(new IMessageHandler<AcceleratorStateMessage, IMessage>() {
			@Override
			public IMessage onMessage(AcceleratorStateMessage message, MessageContext ctx) {
				InfinityWhatBotania.proxy.handleAcceleratorState(message);
				return null;
			}
		}, AcceleratorStateMessage.class, nextId++, Side.CLIENT);
	}

	/** Base for the server-bound packets: decodes, hops to the server thread, then handles. */
	private abstract static class ServerHandler<T extends IMessage> implements IMessageHandler<T, IMessage> {

		abstract void handle(T message, EntityPlayerMP player);

		@Override
		public IMessage onMessage(final T message, MessageContext ctx) {
			final EntityPlayerMP player = ctx.getServerHandler().player;
			player.getServerWorld().addScheduledTask(new Runnable() {
				@Override
				public void run() {
					handle(message, player);
				}
			});
			return null;
		}
	}

	private static BlockPos readPos(ByteBuf buffer) {
		return new BlockPos(buffer.readInt(), buffer.readInt(), buffer.readInt());
	}

	private static void writePos(ByteBuf buffer, BlockPos pos) {
		buffer.writeInt(pos.getX());
		buffer.writeInt(pos.getY());
		buffer.writeInt(pos.getZ());
	}

	private static String readString(ByteBuf buffer) {
		return ByteBufUtils.readUTF8String(buffer);
	}

	private static void writeString(ByteBuf buffer, String value) {
		String text = value == null ? "" : value;
		if (text.length() > 256) {
			text = text.substring(0, 256);
		}
		ByteBufUtils.writeUTF8String(buffer, text);
	}

	private static TileEntity deviceAt(EntityPlayerMP player, BlockPos pos) {
		if (player.getDistanceSq(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) > 64.0D) {
			return null;
		}
		TileEntity tileEntity = player.world.getTileEntity(pos);
		if (!(tileEntity instanceof ManaDeviceHost)) {
			return null;
		}
		if (tileEntity instanceof AbstractManaEndpointTileEntity) {
			AbstractManaEndpointTileEntity endpoint =
					(AbstractManaEndpointTileEntity) tileEntity;
			if (endpoint.getOwnerId() != null
					&& !endpoint.isOwnedBy(player.getUniqueID())) {
				return null;
			}
		}
		return tileEntity;
	}

	private static void sendState(EntityPlayerMP player, BlockPos pos, ManaDeviceHost host) {
		INSTANCE.sendTo(new StateMessage(pos, host.createState(player.world)), player);
	}

	private static AbstractAcceleratorTileEntity acceleratorAt(EntityPlayerMP player, BlockPos pos) {
		if (player.getDistanceSq(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) > 64.0D) {
			return null;
		}
		TileEntity tileEntity = player.world.getTileEntity(pos);
		return tileEntity instanceof AbstractAcceleratorTileEntity
				? (AbstractAcceleratorTileEntity) tileEntity : null;
	}

	private static void sendAcceleratorState(EntityPlayerMP player, BlockPos pos,
											 AbstractAcceleratorTileEntity accelerator) {
		INSTANCE.sendTo(new AcceleratorStateMessage(pos,
				accelerator.createState(player.world)), player);
	}

	public static final class RequestState implements IMessage {

		private BlockPos pos;

		public RequestState() {
		}

		public RequestState(BlockPos pos) {
			this.pos = pos;
		}

		@Override
		public void fromBytes(ByteBuf buffer) {
			pos = readPos(buffer);
		}

		@Override
		public void toBytes(ByteBuf buffer) {
			writePos(buffer, pos);
		}

		void handle(EntityPlayerMP player) {
			TileEntity tileEntity = deviceAt(player, pos);
			if (tileEntity instanceof ManaDeviceHost) {
				sendState(player, pos, (ManaDeviceHost) tileEntity);
			}
		}
	}

	public static final class SetRate implements IMessage {

		private BlockPos pos;
		private int rate;

		public SetRate() {
		}

		public SetRate(BlockPos pos, int rate) {
			this.pos = pos;
			this.rate = rate;
		}

		@Override
		public void fromBytes(ByteBuf buffer) {
			pos = readPos(buffer);
			rate = buffer.readInt();
		}

		@Override
		public void toBytes(ByteBuf buffer) {
			writePos(buffer, pos);
			buffer.writeInt(rate);
		}

		void handle(EntityPlayerMP player) {
			TileEntity tileEntity = deviceAt(player, pos);
			if (tileEntity instanceof ManaDeviceHost) {
				ManaDeviceHost host = (ManaDeviceHost) tileEntity;
				host.settings().setRate(Math.max(0, rate));
				tileEntity.markDirty();
				sendState(player, pos, host);
			}
		}
	}

	public static final class SetRange implements IMessage {

		private BlockPos pos;
		private int reachX;
		private int reachY;
		private int reachZ;
		private int offsetX;
		private int offsetY;
		private int offsetZ;
		private boolean showRange;

		public SetRange() {
		}

		public SetRange(BlockPos pos, int reachX, int reachY, int reachZ,
						int offsetX, int offsetY, int offsetZ, boolean showRange) {
			this.pos = pos;
			this.reachX = reachX;
			this.reachY = reachY;
			this.reachZ = reachZ;
			this.offsetX = offsetX;
			this.offsetY = offsetY;
			this.offsetZ = offsetZ;
			this.showRange = showRange;
		}

		@Override
		public void fromBytes(ByteBuf buffer) {
			pos = readPos(buffer);
			reachX = buffer.readInt();
			reachY = buffer.readInt();
			reachZ = buffer.readInt();
			offsetX = buffer.readInt();
			offsetY = buffer.readInt();
			offsetZ = buffer.readInt();
			showRange = buffer.readBoolean();
		}

		@Override
		public void toBytes(ByteBuf buffer) {
			writePos(buffer, pos);
			buffer.writeInt(reachX);
			buffer.writeInt(reachY);
			buffer.writeInt(reachZ);
			buffer.writeInt(offsetX);
			buffer.writeInt(offsetY);
			buffer.writeInt(offsetZ);
			buffer.writeBoolean(showRange);
		}

		void handle(EntityPlayerMP player) {
			TileEntity tileEntity = deviceAt(player, pos);
			if ((tileEntity instanceof ManaWellTileEntity
					|| tileEntity instanceof ManaAnchorTileEntity)
					&& tileEntity instanceof ManaDeviceHost) {
				ManaDeviceHost host = (ManaDeviceHost) tileEntity;
				host.settings().setRange(reachX, reachY, reachZ,
						offsetX, offsetY, offsetZ);
				host.settings().setShowRange(showRange);
				tileEntity.markDirty();
				VanillaPacketDispatcher.dispatchTEToNearbyPlayers(player.world, pos);
				sendState(player, pos, host);
			}
		}
	}

	public static final class ClearExcluded implements IMessage {

		private BlockPos pos;

		public ClearExcluded() {
		}

		public ClearExcluded(BlockPos pos) {
			this.pos = pos;
		}

		@Override
		public void fromBytes(ByteBuf buffer) {
			pos = readPos(buffer);
		}

		@Override
		public void toBytes(ByteBuf buffer) {
			writePos(buffer, pos);
		}

		void handle(EntityPlayerMP player) {
			TileEntity tileEntity = deviceAt(player, pos);
			if ((tileEntity instanceof ManaWellTileEntity
					|| tileEntity instanceof ManaAnchorTileEntity)
					&& tileEntity instanceof ManaDeviceHost) {
				ManaDeviceHost host = (ManaDeviceHost) tileEntity;
				host.settings().clearExcluded();
				tileEntity.markDirty();
				sendState(player, pos, host);
			}
		}
	}

	public static final class ToggleEnabled implements IMessage {

		private BlockPos pos;

		public ToggleEnabled() {
		}

		public ToggleEnabled(BlockPos pos) {
			this.pos = pos;
		}

		@Override
		public void fromBytes(ByteBuf buffer) {
			pos = readPos(buffer);
		}

		@Override
		public void toBytes(ByteBuf buffer) {
			writePos(buffer, pos);
		}

		void handle(EntityPlayerMP player) {
			TileEntity tileEntity = deviceAt(player, pos);
			if (tileEntity instanceof ManaDeviceHost) {
				ManaDeviceHost host = (ManaDeviceHost) tileEntity;
				host.setMasterEnabled(!host.isMasterEnabled());
				tileEntity.markDirty();
				sendState(player, pos, host);
			}
		}
	}

	public static final class StateMessage implements IMessage {

		private BlockPos pos;
		private DeviceState state;

		public StateMessage() {
		}

		public StateMessage(BlockPos pos, DeviceState state) {
			this.pos = pos;
			this.state = state;
		}

		@Override
		public void fromBytes(ByteBuf buffer) {
			pos = readPos(buffer);
			int rate = buffer.readInt();
			int effectiveRate = buffer.readInt();
			boolean enabled = buffer.readBoolean();
			long bufferMana = buffer.readLong();
			long bufferMax = buffer.readLong();
			String networkName = readString(buffer);
			int reachX = buffer.readInt();
			int reachY = buffer.readInt();
			int reachZ = buffer.readInt();
			int offsetX = buffer.readInt();
			int offsetY = buffer.readInt();
			int offsetZ = buffer.readInt();
			boolean showRange = buffer.readBoolean();
			int count = buffer.readInt();
			List<LinkEntry> links = new ArrayList<LinkEntry>(Math.max(0, count));
			for (int index = 0; index < count; index++) {
				BlockPos linkPos = readPos(buffer);
				String label = readString(buffer);
				int direction = buffer.readByte();
				boolean auto = buffer.readBoolean();
				links.add(new LinkEntry(linkPos, label, direction, auto));
			}
			state = new DeviceState(rate, effectiveRate, enabled, bufferMana, bufferMax,
					networkName, reachX, reachY, reachZ, offsetX, offsetY, offsetZ,
					showRange, links);
		}

		@Override
		public void toBytes(ByteBuf buffer) {
			writePos(buffer, pos);
			buffer.writeInt(state.rate());
			buffer.writeInt(state.effectiveRate());
			buffer.writeBoolean(state.enabled());
			buffer.writeLong(state.buffer());
			buffer.writeLong(state.bufferMax());
			writeString(buffer, state.networkName());
			buffer.writeInt(state.reachX());
			buffer.writeInt(state.reachY());
			buffer.writeInt(state.reachZ());
			buffer.writeInt(state.offsetX());
			buffer.writeInt(state.offsetY());
			buffer.writeInt(state.offsetZ());
			buffer.writeBoolean(state.showRange());
			buffer.writeInt(state.links().size());
			for (LinkEntry entry : state.links()) {
				writePos(buffer, entry.pos());
				writeString(buffer, entry.label());
				buffer.writeByte(entry.direction());
				buffer.writeBoolean(entry.auto());
			}
		}

		public BlockPos pos() {
			return pos;
		}

		public DeviceState state() {
			return state;
		}
	}

	public static final class AcceleratorRequestState implements IMessage {

		private BlockPos pos;

		public AcceleratorRequestState() {
		}

		public AcceleratorRequestState(BlockPos pos) {
			this.pos = pos;
		}

		@Override
		public void fromBytes(ByteBuf buffer) {
			pos = readPos(buffer);
		}

		@Override
		public void toBytes(ByteBuf buffer) {
			writePos(buffer, pos);
		}

		void handle(EntityPlayerMP player) {
			AbstractAcceleratorTileEntity accelerator = acceleratorAt(player, pos);
			if (accelerator != null) {
				sendAcceleratorState(player, pos, accelerator);
			}
		}
	}

	public static final class AcceleratorToggleEnabled implements IMessage {

		private BlockPos pos;

		public AcceleratorToggleEnabled() {
		}

		public AcceleratorToggleEnabled(BlockPos pos) {
			this.pos = pos;
		}

		@Override
		public void fromBytes(ByteBuf buffer) {
			pos = readPos(buffer);
		}

		@Override
		public void toBytes(ByteBuf buffer) {
			writePos(buffer, pos);
		}

		void handle(EntityPlayerMP player) {
			AbstractAcceleratorTileEntity accelerator = acceleratorAt(player, pos);
			if (accelerator != null) {
				accelerator.toggleEnabled();
				sendAcceleratorState(player, pos, accelerator);
			}
		}
	}

	public static final class AcceleratorSetSettings implements IMessage {

		private BlockPos pos;
		private int rate;
		private int cooldown;
		private int reachX;
		private int reachY;
		private int reachZ;
		private int offsetX;
		private int offsetY;
		private int offsetZ;
		private boolean showRange;

		public AcceleratorSetSettings() {
		}

		public AcceleratorSetSettings(BlockPos pos, int rate, int cooldown,
									  int reachX, int reachY, int reachZ,
									  int offsetX, int offsetY, int offsetZ,
									  boolean showRange) {
			this.pos = pos;
			this.rate = rate;
			this.cooldown = cooldown;
			this.reachX = reachX;
			this.reachY = reachY;
			this.reachZ = reachZ;
			this.offsetX = offsetX;
			this.offsetY = offsetY;
			this.offsetZ = offsetZ;
			this.showRange = showRange;
		}

		@Override
		public void fromBytes(ByteBuf buffer) {
			pos = readPos(buffer);
			rate = buffer.readInt();
			cooldown = buffer.readInt();
			reachX = buffer.readInt();
			reachY = buffer.readInt();
			reachZ = buffer.readInt();
			offsetX = buffer.readInt();
			offsetY = buffer.readInt();
			offsetZ = buffer.readInt();
			showRange = buffer.readBoolean();
		}

		@Override
		public void toBytes(ByteBuf buffer) {
			writePos(buffer, pos);
			buffer.writeInt(rate);
			buffer.writeInt(cooldown);
			buffer.writeInt(reachX);
			buffer.writeInt(reachY);
			buffer.writeInt(reachZ);
			buffer.writeInt(offsetX);
			buffer.writeInt(offsetY);
			buffer.writeInt(offsetZ);
			buffer.writeBoolean(showRange);
		}

		void handle(EntityPlayerMP player) {
			AbstractAcceleratorTileEntity accelerator = acceleratorAt(player, pos);
			if (accelerator != null) {
				accelerator.setRate(rate);
				accelerator.setRange(reachX, reachY, reachZ,
						offsetX, offsetY, offsetZ, showRange);
				accelerator.setCooldownMultiplier(cooldown);
				VanillaPacketDispatcher.dispatchTEToNearbyPlayers(player.world, pos);
				sendAcceleratorState(player, pos, accelerator);
			}
		}
	}

	public static final class AcceleratorStateMessage implements IMessage {

		private BlockPos pos;
		private AcceleratorState state;

		public AcceleratorStateMessage() {
		}

		public AcceleratorStateMessage(BlockPos pos, AcceleratorState state) {
			this.pos = pos;
			this.state = state;
		}

		@Override
		public void fromBytes(ByteBuf buffer) {
			pos = readPos(buffer);
			boolean enabled = buffer.readBoolean();
			boolean featureEnabled = buffer.readBoolean();
			int rate = buffer.readInt();
			int cooldown = buffer.readInt();
			int reachX = buffer.readInt();
			int reachY = buffer.readInt();
			int reachZ = buffer.readInt();
			int offsetX = buffer.readInt();
			int offsetY = buffer.readInt();
			int offsetZ = buffer.readInt();
			boolean showRange = buffer.readBoolean();
			int count = buffer.readInt();
			List<AcceleratorTarget> targets = new ArrayList<AcceleratorTarget>(Math.max(0, count));
			for (int index = 0; index < count; index++) {
				BlockPos targetPos = readPos(buffer);
				String label = readString(buffer);
				targets.add(new AcceleratorTarget(targetPos, label));
			}
			state = new AcceleratorState(enabled, featureEnabled, rate, cooldown,
					reachX, reachY, reachZ, offsetX, offsetY, offsetZ, showRange, targets);
		}

		@Override
		public void toBytes(ByteBuf buffer) {
			writePos(buffer, pos);
			buffer.writeBoolean(state.enabled());
			buffer.writeBoolean(state.featureEnabled());
			buffer.writeInt(state.rate());
			buffer.writeInt(state.cooldown());
			buffer.writeInt(state.reachX());
			buffer.writeInt(state.reachY());
			buffer.writeInt(state.reachZ());
			buffer.writeInt(state.offsetX());
			buffer.writeInt(state.offsetY());
			buffer.writeInt(state.offsetZ());
			buffer.writeBoolean(state.showRange());
			buffer.writeInt(state.targets().size());
			for (AcceleratorTarget target : state.targets()) {
				writePos(buffer, target.pos());
				writeString(buffer, target.label());
			}
		}

		public BlockPos pos() {
			return pos;
		}

		public AcceleratorState state() {
			return state;
		}
	}

	private ModNetwork() {
	}
}
