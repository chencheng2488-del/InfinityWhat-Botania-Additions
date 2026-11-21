package com.bandybus.infwhat.botania.network;

import com.bandybus.infwhat.botania.InfinityWhatBotania;
import com.bandybus.infwhat.botania.block.entity.AbstractAcceleratorTileEntity;
import com.bandybus.infwhat.botania.block.entity.AbstractManaEndpointTileEntity;
import com.bandybus.infwhat.botania.block.entity.ManaAnchorTileEntity;
import com.bandybus.infwhat.botania.block.entity.ManaWellTileEntity;
import com.bandybus.infwhat.botania.client.ClientStateReceiver;
import com.bandybus.infwhat.botania.container.AcceleratorContainer;
import com.bandybus.infwhat.botania.container.ManaDeviceContainer;
import com.bandybus.infwhat.botania.device.AcceleratorState;
import com.bandybus.infwhat.botania.device.AcceleratorTarget;
import com.bandybus.infwhat.botania.device.DeviceState;
import com.bandybus.infwhat.botania.device.LinkEntry;
import com.bandybus.infwhat.botania.device.ManaDeviceHost;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.PacketDistributor;
import net.minecraftforge.fml.network.simple.SimpleChannel;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Packets for the device GUI plus the server-side handlers. */
public final class ModNetwork {

    private static final String PROTOCOL = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(InfinityWhatBotania.MOD_ID, "main"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    private ModNetwork() {
    }

    public static void register() {
        int id = 0;
        CHANNEL.registerMessage(id++, RequestState.class,
                RequestState::encode, RequestState::new, RequestState::handle);
        CHANNEL.registerMessage(id++, SetRate.class,
                SetRate::encode, SetRate::new, SetRate::handle);
        CHANNEL.registerMessage(id++, SetRange.class,
                SetRange::encode, SetRange::new, SetRange::handle);
        CHANNEL.registerMessage(id++, ClearExcluded.class,
                ClearExcluded::encode, ClearExcluded::new, ClearExcluded::handle);
        CHANNEL.registerMessage(id++, ToggleEnabled.class,
                ToggleEnabled::encode, ToggleEnabled::new, ToggleEnabled::handle);
        CHANNEL.registerMessage(id++, StateMessage.class,
                StateMessage::encode, StateMessage::new, StateMessage::handle);
        CHANNEL.registerMessage(id++, AcceleratorRequestState.class,
                AcceleratorRequestState::encode, AcceleratorRequestState::new,
                AcceleratorRequestState::handle);
        CHANNEL.registerMessage(id++, AcceleratorToggleEnabled.class,
                AcceleratorToggleEnabled::encode, AcceleratorToggleEnabled::new,
                AcceleratorToggleEnabled::handle);
        CHANNEL.registerMessage(id++, AcceleratorSetSettings.class,
                AcceleratorSetSettings::encode, AcceleratorSetSettings::new,
                AcceleratorSetSettings::handle);
        CHANNEL.registerMessage(id++, AcceleratorStateMessage.class,
                AcceleratorStateMessage::encode, AcceleratorStateMessage::new,
                AcceleratorStateMessage::handle);
    }

    @Nullable
    private static TileEntity deviceAt(ServerPlayerEntity player, BlockPos pos) {
        if (player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) > 64.0D) {
            return null;
        }
        TileEntity tileEntity = player.level.getBlockEntity(pos);
        if (!(tileEntity instanceof ManaDeviceHost)) {
            return null;
        }
        if (tileEntity instanceof AbstractManaEndpointTileEntity) {
            AbstractManaEndpointTileEntity endpoint = (AbstractManaEndpointTileEntity) tileEntity;
            if (endpoint.getOwnerId() != null && !endpoint.isOwnedBy(player.getUUID())) {
                return null;
            }
        }
        return tileEntity;
    }

