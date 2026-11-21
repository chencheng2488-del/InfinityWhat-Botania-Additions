package com.bandybus.infwhat.botania.network;

import com.bandybus.infwhat.botania.InfinityWhatBotania;
import com.bandybus.infwhat.botania.block.entity.AbstractAcceleratorTileEntity;
import com.bandybus.infwhat.botania.block.entity.AbstractManaEndpointTileEntity;
import com.bandybus.infwhat.botania.block.entity.ManaAnchorTileEntity;
import com.bandybus.infwhat.botania.block.entity.ManaWellTileEntity;
import com.bandybus.infwhat.botania.device.AcceleratorState;
import com.bandybus.infwhat.botania.device.AcceleratorTarget;
import com.bandybus.infwhat.botania.device.DeviceState;
import com.bandybus.infwhat.botania.device.LinkEntry;
import com.bandybus.infwhat.botania.device.ManaDeviceHost;
import com.bandybus.infwhat.botania.util.Pos;
import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

/**
 * Network channel for the device and accelerator GUIs.
 *
 * <p>Every client edit travels as a dedicated message; the server validates the block distance,
 * type and ownership before applying it, then answers with a complete state snapshot.
 */
public final class ModNetwork {

    private static final SimpleNetworkWrapper INSTANCE =
            NetworkRegistry.INSTANCE.newSimpleChannel(InfinityWhatBotania.MODID);

    private ModNetwork() {
    }

    public static void register() {
        int id = 0;
        INSTANCE.registerMessage(RequestState.Handler.class, RequestState.class, id++, Side.SERVER);
        INSTANCE.registerMessage(SetRate.Handler.class, SetRate.class, id++, Side.SERVER);
        INSTANCE.registerMessage(SetRange.Handler.class, SetRange.class, id++, Side.SERVER);
        INSTANCE.registerMessage(ClearExcluded.Handler.class, ClearExcluded.class, id++, Side.SERVER);
        INSTANCE.registerMessage(ToggleDevice.Handler.class, ToggleDevice.class, id++, Side.SERVER);
        INSTANCE.registerMessage(StateMessage.Handler.class, StateMessage.class, id++, Side.CLIENT);
        INSTANCE.registerMessage(AcceleratorRequest.Handler.class, AcceleratorRequest.class,
                id++, Side.SERVER);
        INSTANCE.registerMessage(AcceleratorToggle.Handler.class, AcceleratorToggle.class,
                id++, Side.SERVER);
        INSTANCE.registerMessage(AcceleratorSettings.Handler.class, AcceleratorSettings.class,
                id++, Side.SERVER);
        INSTANCE.registerMessage(AcceleratorStateMessage.Handler.class,
                AcceleratorStateMessage.class, id++, Side.CLIENT);
    }

    // ---- send helpers used by the screens ----

    public static void requestDevice(Pos pos) {
        INSTANCE.sendToServer(new RequestState(pos));
    }

    public static void setRate(Pos pos, int rate) {
        INSTANCE.sendToServer(new SetRate(pos, rate));
    }

    public static void setRange(Pos pos, int reachX, int reachY, int reachZ,
                                int offsetX, int offsetY, int offsetZ, boolean showRange) {
        INSTANCE.sendToServer(new SetRange(pos, reachX, reachY, reachZ,
                offsetX, offsetY, offsetZ, showRange));
    }

    public static void clearExcluded(Pos pos) {
        INSTANCE.sendToServer(new ClearExcluded(pos));
    }

    public static void toggleDevice(Pos pos) {
        INSTANCE.sendToServer(new ToggleDevice(pos));
    }

    public static void requestAccelerator(Pos pos) {
        INSTANCE.sendToServer(new AcceleratorRequest(pos));
    }

    public static void toggleAccelerator(Pos pos) {
        INSTANCE.sendToServer(new AcceleratorToggle(pos));
    }

