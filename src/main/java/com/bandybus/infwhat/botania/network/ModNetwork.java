package com.bandybus.infwhat.botania.network;

import com.bandybus.infwhat.botania.InfinityWhatBotania;
import com.bandybus.infwhat.botania.block.entity.AbstractAcceleratorBlockEntity;
import com.bandybus.infwhat.botania.block.entity.AbstractManaEndpointBlockEntity;
import com.bandybus.infwhat.botania.block.entity.ManaAnchorBlockEntity;
import com.bandybus.infwhat.botania.block.entity.ManaWellBlockEntity;
import com.bandybus.infwhat.botania.block.entity.RuneAcceleratorBlockEntity;
import com.bandybus.infwhat.botania.client.ClientStateReceiver;
import com.bandybus.infwhat.botania.device.AcceleratorState;
import com.bandybus.infwhat.botania.device.AcceleratorTarget;
import com.bandybus.infwhat.botania.device.DeviceState;
import com.bandybus.infwhat.botania.device.LinkEntry;
import com.bandybus.infwhat.botania.device.ManaDeviceHost;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Packets for the device GUI plus the server-side handlers. */
public final class ModNetwork {

    private static final String PROTOCOL = "5";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(InfinityWhatBotania.MOD_ID, "main"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    private ModNetwork() {
    }

    public static void register() {
        int id = 0;
        CHANNEL.messageBuilder(RequestState.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(RequestState::encode)
                .decoder(RequestState::new)
                .consumerMainThread(RequestState::handle)
                .add();
        CHANNEL.messageBuilder(SetRate.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SetRate::encode)
                .decoder(SetRate::new)
                .consumerMainThread(SetRate::handle)
                .add();
        CHANNEL.messageBuilder(SetRange.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SetRange::encode)
                .decoder(SetRange::new)
                .consumerMainThread(SetRange::handle)
                .add();
        CHANNEL.messageBuilder(ClearExcluded.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ClearExcluded::encode)
                .decoder(ClearExcluded::new)
                .consumerMainThread(ClearExcluded::handle)
                .add();
        CHANNEL.messageBuilder(ToggleEnabled.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ToggleEnabled::encode)
                .decoder(ToggleEnabled::new)
                .consumerMainThread(ToggleEnabled::handle)
                .add();
        CHANNEL.messageBuilder(StateMessage.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(StateMessage::encode)
                .decoder(StateMessage::new)
                .consumerMainThread(StateMessage::handle)
                .add();
        CHANNEL.messageBuilder(AcceleratorRequestState.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(AcceleratorRequestState::encode)
                .decoder(AcceleratorRequestState::new)
                .consumerMainThread(AcceleratorRequestState::handle)
                .add();
        CHANNEL.messageBuilder(AcceleratorToggleEnabled.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(AcceleratorToggleEnabled::encode)
                .decoder(AcceleratorToggleEnabled::new)
                .consumerMainThread(AcceleratorToggleEnabled::handle)
                .add();
        CHANNEL.messageBuilder(AcceleratorSetSettings.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(AcceleratorSetSettings::encode)
                .decoder(AcceleratorSetSettings::new)
                .consumerMainThread(AcceleratorSetSettings::handle)
                .add();
        CHANNEL.messageBuilder(AcceleratorStateMessage.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(AcceleratorStateMessage::encode)
                .decoder(AcceleratorStateMessage::new)
                .consumerMainThread(AcceleratorStateMessage::handle)
                .add();
    }

    private static BlockEntity deviceAt(ServerPlayer player, BlockPos pos) {
        if (player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) > 64.0D) {
            return null;
        }
        BlockEntity blockEntity = player.level().getBlockEntity(pos);
        if (!(blockEntity instanceof ManaDeviceHost)) {
            return null;
        }
        if (blockEntity instanceof AbstractManaEndpointBlockEntity endpoint
                && endpoint.getOwnerId() != null && !endpoint.isOwnedBy(player.getUUID())) {
            return null;
        }
        return blockEntity;
    }

    private static void sendState(ServerPlayer player, BlockPos pos, ManaDeviceHost host) {
        if (player.level() instanceof ServerLevel level) {
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                    new StateMessage(pos, host.createState(level)));
        }
    }

    private static BlockEntity acceleratorAt(ServerPlayer player, BlockPos pos) {
        if (player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) > 64.0D) {
            return null;
        }
        BlockEntity blockEntity = player.level().getBlockEntity(pos);
        return blockEntity instanceof AbstractAcceleratorBlockEntity ? blockEntity : null;
    }

    private static void sendAcceleratorState(ServerPlayer player, BlockPos pos,
                                             AbstractAcceleratorBlockEntity accelerator) {
        if (player.level() instanceof ServerLevel level) {
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                    new AcceleratorStateMessage(pos, accelerator.createState(level)));
        }
    }

    public static final class RequestState {
        private final BlockPos pos;

        public RequestState(BlockPos pos) {
            this.pos = pos;
        }

        public RequestState(FriendlyByteBuf buffer) {
            this.pos = buffer.readBlockPos();
        }

        public void encode(FriendlyByteBuf buffer) {
            buffer.writeBlockPos(pos);
        }

        public static void handle(RequestState message, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            ServerPlayer player = context.getSender();
            if (player != null) {
                BlockEntity blockEntity = deviceAt(player, message.pos);
                if (blockEntity instanceof ManaDeviceHost host) {
                    sendState(player, message.pos, host);
                }
            }
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

        public SetRate(FriendlyByteBuf buffer) {
            this.pos = buffer.readBlockPos();
            this.rate = buffer.readVarInt();
        }

        public void encode(FriendlyByteBuf buffer) {
            buffer.writeBlockPos(pos);
            buffer.writeVarInt(rate);
        }

        public static void handle(SetRate message, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            ServerPlayer player = context.getSender();
            if (player != null) {
                BlockEntity blockEntity = deviceAt(player, message.pos);
                if (blockEntity instanceof ManaDeviceHost host) {
                    host.settings().setRate(Math.max(0, message.rate));
                    blockEntity.setChanged();
                    sendState(player, message.pos, host);
                }
            }
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

        public SetRange(FriendlyByteBuf buffer) {
            this.pos = buffer.readBlockPos();
            this.reachX = buffer.readVarInt();
            this.reachY = buffer.readVarInt();
            this.reachZ = buffer.readVarInt();
            this.offsetX = buffer.readVarInt();
            this.offsetY = buffer.readVarInt();
            this.offsetZ = buffer.readVarInt();
            this.showRange = buffer.readBoolean();
        }

        public void encode(FriendlyByteBuf buffer) {
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
            ServerPlayer player = context.getSender();
            if (player != null) {
                BlockEntity blockEntity = deviceAt(player, message.pos);
                if ((blockEntity instanceof ManaWellBlockEntity
                        || blockEntity instanceof ManaAnchorBlockEntity)
                        && blockEntity instanceof ManaDeviceHost host) {
                    host.settings().setRange(message.reachX, message.reachY, message.reachZ,
                            message.offsetX, message.offsetY, message.offsetZ);
                    host.settings().setShowRange(message.showRange);
                    blockEntity.setChanged();
                    player.level().sendBlockUpdated(message.pos, blockEntity.getBlockState(),
                            blockEntity.getBlockState(), 3);
                    sendState(player, message.pos, host);
                }
            }
            context.setPacketHandled(true);
        }
    }

    public static final class ClearExcluded {
        private final BlockPos pos;

        public ClearExcluded(BlockPos pos) {
            this.pos = pos;
        }

        public ClearExcluded(FriendlyByteBuf buffer) {
            this.pos = buffer.readBlockPos();
        }

        public void encode(FriendlyByteBuf buffer) {
            buffer.writeBlockPos(pos);
        }

        public static void handle(ClearExcluded message, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            ServerPlayer player = context.getSender();
            if (player != null) {
                BlockEntity blockEntity = deviceAt(player, message.pos);
                if ((blockEntity instanceof ManaWellBlockEntity
                        || blockEntity instanceof ManaAnchorBlockEntity)
                        && blockEntity instanceof ManaDeviceHost host) {
                    host.settings().clearExcluded();
                    blockEntity.setChanged();
                    sendState(player, message.pos, host);
                }
            }
            context.setPacketHandled(true);
        }
    }

    public static final class ToggleEnabled {
        private final BlockPos pos;

        public ToggleEnabled(BlockPos pos) {
            this.pos = pos;
        }

        public ToggleEnabled(FriendlyByteBuf buffer) {
            this.pos = buffer.readBlockPos();
        }

        public void encode(FriendlyByteBuf buffer) {
            buffer.writeBlockPos(pos);
        }

        public static void handle(ToggleEnabled message, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            ServerPlayer player = context.getSender();
            if (player != null) {
                BlockEntity blockEntity = deviceAt(player, message.pos);
                if (blockEntity instanceof ManaDeviceHost host) {
                    host.setMasterEnabled(!host.isMasterEnabled());
                    blockEntity.setChanged();
                    sendState(player, message.pos, host);
                }
            }
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

        public StateMessage(FriendlyByteBuf buffer) {
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
            List<LinkEntry> links = new ArrayList<>(count);
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

        public void encode(FriendlyByteBuf buffer) {
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
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> ClientStateReceiver.receive(message.pos, message.state));
            context.setPacketHandled(true);
        }
    }

    public static final class AcceleratorRequestState {
        private final BlockPos pos;

        public AcceleratorRequestState(BlockPos pos) {
            this.pos = pos;
        }

        public AcceleratorRequestState(FriendlyByteBuf buffer) {
            this.pos = buffer.readBlockPos();
        }

        public void encode(FriendlyByteBuf buffer) {
            buffer.writeBlockPos(pos);
        }

        public static void handle(AcceleratorRequestState message,
                                  Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            ServerPlayer player = context.getSender();
            if (player != null) {
                BlockEntity blockEntity = acceleratorAt(player, message.pos);
                if (blockEntity instanceof AbstractAcceleratorBlockEntity accelerator) {
                    sendAcceleratorState(player, message.pos, accelerator);
                }
            }
            context.setPacketHandled(true);
        }
    }

    public static final class AcceleratorToggleEnabled {
        private final BlockPos pos;

        public AcceleratorToggleEnabled(BlockPos pos) {
            this.pos = pos;
        }

        public AcceleratorToggleEnabled(FriendlyByteBuf buffer) {
            this.pos = buffer.readBlockPos();
        }

        public void encode(FriendlyByteBuf buffer) {
            buffer.writeBlockPos(pos);
        }

        public static void handle(AcceleratorToggleEnabled message,
                                  Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            ServerPlayer player = context.getSender();
            if (player != null) {
                BlockEntity blockEntity = acceleratorAt(player, message.pos);
                if (blockEntity instanceof AbstractAcceleratorBlockEntity accelerator) {
                    accelerator.toggleEnabled();
                    sendAcceleratorState(player, message.pos, accelerator);
                }
            }
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

        public AcceleratorSetSettings(FriendlyByteBuf buffer) {
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

        public void encode(FriendlyByteBuf buffer) {
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
            ServerPlayer player = context.getSender();
            if (player != null) {
                BlockEntity blockEntity = acceleratorAt(player, message.pos);
                if (blockEntity instanceof AbstractAcceleratorBlockEntity accelerator) {
                    accelerator.setRate(message.rate);
                    accelerator.setRange(message.reachX, message.reachY, message.reachZ,
                            message.offsetX, message.offsetY, message.offsetZ,
                            message.showRange);
                    accelerator.setCooldownMultiplier(message.cooldown);
                    blockEntity.setChanged();
                    player.level().sendBlockUpdated(message.pos, blockEntity.getBlockState(),
                            blockEntity.getBlockState(), 3);
                    sendAcceleratorState(player, message.pos, accelerator);
                }
            }
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

        public AcceleratorStateMessage(FriendlyByteBuf buffer) {
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
            List<AcceleratorTarget> targets = new ArrayList<>(count);
            for (int index = 0; index < count; index++) {
                BlockPos targetPos = buffer.readBlockPos();
                String label = buffer.readUtf(64);
                targets.add(new AcceleratorTarget(targetPos, label));
            }
            this.state = new AcceleratorState(enabled, featureEnabled, rate, cooldown,
                    reachX, reachY, reachZ, offsetX, offsetY, offsetZ, showRange, targets);
        }

        public void encode(FriendlyByteBuf buffer) {
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
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> ClientStateReceiver.receiveAccelerator(message.pos, message.state));
            context.setPacketHandled(true);
        }
    }
}