    private static void sendState(ServerPlayerEntity player, BlockPos pos, ManaDeviceHost host) {
        if (player.level instanceof net.minecraft.world.server.ServerWorld) {
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                    new StateMessage(pos, host.createState(
                            (net.minecraft.world.server.ServerWorld) player.level)));
        }
    }

    @Nullable
    private static TileEntity acceleratorAt(ServerPlayerEntity player, BlockPos pos) {
        if (player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) > 64.0D) {
            return null;
        }
        TileEntity tileEntity = player.level.getBlockEntity(pos);
        return tileEntity instanceof AbstractAcceleratorTileEntity ? tileEntity : null;
    }

    private static void sendAcceleratorState(ServerPlayerEntity player, BlockPos pos,
                                             AbstractAcceleratorTileEntity accelerator) {
        if (player.level instanceof net.minecraft.world.server.ServerWorld) {
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                    new AcceleratorStateMessage(pos, accelerator.createState(
                            (net.minecraft.world.server.ServerWorld) player.level)));
        }
    }

    public static final class RequestState {
        private final BlockPos pos;

        public RequestState(BlockPos pos) {
            this.pos = pos;
        }

        public RequestState(PacketBuffer buffer) {
            this.pos = buffer.readBlockPos();
        }

        public void encode(PacketBuffer buffer) {
            buffer.writeBlockPos(pos);
        }

        public static void handle(RequestState message, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            context.enqueueWork(() -> {
                ServerPlayerEntity player = context.getSender();
                if (player != null) {
                    TileEntity tileEntity = deviceAt(player, message.pos);
                    if (tileEntity instanceof ManaDeviceHost) {
                        sendState(player, message.pos, (ManaDeviceHost) tileEntity);
                    }
                }
            });
            context.setPacketHandled(true);
        }
    }

    public static final class SetRate {
        private final BlockPos pos;
        private final int rate;

        public SetRate(BlockPos pos, int rate) {
            this.pos = pos;
            this.rate = rate;
        }

        public SetRate(PacketBuffer buffer) {
            this.pos = buffer.readBlockPos();
            this.rate = buffer.readVarInt();
        }

        public void encode(PacketBuffer buffer) {
            buffer.writeBlockPos(pos);
            buffer.writeVarInt(rate);
        }

        public static void handle(SetRate message, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            context.enqueueWork(() -> {
                ServerPlayerEntity player = context.getSender();
                if (player != null) {
                    TileEntity tileEntity = deviceAt(player, message.pos);
                    if (tileEntity instanceof ManaDeviceHost) {
                        ManaDeviceHost host = (ManaDeviceHost) tileEntity;
                        host.settings().setRate(Math.max(0, message.rate));
                        tileEntity.setChanged();
                        sendState(player, message.pos, host);
                    }
                }
            });
            context.setPacketHandled(true);
        }
    }

    public static final class SetRange {
        private final BlockPos pos;
        private final int reachX;
        private final int reachY;
        private final int reachZ;
        private final int offsetX;
        private final int offsetY;
        private final int offsetZ;
        private final boolean showRange;

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

        public SetRange(PacketBuffer buffer) {
            this.pos = buffer.readBlockPos();
            this.reachX = buffer.readVarInt();
            this.reachY = buffer.readVarInt();
            this.reachZ = buffer.readVarInt();
            this.offsetX = buffer.readVarInt();
            this.offsetY = buffer.readVarInt();
            this.offsetZ = buffer.readVarInt();
            this.showRange = buffer.readBoolean();
        }

        public void encode(PacketBuffer buffer) {
            buffer.writeBlockPos(pos);
            buffer.writeVarInt(reachX);
            buffer.writeVarInt(reachY);
            buffer.writeVarInt(reachZ);
            buffer.writeVarInt(offsetX);
            buffer.writeVarInt(offsetY);
            buffer.writeVarInt(offsetZ);
            buffer.writeBoolean(showRange);
        }

        public static void handle(SetRange message, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            context.enqueueWork(() -> {
                ServerPlayerEntity player = context.getSender();
                if (player != null) {
                    TileEntity tileEntity = deviceAt(player, message.pos);
                    if ((tileEntity instanceof ManaWellTileEntity
                            || tileEntity instanceof ManaAnchorTileEntity)
                            && tileEntity instanceof ManaDeviceHost) {
                        ManaDeviceHost host = (ManaDeviceHost) tileEntity;
                        host.settings().setRange(message.reachX, message.reachY, message.reachZ,
                                message.offsetX, message.offsetY, message.offsetZ);
                        host.settings().setShowRange(message.showRange);
                        tileEntity.setChanged();
                        player.level.sendBlockUpdated(message.pos, tileEntity.getBlockState(),
                                tileEntity.getBlockState(), 3);
                        sendState(player, message.pos, host);
                    }
                }
            });
            context.setPacketHandled(true);
        }
    }

    public static final class ClearExcluded {
        private final BlockPos pos;

        public ClearExcluded(BlockPos pos) {
            this.pos = pos;
        }

        public ClearExcluded(PacketBuffer buffer) {
            this.pos = buffer.readBlockPos();
        }

        public void encode(PacketBuffer buffer) {
            buffer.writeBlockPos(pos);
        }

        public static void handle(ClearExcluded message, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            context.enqueueWork(() -> {
                ServerPlayerEntity player = context.getSender();
                if (player != null) {
                    TileEntity tileEntity = deviceAt(player, message.pos);
                    if ((tileEntity instanceof ManaWellTileEntity
                            || tileEntity instanceof ManaAnchorTileEntity)
                            && tileEntity instanceof ManaDeviceHost) {
                        ManaDeviceHost host = (ManaDeviceHost) tileEntity;
                        host.settings().clearExcluded();
                        tileEntity.setChanged();
                        sendState(player, message.pos, host);
                    }
                }
            });
            context.setPacketHandled(true);
        }
    }

    public static final class ToggleEnabled {
        private final BlockPos pos;

        public ToggleEnabled(BlockPos pos) {
            this.pos = pos;
        }

        public ToggleEnabled(PacketBuffer buffer) {
            this.pos = buffer.readBlockPos();
        }

        public void encode(PacketBuffer buffer) {
            buffer.writeBlockPos(pos);
        }

        public static void handle(ToggleEnabled message, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            context.enqueueWork(() -> {
                ServerPlayerEntity player = context.getSender();
                if (player != null) {
                    TileEntity tileEntity = deviceAt(player, message.pos);
                    if (tileEntity instanceof ManaDeviceHost) {
                        ManaDeviceHost host = (ManaDeviceHost) tileEntity;
                        host.setMasterEnabled(!host.isMasterEnabled());
                        tileEntity.setChanged();
                        sendState(player, message.pos, host);
                    }
                }
            });
            context.setPacketHandled(true);
        }
    }

    public static final class StateMessage {
        private final BlockPos pos;
        private final DeviceState state;

        public StateMessage(BlockPos pos, DeviceState state) {
            this.pos = pos;
            this.state = state;
        }

        public StateMessage(PacketBuffer buffer) {
            this.pos = buffer.readBlockPos();
            int rate = buffer.readVarInt();
            int effectiveRate = buffer.readVarInt();
            boolean enabled = buffer.readBoolean();
            long bufferMana = buffer.readVarLong();
            long bufferMax = buffer.readVarLong();
            String networkName = buffer.readUtf(32);
            int reachX = buffer.readVarInt();
            int reachY = buffer.readVarInt();
            int reachZ = buffer.readVarInt();
            int offsetX = buffer.readVarInt();
            int offsetY = buffer.readVarInt();
            int offsetZ = buffer.readVarInt();
            boolean showRange = buffer.readBoolean();
            int count = buffer.readVarInt();
            List<LinkEntry> links = new ArrayList<LinkEntry>(count);
            for (int index = 0; index < count; index++) {
                BlockPos linkPos = buffer.readBlockPos();
                String label = buffer.readUtf(64);
                int direction = buffer.readByte();
                boolean auto = buffer.readBoolean();
                links.add(new LinkEntry(linkPos, label, direction, auto));
            }
            this.state = new DeviceState(rate, effectiveRate, enabled, bufferMana, bufferMax,
                    networkName, reachX, reachY, reachZ, offsetX, offsetY, offsetZ, showRange, links);
        }

        public void encode(PacketBuffer buffer) {
            buffer.writeBlockPos(pos);
            buffer.writeVarInt(state.rate());
            buffer.writeVarInt(state.effectiveRate());
            buffer.writeBoolean(state.enabled());
            buffer.writeVarLong(state.buffer());
            buffer.writeVarLong(state.bufferMax());
            buffer.writeUtf(clamp(state.networkName(), 32), 32);
            buffer.writeVarInt(state.reachX());
            buffer.writeVarInt(state.reachY());
            buffer.writeVarInt(state.reachZ());
            buffer.writeVarInt(state.offsetX());
            buffer.writeVarInt(state.offsetY());
            buffer.writeVarInt(state.offsetZ());
            buffer.writeBoolean(state.showRange());
            buffer.writeVarInt(state.links().size());
            for (LinkEntry entry : state.links()) {
                buffer.writeBlockPos(entry.pos());
                buffer.writeUtf(clamp(entry.label(), 64), 64);
                buffer.writeByte(entry.direction());
                buffer.writeBoolean(entry.auto());
            }
        }

        private static String clamp(@Nullable String text, int maxLength) {
            if (text == null) {
                return "";
            }
            return text.length() <= maxLength ? text : text.substring(0, maxLength);
        }

        public static void handle(StateMessage message, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> ClientStateReceiver.receive(message.pos, message.state)));
            context.setPacketHandled(true);
        }
    }

    public static final class AcceleratorRequestState {
        private final BlockPos pos;

        public AcceleratorRequestState(BlockPos pos) {
            this.pos = pos;
        }

        public AcceleratorRequestState(PacketBuffer buffer) {
            this.pos = buffer.readBlockPos();
        }

        public void encode(PacketBuffer buffer) {
            buffer.writeBlockPos(pos);
        }

        public static void handle(AcceleratorRequestState message,
                                  Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            context.enqueueWork(() -> {
                ServerPlayerEntity player = context.getSender();
                if (player != null) {
                    TileEntity tileEntity = acceleratorAt(player, message.pos);
                    if (tileEntity instanceof AbstractAcceleratorTileEntity) {
                        sendAcceleratorState(player, message.pos,
                                (AbstractAcceleratorTileEntity) tileEntity);
                    }
                }
            });
            context.setPacketHandled(true);
        }
    }

    public static final class AcceleratorToggleEnabled {
        private final BlockPos pos;

        public AcceleratorToggleEnabled(BlockPos pos) {
            this.pos = pos;
        }

        public AcceleratorToggleEnabled(PacketBuffer buffer) {
            this.pos = buffer.readBlockPos();
        }

        public void encode(PacketBuffer buffer) {
            buffer.writeBlockPos(pos);
        }

        public static void handle(AcceleratorToggleEnabled message,
                                  Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            context.enqueueWork(() -> {
                ServerPlayerEntity player = context.getSender();
                if (player != null) {
                    TileEntity tileEntity = acceleratorAt(player, message.pos);
                    if (tileEntity instanceof AbstractAcceleratorTileEntity) {
                        AbstractAcceleratorTileEntity accelerator =
                                (AbstractAcceleratorTileEntity) tileEntity;
                        accelerator.toggleEnabled();
                        sendAcceleratorState(player, message.pos, accelerator);
                    }
                }
            });
            context.setPacketHandled(true);
        }
    }

    public static final class AcceleratorSetSettings {
        private final BlockPos pos;
        private final int rate;
        private final int cooldown;
        private final int reachX;
        private final int reachY;
        private final int reachZ;
        private final int offsetX;
        private final int offsetY;
        private final int offsetZ;
        private final boolean showRange;

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

        public AcceleratorSetSettings(PacketBuffer buffer) {
            this.pos = buffer.readBlockPos();
            this.rate = buffer.readVarInt();
            this.cooldown = buffer.readVarInt();
            this.reachX = buffer.readVarInt();
            this.reachY = buffer.readVarInt();
            this.reachZ = buffer.readVarInt();
            this.offsetX = buffer.readVarInt();
            this.offsetY = buffer.readVarInt();
            this.offsetZ = buffer.readVarInt();
            this.showRange = buffer.readBoolean();
        }

        public void encode(PacketBuffer buffer) {
            buffer.writeBlockPos(pos);
            buffer.writeVarInt(rate);
            buffer.writeVarInt(cooldown);
            buffer.writeVarInt(reachX);
            buffer.writeVarInt(reachY);
            buffer.writeVarInt(reachZ);
            buffer.writeVarInt(offsetX);
            buffer.writeVarInt(offsetY);
            buffer.writeVarInt(offsetZ);
            buffer.writeBoolean(showRange);
        }

        public static void handle(AcceleratorSetSettings message,
                                  Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            context.enqueueWork(() -> {
                ServerPlayerEntity player = context.getSender();
                if (player != null) {
                    TileEntity tileEntity = acceleratorAt(player, message.pos);
                    if (tileEntity instanceof AbstractAcceleratorTileEntity) {
                        AbstractAcceleratorTileEntity accelerator =
                                (AbstractAcceleratorTileEntity) tileEntity;
                        accelerator.setRate(message.rate);
                        accelerator.setRange(message.reachX, message.reachY, message.reachZ,
                                message.offsetX, message.offsetY, message.offsetZ,
                                message.showRange);
                        accelerator.setCooldownMultiplier(message.cooldown);
                        tileEntity.setChanged();
                        player.level.sendBlockUpdated(message.pos, tileEntity.getBlockState(),
                                tileEntity.getBlockState(), 3);
                        sendAcceleratorState(player, message.pos, accelerator);
                    }
                }
            });
            context.setPacketHandled(true);
        }
    }

    public static final class AcceleratorStateMessage {
        private final BlockPos pos;
        private final AcceleratorState state;

        public AcceleratorStateMessage(BlockPos pos, AcceleratorState state) {
            this.pos = pos;
            this.state = state;
        }

        public AcceleratorStateMessage(PacketBuffer buffer) {
            this.pos = buffer.readBlockPos();
            boolean enabled = buffer.readBoolean();
            boolean featureEnabled = buffer.readBoolean();
            int rate = buffer.readVarInt();
            int cooldown = buffer.readVarInt();
            int reachX = buffer.readVarInt();
            int reachY = buffer.readVarInt();
            int reachZ = buffer.readVarInt();
            int offsetX = buffer.readVarInt();
            int offsetY = buffer.readVarInt();
            int offsetZ = buffer.readVarInt();
            boolean showRange = buffer.readBoolean();
            int count = buffer.readVarInt();
            List<AcceleratorTarget> targets = new ArrayList<AcceleratorTarget>(count);
            for (int index = 0; index < count; index++) {
                BlockPos targetPos = buffer.readBlockPos();
                String label = buffer.readUtf(64);
                targets.add(new AcceleratorTarget(targetPos, label));
            }
            this.state = new AcceleratorState(enabled, featureEnabled, rate, cooldown,
                    reachX, reachY, reachZ, offsetX, offsetY, offsetZ, showRange, targets);
        }

        public void encode(PacketBuffer buffer) {
            buffer.writeBlockPos(pos);
            buffer.writeBoolean(state.enabled());
            buffer.writeBoolean(state.featureEnabled());
            buffer.writeVarInt(state.rate());
            buffer.writeVarInt(state.cooldown());
            buffer.writeVarInt(state.reachX());
            buffer.writeVarInt(state.reachY());
            buffer.writeVarInt(state.reachZ());
            buffer.writeVarInt(state.offsetX());
            buffer.writeVarInt(state.offsetY());
            buffer.writeVarInt(state.offsetZ());
            buffer.writeBoolean(state.showRange());
            buffer.writeVarInt(state.targets().size());
            for (AcceleratorTarget target : state.targets()) {
                buffer.writeBlockPos(target.pos());
                buffer.writeUtf(StateMessage.clamp(target.label(), 64), 64);
            }
        }

        public static void handle(AcceleratorStateMessage message,
                                  Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> ClientStateReceiver.receiveAccelerator(message.pos, message.state)));
            context.setPacketHandled(true);
        }
    }
}