    public static void setAccelerator(Pos pos, int rate, int cooldown,
                                      int reachX, int reachY, int reachZ,
                                      int offsetX, int offsetY, int offsetZ,
                                      boolean showRange) {
        INSTANCE.sendToServer(new AcceleratorSettings(pos, rate, cooldown,
                reachX, reachY, reachZ, offsetX, offsetY, offsetZ, showRange));
    }

    // ---- server side validation ----

    private static TileEntity deviceAt(EntityPlayerMP player, Pos pos) {
        if (player == null || pos == null) {
            return null;
        }
        if (player.getDistanceSq( pos.x + 0.5D, pos.y + 0.5D, pos.z + 0.5D) > 64.0D) {
            return null;
        }
        TileEntity tileEntity = player.worldObj.getTileEntity(pos.x, pos.y, pos.z);
        if (!(tileEntity instanceof ManaDeviceHost)) {
            return null;
        }
        if (tileEntity instanceof AbstractManaEndpointTileEntity) {
            AbstractManaEndpointTileEntity endpoint =
                    (AbstractManaEndpointTileEntity) tileEntity;
            if (endpoint.getOwnerId() != null && !endpoint.isOwnedBy(player.getUniqueID())) {
                return null;
            }
        }
        return tileEntity;
    }

    private static TileEntity acceleratorAt(EntityPlayerMP player, Pos pos) {
        if (player == null || pos == null) {
            return null;
        }
        if (player.getDistanceSq(pos.x + 0.5D, pos.y + 0.5D, pos.z + 0.5D) > 64.0D) {
            return null;
        }
        TileEntity tileEntity = player.worldObj.getTileEntity(pos.x, pos.y, pos.z);
        return tileEntity instanceof AbstractAcceleratorTileEntity ? tileEntity : null;
    }

    private static void sendState(EntityPlayerMP player, Pos pos, ManaDeviceHost host) {
        World world = player.worldObj;
        INSTANCE.sendTo(new StateMessage(pos, host.createState(world)), player);
    }

    private static void sendAcceleratorState(EntityPlayerMP player, Pos pos,
                                             AbstractAcceleratorTileEntity accelerator) {
        INSTANCE.sendTo(new AcceleratorStateMessage(pos,
                accelerator.createState(player.worldObj)), player);
    }

    private static void writePos(ByteBuf buf, Pos pos) {
        buf.writeInt(pos.x);
        buf.writeInt(pos.y);
        buf.writeInt(pos.z);
    }

    private static Pos readPos(ByteBuf buf) {
        return new Pos(buf.readInt(), buf.readInt(), buf.readInt());
    }

    private static String readString(ByteBuf buf) {
        return ByteBufUtils.readUTF8String(buf);
    }

    private static void writeString(ByteBuf buf, String value) {
        ByteBufUtils.writeUTF8String(buf, clamp(value, 64));
    }

    private static String clamp(String value, int max) {
        if (value == null) {
            return "";
        }
        return value.length() <= max ? value : value.substring(0, max);
    }

    // ---- client to server messages ----

    public static final class RequestState implements IMessage {

        private Pos pos;

        public RequestState() {
        }

        public RequestState(Pos pos) {
            this.pos = pos;
        }

        @Override
        public void fromBytes(ByteBuf buf) {
            pos = readPos(buf);
        }

        @Override
        public void toBytes(ByteBuf buf) {
            writePos(buf, pos);
        }

        public static final class Handler implements IMessageHandler<RequestState, IMessage> {

            @Override
            public IMessage onMessage(final RequestState message, final MessageContext ctx) {
                ServerTaskQueue.enqueue(new Runnable() {
                    @Override
                    public void run() {
                        EntityPlayerMP player = ctx.getServerHandler().playerEntity;
                        TileEntity tileEntity = deviceAt(player, message.pos);
                        if (tileEntity instanceof ManaDeviceHost) {
                            sendState(player, message.pos, (ManaDeviceHost) tileEntity);
                        }
                    }
                });
                return null;
            }
        }
    }

    public static final class SetRate implements IMessage {

        private Pos pos;
        private int rate;

        public SetRate() {
        }

        public SetRate(Pos pos, int rate) {
            this.pos = pos;
            this.rate = rate;
        }

        @Override
        public void fromBytes(ByteBuf buf) {
            pos = readPos(buf);
            rate = buf.readInt();
        }

        @Override
        public void toBytes(ByteBuf buf) {
            writePos(buf, pos);
            buf.writeInt(rate);
        }

        public static final class Handler implements IMessageHandler<SetRate, IMessage> {

            @Override
            public IMessage onMessage(final SetRate message, final MessageContext ctx) {
                ServerTaskQueue.enqueue(new Runnable() {
                    @Override
                    public void run() {
                        EntityPlayerMP player = ctx.getServerHandler().playerEntity;
                        TileEntity tileEntity = deviceAt(player, message.pos);
                        if (tileEntity instanceof ManaDeviceHost) {
                            ManaDeviceHost host = (ManaDeviceHost) tileEntity;
                            host.settings().setRate(Math.max(0, message.rate));
                            tileEntity.markDirty();
                            sendState(player, message.pos, host);
                        }
                    }
                });
                return null;
            }
        }
    }

    public static final class SetRange implements IMessage {

        private Pos pos;
        private int reachX;
        private int reachY;
        private int reachZ;
        private int offsetX;
        private int offsetY;
        private int offsetZ;
        private boolean showRange;

        public SetRange() {
        }

        public SetRange(Pos pos, int reachX, int reachY, int reachZ,
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
        public void fromBytes(ByteBuf buf) {
            pos = readPos(buf);
            reachX = buf.readInt();
            reachY = buf.readInt();
            reachZ = buf.readInt();
            offsetX = buf.readInt();
            offsetY = buf.readInt();
            offsetZ = buf.readInt();
            showRange = buf.readBoolean();
        }

        @Override
        public void toBytes(ByteBuf buf) {
            writePos(buf, pos);
            buf.writeInt(reachX);
            buf.writeInt(reachY);
            buf.writeInt(reachZ);
            buf.writeInt(offsetX);
            buf.writeInt(offsetY);
            buf.writeInt(offsetZ);
            buf.writeBoolean(showRange);
        }

        public static final class Handler implements IMessageHandler<SetRange, IMessage> {

            @Override
            public IMessage onMessage(final SetRange message, final MessageContext ctx) {
                ServerTaskQueue.enqueue(new Runnable() {
                    @Override
                    public void run() {
                        EntityPlayerMP player = ctx.getServerHandler().playerEntity;
                        TileEntity tileEntity = deviceAt(player, message.pos);
                        if ((tileEntity instanceof ManaWellTileEntity
                                || tileEntity instanceof ManaAnchorTileEntity)
                                && tileEntity instanceof ManaDeviceHost) {
                            ManaDeviceHost host = (ManaDeviceHost) tileEntity;
                            host.settings().setRange(message.reachX, message.reachY,
                                    message.reachZ, message.offsetX, message.offsetY,
                                    message.offsetZ);
                            host.settings().setShowRange(message.showRange);
                            tileEntity.markDirty();
                            player.worldObj.markBlockForUpdate(
                                    message.pos.x, message.pos.y, message.pos.z);
                            sendState(player, message.pos, host);
                        }
                    }
                });
                return null;
            }
        }
    }

    public static final class ClearExcluded implements IMessage {

        private Pos pos;

        public ClearExcluded() {
        }

        public ClearExcluded(Pos pos) {
            this.pos = pos;
        }

        @Override
        public void fromBytes(ByteBuf buf) {
            pos = readPos(buf);
        }

        @Override
        public void toBytes(ByteBuf buf) {
            writePos(buf, pos);
        }

        public static final class Handler implements IMessageHandler<ClearExcluded, IMessage> {

            @Override
            public IMessage onMessage(final ClearExcluded message, final MessageContext ctx) {
                ServerTaskQueue.enqueue(new Runnable() {
                    @Override
                    public void run() {
                        EntityPlayerMP player = ctx.getServerHandler().playerEntity;
                        TileEntity tileEntity = deviceAt(player, message.pos);
                        if ((tileEntity instanceof ManaWellTileEntity
                                || tileEntity instanceof ManaAnchorTileEntity)
                                && tileEntity instanceof ManaDeviceHost) {
                            ManaDeviceHost host = (ManaDeviceHost) tileEntity;
                            host.settings().clearExcluded();
                            tileEntity.markDirty();
                            sendState(player, message.pos, host);
                        }
                    }
                });
                return null;
            }
        }
    }

    public static final class ToggleDevice implements IMessage {

        private Pos pos;

        public ToggleDevice() {
        }

        public ToggleDevice(Pos pos) {
            this.pos = pos;
        }

        @Override
        public void fromBytes(ByteBuf buf) {
            pos = readPos(buf);
        }

        @Override
        public void toBytes(ByteBuf buf) {
            writePos(buf, pos);
        }

        public static final class Handler implements IMessageHandler<ToggleDevice, IMessage> {

            @Override
            public IMessage onMessage(final ToggleDevice message, final MessageContext ctx) {
                ServerTaskQueue.enqueue(new Runnable() {
                    @Override
                    public void run() {
                        EntityPlayerMP player = ctx.getServerHandler().playerEntity;
                        TileEntity tileEntity = deviceAt(player, message.pos);
                        if (tileEntity instanceof ManaDeviceHost) {
                            ManaDeviceHost host = (ManaDeviceHost) tileEntity;
                            host.setMasterEnabled(!host.isMasterEnabled());
                            tileEntity.markDirty();
                            sendState(player, message.pos, host);
                        }
                    }
                });
                return null;
            }
        }
    }

    public static final class AcceleratorRequest implements IMessage {

        private Pos pos;

        public AcceleratorRequest() {
        }

        public AcceleratorRequest(Pos pos) {
            this.pos = pos;
        }

        @Override
        public void fromBytes(ByteBuf buf) {
            pos = readPos(buf);
        }

        @Override
        public void toBytes(ByteBuf buf) {
            writePos(buf, pos);
        }

        public static final class Handler
                implements IMessageHandler<AcceleratorRequest, IMessage> {

            @Override
            public IMessage onMessage(final AcceleratorRequest message,
                                      final MessageContext ctx) {
                ServerTaskQueue.enqueue(new Runnable() {
                    @Override
                    public void run() {
                        EntityPlayerMP player = ctx.getServerHandler().playerEntity;
                        TileEntity tileEntity = acceleratorAt(player, message.pos);
                        if (tileEntity instanceof AbstractAcceleratorTileEntity) {
                            sendAcceleratorState(player, message.pos,
                                    (AbstractAcceleratorTileEntity) tileEntity);
                        }
                    }
                });
                return null;
            }
        }
    }

    public static final class AcceleratorToggle implements IMessage {

        private Pos pos;

        public AcceleratorToggle() {
        }

        public AcceleratorToggle(Pos pos) {
            this.pos = pos;
        }

        @Override
        public void fromBytes(ByteBuf buf) {
            pos = readPos(buf);
        }

        @Override
        public void toBytes(ByteBuf buf) {
            writePos(buf, pos);
        }

        public static final class Handler
                implements IMessageHandler<AcceleratorToggle, IMessage> {

            @Override
            public IMessage onMessage(final AcceleratorToggle message,
                                      final MessageContext ctx) {
                ServerTaskQueue.enqueue(new Runnable() {
                    @Override
                    public void run() {
                        EntityPlayerMP player = ctx.getServerHandler().playerEntity;
                        TileEntity tileEntity = acceleratorAt(player, message.pos);
                        if (tileEntity instanceof AbstractAcceleratorTileEntity) {
                            AbstractAcceleratorTileEntity accelerator =
                                    (AbstractAcceleratorTileEntity) tileEntity;
                            accelerator.toggleEnabled();
                            sendAcceleratorState(player, message.pos, accelerator);
                        }
                    }
                });
                return null;
            }
        }
    }

    public static final class AcceleratorSettings implements IMessage {

        private Pos pos;
        private int rate;
        private int cooldown;
        private int reachX;
        private int reachY;
        private int reachZ;
        private int offsetX;
        private int offsetY;
        private int offsetZ;
        private boolean showRange;

        public AcceleratorSettings() {
        }

        public AcceleratorSettings(Pos pos, int rate, int cooldown,
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
        public void fromBytes(ByteBuf buf) {
            pos = readPos(buf);
            rate = buf.readInt();
            cooldown = buf.readInt();
            reachX = buf.readInt();
            reachY = buf.readInt();
            reachZ = buf.readInt();
            offsetX = buf.readInt();
            offsetY = buf.readInt();
            offsetZ = buf.readInt();
            showRange = buf.readBoolean();
        }

        @Override
        public void toBytes(ByteBuf buf) {
            writePos(buf, pos);
            buf.writeInt(rate);
            buf.writeInt(cooldown);
            buf.writeInt(reachX);
            buf.writeInt(reachY);
            buf.writeInt(reachZ);
            buf.writeInt(offsetX);
            buf.writeInt(offsetY);
            buf.writeInt(offsetZ);
            buf.writeBoolean(showRange);
        }

        public static final class Handler
                implements IMessageHandler<AcceleratorSettings, IMessage> {

            @Override
            public IMessage onMessage(final AcceleratorSettings message,
                                      final MessageContext ctx) {
                ServerTaskQueue.enqueue(new Runnable() {
                    @Override
                    public void run() {
                        EntityPlayerMP player = ctx.getServerHandler().playerEntity;
                        TileEntity tileEntity = acceleratorAt(player, message.pos);
                        if (tileEntity instanceof AbstractAcceleratorTileEntity) {
                            AbstractAcceleratorTileEntity accelerator =
                                    (AbstractAcceleratorTileEntity) tileEntity;
                            accelerator.setRate(message.rate);
                            accelerator.setRange(message.reachX, message.reachY,
                                    message.reachZ, message.offsetX, message.offsetY,
                                    message.offsetZ, message.showRange);
                            accelerator.setCooldownMultiplier(message.cooldown);
                            tileEntity.markDirty();
                            player.worldObj.markBlockForUpdate(
                                    message.pos.x, message.pos.y, message.pos.z);
                            sendAcceleratorState(player, message.pos, accelerator);
                        }
                    }
                });
                return null;
            }
        }
    }

    // ---- server to client messages ----

    public static final class StateMessage implements IMessage {

        private Pos pos;
        private DeviceState state;

        public StateMessage() {
        }

        public StateMessage(Pos pos, DeviceState state) {
            this.pos = pos;
            this.state = state;
        }

        @Override
        public void fromBytes(ByteBuf buf) {
            pos = readPos(buf);
            int rate = buf.readInt();
            int effectiveRate = buf.readInt();
            boolean enabled = buf.readBoolean();
            long buffer = buf.readLong();
            long bufferMax = buf.readLong();
            String networkName = readString(buf);
            int reachX = buf.readInt();
            int reachY = buf.readInt();
            int reachZ = buf.readInt();
            int offsetX = buf.readInt();
            int offsetY = buf.readInt();
            int offsetZ = buf.readInt();
            boolean showRange = buf.readBoolean();
            int count = buf.readInt();
            List<LinkEntry> links = new ArrayList<LinkEntry>();
            for (int index = 0; index < count; index++) {
                Pos linkPos = readPos(buf);
                String label = readString(buf);
                int direction = buf.readByte();
                boolean auto = buf.readBoolean();
                links.add(new LinkEntry(linkPos, label, direction, auto));
            }
            state = new DeviceState(rate, effectiveRate, enabled, buffer, bufferMax,
                    networkName, reachX, reachY, reachZ, offsetX, offsetY, offsetZ,
                    showRange, links);
        }

        @Override
        public void toBytes(ByteBuf buf) {
            writePos(buf, pos);
            buf.writeInt(state.rate());
            buf.writeInt(state.effectiveRate());
            buf.writeBoolean(state.enabled());
            buf.writeLong(state.buffer());
            buf.writeLong(state.bufferMax());
            writeString(buf, state.networkName());
            buf.writeInt(state.reachX());
            buf.writeInt(state.reachY());
            buf.writeInt(state.reachZ());
            buf.writeInt(state.offsetX());
            buf.writeInt(state.offsetY());
            buf.writeInt(state.offsetZ());
            buf.writeBoolean(state.showRange());
            buf.writeInt(state.links().size());
            for (LinkEntry entry : state.links()) {
                writePos(buf, entry.pos());
                writeString(buf, entry.label());
                buf.writeByte(entry.direction());
                buf.writeBoolean(entry.auto());
            }
        }

        public static final class Handler implements IMessageHandler<StateMessage, IMessage> {

            @Override
            public IMessage onMessage(StateMessage message, MessageContext ctx) {
                InfinityWhatBotania.proxy.applyDeviceState(message.pos, message.state);
                return null;
            }
        }
    }

    public static final class AcceleratorStateMessage implements IMessage {

        private Pos pos;
        private AcceleratorState state;

        public AcceleratorStateMessage() {
        }

        public AcceleratorStateMessage(Pos pos, AcceleratorState state) {
            this.pos = pos;
            this.state = state;
        }

        @Override
        public void fromBytes(ByteBuf buf) {
            pos = readPos(buf);
            boolean enabled = buf.readBoolean();
            boolean featureEnabled = buf.readBoolean();
            int rate = buf.readInt();
            int cooldown = buf.readInt();
            int reachX = buf.readInt();
            int reachY = buf.readInt();
            int reachZ = buf.readInt();
            int offsetX = buf.readInt();
            int offsetY = buf.readInt();
            int offsetZ = buf.readInt();
            boolean showRange = buf.readBoolean();
            int count = buf.readInt();
            List<AcceleratorTarget> targets = new ArrayList<AcceleratorTarget>();
            for (int index = 0; index < count; index++) {
                Pos targetPos = readPos(buf);
                String label = readString(buf);
                targets.add(new AcceleratorTarget(targetPos, label));
            }
            state = new AcceleratorState(enabled, featureEnabled, rate, cooldown,
                    reachX, reachY, reachZ, offsetX, offsetY, offsetZ, showRange, targets);
        }

        @Override
        public void toBytes(ByteBuf buf) {
            writePos(buf, pos);
            buf.writeBoolean(state.enabled());
            buf.writeBoolean(state.featureEnabled());
            buf.writeInt(state.rate());
            buf.writeInt(state.cooldown());
            buf.writeInt(state.reachX());
            buf.writeInt(state.reachY());
            buf.writeInt(state.reachZ());
            buf.writeInt(state.offsetX());
            buf.writeInt(state.offsetY());
            buf.writeInt(state.offsetZ());
            buf.writeBoolean(state.showRange());
            buf.writeInt(state.targets().size());
            for (AcceleratorTarget target : state.targets()) {
                writePos(buf, target.pos());
                writeString(buf, target.label());
            }
        }

        public static final class Handler
                implements IMessageHandler<AcceleratorStateMessage, IMessage> {

            @Override
            public IMessage onMessage(AcceleratorStateMessage message, MessageContext ctx) {
                InfinityWhatBotania.proxy.applyAcceleratorState(message.pos, message.state);
                return null;
            }
        }
    }
}
