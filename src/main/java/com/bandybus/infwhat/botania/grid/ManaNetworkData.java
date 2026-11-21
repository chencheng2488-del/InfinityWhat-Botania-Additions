package com.bandybus.infwhat.botania.grid;

import com.bandybus.infwhat.botania.block.entity.ManaAnchorTileEntity;
import com.bandybus.infwhat.botania.block.entity.ManaBeaconTileEntity;
import com.bandybus.infwhat.botania.block.entity.ManaWellTileEntity;
import com.bandybus.infwhat.botania.device.LinkEntry;
import com.bandybus.infwhat.botania.device.ManaNodeKind;
import com.bandybus.infwhat.botania.util.ManaReceiverHelper;
import com.bandybus.infwhat.botania.util.Pos;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraft.world.WorldSavedData;
import net.minecraft.world.storage.MapStorage;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * World-level topology of the implicit mana networks.
 *
 * <p>Players never type a channel name. A network is simply a connected component of explicitly
 * linked blocks, and receives a random name that is only used to report conflicts and to show a
 * label in the GUI. Two components that both contain a mana well may never be joined; this is the
 * only rule that depends on the name.
 */
public final class ManaNetworkData extends WorldSavedData {

    private static final String DATA_NAME = "infwhat_botania_mana_network";

    private static final String TAG_NODES = "Nodes";
    private static final String TAG_EDGES = "Edges";
    private static final String TAG_BUFFERS = "Buffers";
    private static final String TAG_DIMENSION = "Dimension";
    private static final String TAG_POS = "Pos";
    private static final String TAG_KIND = "Kind";
    private static final String TAG_NETWORK = "Network";
    private static final String TAG_FROM_DIMENSION = "FromDimension";
    private static final String TAG_FROM_POS = "FromPos";
    private static final String TAG_TO_DIMENSION = "ToDimension";
    private static final String TAG_TO_POS = "ToPos";
    private static final String TAG_AUTO = "Auto";
    private static final String TAG_NAME = "Name";
    private static final String TAG_MANA = "Mana";
    private static final String TAG_CLIENT_LINKS = "Links";

    private static final Set<ManaNodeKind> WELL_OUTPUTS =
            EnumSet.of(ManaNodeKind.BEACON, ManaNodeKind.STORAGE);
    private static final Set<ManaNodeKind> BEACON_INPUTS =
            EnumSet.of(ManaNodeKind.STORAGE);
    private static final Set<ManaNodeKind> ANCHOR_OUTPUTS =
            EnumSet.of(ManaNodeKind.STORAGE);

    /** Identifies a node across dimensions. */
    public static final class NodeKey {

        private final int dimension;
        private final Pos pos;

        public NodeKey(int dimension, Pos pos) {
            this.dimension = dimension;
            this.pos = pos;
        }

        public int dimension() {
            return dimension;
        }

        public Pos pos() {
            return pos;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof NodeKey)) {
                return false;
            }
            NodeKey key = (NodeKey) other;
            return dimension == key.dimension && pos.equals(key.pos);
        }

        @Override
        public int hashCode() {
            return 31 * dimension + pos.hashCode();
        }
    }

    private static final class Edge {

        private final NodeKey from;
        private final NodeKey to;

        private Edge(NodeKey from, NodeKey to) {
            this.from = from;
            this.to = to;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof Edge)) {
                return false;
            }
            Edge edge = (Edge) other;
            return from.equals(edge.from) && to.equals(edge.to);
        }

        @Override
        public int hashCode() {
            return 31 * from.hashCode() + to.hashCode();
        }
    }

    public enum Result {
        OK,
        ALREADY,
        CONFLICT,
        INVALID_PAIR,
        INVALID_NODE,
        SAME_NODE
    }

    public static final class ConnectResult {

        private final Result result;
        private final String firstName;
        private final String secondName;

        private ConnectResult(Result result, String firstName, String secondName) {
            this.result = result;
            this.firstName = firstName;
            this.secondName = secondName;
        }

        public Result result() {
            return result;
        }

        public String firstName() {
            return firstName;
        }

        public String secondName() {
            return secondName;
        }
    }

    private final Map<NodeKey, ManaNodeKind> kinds = new HashMap<NodeKey, ManaNodeKind>();
    private final Map<NodeKey, Set<NodeKey>> outgoing = new HashMap<NodeKey, Set<NodeKey>>();
    private final Map<NodeKey, Set<NodeKey>> incoming = new HashMap<NodeKey, Set<NodeKey>>();
    private final Set<Edge> autoEdges = new HashSet<Edge>();
    private final Map<NodeKey, String> names = new HashMap<NodeKey, String>();
    private final Map<String, Long> buffers = new HashMap<String, Long>();

    public ManaNetworkData() {
        this(DATA_NAME);
    }

    public ManaNetworkData(String name) {
        super(name);
    }

    public static ManaNetworkData get(World world) {
        if (world == null || world.isRemote || MinecraftServer.getServer() == null) {
            return null;
        }
        World overworld = MinecraftServer.getServer().worldServerForDimension(0);
        if (overworld == null) {
            return null;
        }
        MapStorage storage = overworld.mapStorage;
        ManaNetworkData data = (ManaNetworkData) storage.loadData(
                ManaNetworkData.class, DATA_NAME);
        if (data == null) {
            data = new ManaNetworkData(DATA_NAME);
            storage.setData(DATA_NAME, data);
        }
        return data;
    }

    // ---- persistence ----

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        kinds.clear();
        outgoing.clear();
        incoming.clear();
        autoEdges.clear();
        names.clear();
        buffers.clear();

        NBTTagList nodeTags = tag.getTagList(TAG_NODES, 10);
        for (int index = 0; index < nodeTags.tagCount(); index++) {
            NBTTagCompound entry = nodeTags.getCompoundTagAt(index);
            NodeKey key = readKey(entry, TAG_DIMENSION, TAG_POS);
            ManaNodeKind kind = readKind(entry.getString(TAG_KIND));
            if (key == null || kind == null) {
                continue;
            }
            kinds.put(key, kind);
            String name = entry.getString(TAG_NETWORK);
            if (!name.isEmpty()) {
                names.put(key, name);
            }
        }

        NBTTagList edgeTags = tag.getTagList(TAG_EDGES, 10);
        for (int index = 0; index < edgeTags.tagCount(); index++) {
            NBTTagCompound entry = edgeTags.getCompoundTagAt(index);
            NodeKey from = readKey(entry, TAG_FROM_DIMENSION, TAG_FROM_POS);
            NodeKey to = readKey(entry, TAG_TO_DIMENSION, TAG_TO_POS);
            if (from != null && to != null && kinds.containsKey(from) && kinds.containsKey(to)) {
                addEdge(from, to, entry.getBoolean(TAG_AUTO));
            }
        }

        NBTTagList bufferTags = tag.getTagList(TAG_BUFFERS, 10);
        for (int index = 0; index < bufferTags.tagCount(); index++) {
            NBTTagCompound entry = bufferTags.getCompoundTagAt(index);
            String name = entry.getString(TAG_NAME);
            if (!name.isEmpty()) {
                buffers.put(name, Math.max(0L, entry.getLong(TAG_MANA)));
            }
        }

        rebuild();
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        NBTTagList nodeTags = new NBTTagList();
        for (Map.Entry<NodeKey, ManaNodeKind> entry : kinds.entrySet()) {
            NBTTagCompound node = new NBTTagCompound();
            writeKey(node, entry.getKey(), TAG_DIMENSION, TAG_POS);
            node.setString(TAG_KIND, entry.getValue().name());
            String name = names.get(entry.getKey());
            if (name != null) {
                node.setString(TAG_NETWORK, name);
            }
            nodeTags.appendTag(node);
        }
        tag.setTag(TAG_NODES, nodeTags);

        NBTTagList edgeTags = new NBTTagList();
        for (Map.Entry<NodeKey, Set<NodeKey>> entry : outgoing.entrySet()) {
            for (NodeKey target : entry.getValue()) {
                NBTTagCompound edge = new NBTTagCompound();
                writeKey(edge, entry.getKey(), TAG_FROM_DIMENSION, TAG_FROM_POS);
                writeKey(edge, target, TAG_TO_DIMENSION, TAG_TO_POS);
                edge.setBoolean(TAG_AUTO, autoEdges.contains(new Edge(entry.getKey(), target)));
                edgeTags.appendTag(edge);
            }
        }
        tag.setTag(TAG_EDGES, edgeTags);

        NBTTagList bufferTags = new NBTTagList();
        for (Map.Entry<String, Long> entry : buffers.entrySet()) {
            NBTTagCompound buffer = new NBTTagCompound();
            buffer.setString(TAG_NAME, entry.getKey());
            buffer.setLong(TAG_MANA, entry.getValue());
            bufferTags.appendTag(buffer);
        }
        tag.setTag(TAG_BUFFERS, bufferTags);
    }

    private static NodeKey readKey(NBTTagCompound tag, String dimensionKey, String posKey) {
        if (!tag.hasKey(dimensionKey) || !tag.hasKey(posKey)) {
            return null;
        }
        return new NodeKey(tag.getInteger(dimensionKey), Pos.of(tag.getLong(posKey)));
    }

    private static void writeKey(NBTTagCompound tag, NodeKey key, String dimensionKey,
                                 String posKey) {
        tag.setInteger(dimensionKey, key.dimension);
        tag.setLong(posKey, key.pos.asLong());
    }

    private static ManaNodeKind readKind(String value) {
        try {
            return ManaNodeKind.valueOf(value);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    // ---- lookups ----

    public String networkName(World world, Pos pos) {
        return names.get(key(world, pos));
    }

    public long buffer(World world, Pos pos) {
        String name = names.get(key(world, pos));
        if (name == null) {
            return 0L;
        }
        Long value = buffers.get(name);
        return value == null ? 0L : value;
    }

    public long deposit(World world, Pos pos, long amount, long capacity) {
        if (amount <= 0L) {
            return 0L;
        }
        String name = names.get(key(world, pos));
        if (name == null) {
            return 0L;
        }
        Long boxed = buffers.get(name);
        long current = boxed == null ? 0L : boxed;
        long accepted = Math.min(amount, Math.max(0L, capacity - current));
        if (accepted <= 0L) {
            return 0L;
        }
        buffers.put(name, current + accepted);
        markDirty();
        return accepted;
    }

    public long withdraw(World world, Pos pos, long amount) {
        if (amount <= 0L) {
            return 0L;
        }
        String name = names.get(key(world, pos));
        if (name == null) {
            return 0L;
        }
        Long boxed = buffers.get(name);
        long current = boxed == null ? 0L : boxed;
        long withdrawn = Math.min(amount, current);
        if (withdrawn <= 0L) {
            return 0L;
        }
        buffers.put(name, current - withdrawn);
        markDirty();
        return withdrawn;
    }

    public List<Pos> wellOutputs(World world, Pos pos) {
        return neighbors(world, pos, outgoing, WELL_OUTPUTS);
    }

    public List<Pos> beaconInputs(World world, Pos pos) {
        return neighbors(world, pos, incoming, BEACON_INPUTS);
    }

    public List<Pos> anchorOutputs(World world, Pos pos) {
        return neighbors(world, pos, outgoing, ANCHOR_OUTPUTS);
    }

    private List<Pos> neighbors(World world, Pos pos, Map<NodeKey, Set<NodeKey>> side,
                                Set<ManaNodeKind> filter) {
        NodeKey key = key(world, pos);
        Set<NodeKey> targets = side.get(key);
        if (targets == null || targets.isEmpty()) {
            return new ArrayList<Pos>();
        }
        List<Pos> result = new ArrayList<Pos>();
        for (NodeKey target : new ArrayList<NodeKey>(targets)) {
            if (target.dimension != key.dimension) {
                continue;
            }
            ManaNodeKind kind = kinds.get(target);
            if (kind == null || !filter.contains(kind)) {
                continue;
            }
            if (kind == ManaNodeKind.STORAGE && !storageValid(world, target)) {
                continue;
            }
            result.add(target.pos);
        }
        return result;
    }

    public List<LinkEntry> links(World world, Pos pos) {
        NodeKey key = key(world, pos);
        List<LinkEntry> result = new ArrayList<LinkEntry>();
        for (NodeKey other : new LinkedHashSet<NodeKey>(neighborsOf(key))) {
            if (other.dimension != key.dimension) {
                continue;
            }
            ManaNodeKind kind = kinds.get(other);
            if (kind == null) {
                continue;
            }
            if (kind == ManaNodeKind.STORAGE && !storageValid(world, other)) {
                continue;
            }
            Set<NodeKey> outs = outgoing.get(key);
            Set<NodeKey> ins = incoming.get(key);
            boolean out = outs != null && outs.contains(other);
            boolean in = ins != null && ins.contains(other);
            int direction = out && in ? LinkEntry.PEER : out ? LinkEntry.OUT : LinkEntry.IN;
            boolean auto = autoEdges.contains(new Edge(key, other))
                    || autoEdges.contains(new Edge(other, key));
            result.add(new LinkEntry(other.pos, label(world, other.pos), direction, auto));
        }
        return result;
    }

    private static String label(World world, Pos pos) {
        if (!world.blockExists(pos.x, pos.y, pos.z)) {
            return "?";
        }
        return com.bandybus.infwhat.botania.util.BlockNames.displayName(world, pos.x, pos.y, pos.z);
    }

    public List<Pos> neighborPositions(World world, Pos pos) {
        NodeKey key = key(world, pos);
        LinkedHashSet<Pos> result = new LinkedHashSet<Pos>();
        for (NodeKey other : neighborsOf(key)) {
            if (other.dimension == key.dimension) {
                result.add(other.pos);
            }
        }
        return new ArrayList<Pos>(result);
    }

    public void validateLinks(World world, Pos pos) {
        NodeKey key = key(world, pos);
        for (NodeKey other : new ArrayList<NodeKey>(neighborsOf(key))) {
            if (other.dimension == key.dimension
                    && kinds.get(other) == ManaNodeKind.STORAGE) {
                storageValid(world, other);
            }
        }
    }

    public static void writeLinks(NBTTagCompound tag, World world, Pos pos) {
        ManaNetworkData data = get(world);
        if (data != null) {
            writePositions(tag, TAG_CLIENT_LINKS, data.neighborPositions(world, pos));
        }
    }

    public static List<Pos> readLinks(NBTTagCompound tag) {
        return readPositions(tag, TAG_CLIENT_LINKS);
    }

    public static void writePositions(NBTTagCompound tag, String key, List<Pos> positions) {
        NBTTagList list = new NBTTagList();
        for (Pos pos : positions) {
            NBTTagCompound entry = new NBTTagCompound();
            entry.setLong("P", pos.asLong());
            list.appendTag(entry);
        }
        tag.setTag(key, list);
    }

    public static List<Pos> readPositions(NBTTagCompound tag, String key) {
        List<Pos> result = new ArrayList<Pos>();
        NBTTagList list = tag.getTagList(key, 10);
        for (int index = 0; index < list.tagCount(); index++) {
            result.add(Pos.of(list.getCompoundTagAt(index).getLong("P")));
        }
        return result;
    }

    // ---- topology ----

    public static ManaNodeKind kindAt(World world, Pos pos) {
        TileEntity tileEntity = world.getTileEntity(pos.x, pos.y, pos.z);
        if (tileEntity instanceof ManaWellTileEntity) {
            return ManaNodeKind.WELL;
        }
        if (tileEntity instanceof ManaBeaconTileEntity) {
            return ManaNodeKind.BEACON;
        }
        if (tileEntity instanceof ManaAnchorTileEntity) {
            return ManaNodeKind.ANCHOR;
        }
        if (ManaReceiverHelper.find(world, pos) != null) {
            return ManaNodeKind.STORAGE;
        }
        return null;
    }

    public void register(World world, Pos pos, ManaNodeKind kind) {
        NodeKey key = key(world, pos);
        ManaNodeKind previous = kinds.get(key);
        if (previous == kind && names.containsKey(key)) {
            return;
        }
        ensureNode(key, kind);
        rebuild();
        markDirty();
    }

    public void removeNode(World world, Pos pos) {
        NodeKey key = key(world, pos);
        if (!kinds.containsKey(key)) {
            return;
        }
        Set<NodeKey> affected = neighborsOf(key);
        kinds.remove(key);
        removeEdges(key);
        rebuild();
        pruneIsolatedStorage(null, null);
        markDirty();
        refresh(world, affected);
    }

    public ConnectResult connect(World world, Pos first, Pos second) {
        return link(world, first, second, false);
    }

    public ConnectResult autoConnect(World world, Pos first, Pos second) {
        return link(world, first, second, true);
    }

    private ConnectResult link(World world, Pos first, Pos second, boolean auto) {
        int dimension = world.provider.dimensionId;
        NodeKey a = new NodeKey(dimension, first);
        NodeKey b = new NodeKey(dimension, second);
        if (a.equals(b)) {
            return new ConnectResult(Result.SAME_NODE, null, null);
        }
        ManaNodeKind kindA = kindAt(world, first);
        ManaNodeKind kindB = kindAt(world, second);
        if (kindA == null || kindB == null) {
            return new ConnectResult(Result.INVALID_NODE, null, null);
        }
        List<Edge> edges = planEdges(a, b, kindA, kindB);
        if (edges == null) {
            return new ConnectResult(Result.INVALID_PAIR, null, null);
        }

        boolean nodesChanged = ensureNode(a, kindA);
        nodesChanged |= ensureNode(b, kindB);
        rebuild();
        pruneIsolatedStorage(a, b);
        if (nodesChanged) {
            markDirty();
        }

        String nameA = names.get(a);
        String nameB = names.get(b);
        if (auto && nameB != null && countWells(nameB) > 0) {
            return new ConnectResult(Result.CONFLICT, nameA, nameB);
        }
        if (nameA != null && nameB != null && !nameA.equals(nameB)
                && countWells(nameA) > 0 && countWells(nameB) > 0) {
            return new ConnectResult(Result.CONFLICT, nameA, nameB);
        }

        boolean changed = false;
        for (Edge edge : edges) {
            changed |= addEdge(edge.from, edge.to, auto);
        }
        if (!changed) {
            return new ConnectResult(Result.ALREADY, nameA, nameB);
        }
        rebuild();
        markDirty();
        refresh(world, java.util.Arrays.asList(a, b));
        return new ConnectResult(Result.OK, names.get(a), names.get(b));
    }

    public boolean isAutoEdge(World world, Pos first, Pos second) {
        return autoEdges.contains(new Edge(key(world, first), key(world, second)));
    }

    public boolean hasDirectEdge(World world, Pos first, Pos second) {
        Set<NodeKey> firstOut = outgoing.get(key(world, first));
        if (firstOut != null && firstOut.contains(key(world, second))) {
            return true;
        }
        Set<NodeKey> secondOut = outgoing.get(key(world, second));
        return secondOut != null && secondOut.contains(key(world, first));
    }

    public boolean autoDisconnect(World world, Pos first, Pos second) {
        NodeKey a = key(world, first);
        NodeKey b = key(world, second);
        boolean removed = false;
        if (autoEdges.contains(new Edge(a, b))) {
            removed |= removeEdge(a, b);
        }
        if (autoEdges.contains(new Edge(b, a))) {
            removed |= removeEdge(b, a);
        }
        if (!removed) {
            return false;
        }
        rebuild();
        pruneIsolatedStorage(null, null);
        markDirty();
        refresh(world, java.util.Arrays.asList(a, b));
        return true;
    }

    public boolean disconnect(World world, Pos first, Pos second) {
        NodeKey a = key(world, first);
        NodeKey b = key(world, second);
        boolean removed = removeEdge(a, b);
        removed |= removeEdge(b, a);
        if (!removed) {
            return false;
        }
        rebuild();
        pruneIsolatedStorage(null, null);
        markDirty();
        refresh(world, java.util.Arrays.asList(a, b));
        return true;
    }

    private boolean removeEdge(NodeKey from, NodeKey to) {
        Set<NodeKey> outs = outgoing.get(from);
        if (outs == null || !outs.remove(to)) {
            return false;
        }
        autoEdges.remove(new Edge(from, to));
        Set<NodeKey> ins = incoming.get(to);
        if (ins != null) {
            ins.remove(from);
            if (ins.isEmpty()) {
                incoming.remove(to);
            }
        }
        if (outs.isEmpty()) {
            outgoing.remove(from);
        }
        return true;
    }

    private static List<Edge> planEdges(NodeKey a, NodeKey b,
                                        ManaNodeKind kindA, ManaNodeKind kindB) {
        if (kindA == ManaNodeKind.BEACON && kindB == ManaNodeKind.BEACON) {
            return java.util.Arrays.asList(new Edge(a, b), new Edge(b, a));
        }
        if (kindA == ManaNodeKind.WELL && kindB == ManaNodeKind.BEACON) {
            return Collections.singletonList(new Edge(a, b));
        }
        if (kindA == ManaNodeKind.BEACON && kindB == ManaNodeKind.WELL) {
            return Collections.singletonList(new Edge(b, a));
        }
        if (kindA == ManaNodeKind.WELL && kindB == ManaNodeKind.STORAGE) {
            return Collections.singletonList(new Edge(a, b));
        }
        if (kindA == ManaNodeKind.STORAGE && kindB == ManaNodeKind.WELL) {
            return Collections.singletonList(new Edge(b, a));
        }
        if (kindA == ManaNodeKind.BEACON && kindB == ManaNodeKind.ANCHOR) {
            return Collections.singletonList(new Edge(a, b));
        }
        if (kindA == ManaNodeKind.ANCHOR && kindB == ManaNodeKind.BEACON) {
            return Collections.singletonList(new Edge(b, a));
        }
        if (kindA == ManaNodeKind.BEACON && kindB == ManaNodeKind.STORAGE) {
            return Collections.singletonList(new Edge(b, a));
        }
        if (kindA == ManaNodeKind.STORAGE && kindB == ManaNodeKind.BEACON) {
            return Collections.singletonList(new Edge(a, b));
        }
        if (kindA == ManaNodeKind.ANCHOR && kindB == ManaNodeKind.STORAGE) {
            return Collections.singletonList(new Edge(a, b));
        }
        if (kindA == ManaNodeKind.STORAGE && kindB == ManaNodeKind.ANCHOR) {
            return Collections.singletonList(new Edge(b, a));
        }
        return null;
    }

    private boolean ensureNode(NodeKey key, ManaNodeKind kind) {
        ManaNodeKind previous = kinds.put(key, kind);
        if (previous == kind) {
            return false;
        }
        if (previous != null) {
            removeEdges(key);
        }
        return true;
    }

    private int countWells(String name) {
        int count = 0;
        for (Map.Entry<NodeKey, ManaNodeKind> entry : kinds.entrySet()) {
            if (entry.getValue() == ManaNodeKind.WELL && name.equals(names.get(entry.getKey()))) {
                count++;
            }
        }
        return count;
    }

    private boolean addEdge(NodeKey from, NodeKey to, boolean auto) {
        Set<NodeKey> outs = outgoing.get(from);
        if (outs == null) {
            outs = new HashSet<NodeKey>();
            outgoing.put(from, outs);
        }
        boolean added = outs.add(to);
        Set<NodeKey> ins = incoming.get(to);
        if (ins == null) {
            ins = new HashSet<NodeKey>();
            incoming.put(to, ins);
        }
        ins.add(from);
        boolean changed = added;
        Edge edge = new Edge(from, to);
        if (auto) {
            if (added) {
                changed |= autoEdges.add(edge);
            }
        } else {
            changed |= autoEdges.remove(edge);
        }
        return changed;
    }

    private void removeEdges(NodeKey key) {
        Set<NodeKey> outs = outgoing.remove(key);
        if (outs != null) {
            for (NodeKey target : outs) {
                autoEdges.remove(new Edge(key, target));
                Set<NodeKey> in = incoming.get(target);
                if (in != null) {
                    in.remove(key);
                    if (in.isEmpty()) {
                        incoming.remove(target);
                    }
                }
            }
        }
        Set<NodeKey> ins = incoming.remove(key);
        if (ins != null) {
            for (NodeKey source : ins) {
                autoEdges.remove(new Edge(source, key));
                Set<NodeKey> out = outgoing.get(source);
                if (out != null) {
                    out.remove(key);
                    if (out.isEmpty()) {
                        outgoing.remove(source);
                    }
                }
            }
        }
    }

    private Set<NodeKey> neighborsOf(NodeKey key) {
        Set<NodeKey> result = new LinkedHashSet<NodeKey>();
        Set<NodeKey> outs = outgoing.get(key);
        if (outs != null) {
            result.addAll(outs);
        }
        Set<NodeKey> ins = incoming.get(key);
        if (ins != null) {
            result.addAll(ins);
        }
        return result;
    }

    private boolean storageValid(World world, NodeKey key) {
        if (key.dimension != world.provider.dimensionId) {
            return true;
        }
        Pos pos = key.pos;
        if (!world.blockExists(pos.x, pos.y, pos.z)) {
            return true;
        }
        if (ManaReceiverHelper.find(world, pos) != null) {
            return true;
        }
        removeNode(world, pos);
        return false;
    }

    private void refresh(World world, Collection<NodeKey> keys) {
        int dimension = world.provider.dimensionId;
        for (NodeKey key : keys) {
            if (key.dimension != dimension || !world.blockExists(key.pos.x, key.pos.y, key.pos.z)) {
                continue;
            }
            TileEntity tileEntity = world.getTileEntity(key.pos.x, key.pos.y, key.pos.z);
            if (tileEntity instanceof ManaWellTileEntity
                    || tileEntity instanceof ManaBeaconTileEntity
                    || tileEntity instanceof ManaAnchorTileEntity) {
                world.markBlockForUpdate(key.pos.x, key.pos.y, key.pos.z);
            }
        }
    }

    private void pruneIsolatedStorage(NodeKey keepA, NodeKey keepB) {
        boolean changed = false;
        Iterator<Map.Entry<NodeKey, ManaNodeKind>> iterator = kinds.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<NodeKey, ManaNodeKind> entry = iterator.next();
            NodeKey key = entry.getKey();
            if (entry.getValue() != ManaNodeKind.STORAGE
                    || key.equals(keepA) || key.equals(keepB)) {
                continue;
            }
            if (!outgoing.containsKey(key) && !incoming.containsKey(key)) {
                iterator.remove();
                names.remove(key);
                changed = true;
            }
        }
        if (changed) {
            rebuild();
        }
    }

    private static NodeKey key(World world, Pos pos) {
        return new NodeKey(world.provider.dimensionId, pos);
    }

    // ---- naming ----

    private List<Set<NodeKey>> components() {
        Set<NodeKey> remaining = new HashSet<NodeKey>(kinds.keySet());
        List<Set<NodeKey>> result = new ArrayList<Set<NodeKey>>();
        while (!remaining.isEmpty()) {
            Iterator<NodeKey> iterator = remaining.iterator();
            NodeKey start = iterator.next();
            iterator.remove();
            Set<NodeKey> component = new HashSet<NodeKey>();
            Deque<NodeKey> queue = new ArrayDeque<NodeKey>();
            queue.add(start);
            component.add(start);
            while (!queue.isEmpty()) {
                NodeKey current = queue.poll();
                for (NodeKey neighbor : neighborsOf(current)) {
                    if (remaining.remove(neighbor)) {
                        component.add(neighbor);
                        queue.add(neighbor);
                    }
                }
            }
            result.add(component);
        }
        return result;
    }

    private void rebuild() {
        List<Set<NodeKey>> components = components();
        List<Map<String, Integer>> votes = new ArrayList<Map<String, Integer>>(components.size());
        for (Set<NodeKey> component : components) {
            Map<String, Integer> count = new HashMap<String, Integer>();
            for (NodeKey key : component) {
                String name = names.get(key);
                if (name != null) {
                    Integer previous = count.get(name);
                    count.put(name, previous == null ? 1 : previous + 1);
                }
            }
            votes.add(count);
        }

        String[] chosen = new String[components.size()];
        for (int index = 0; index < components.size(); index++) {
            chosen[index] = pickMost(votes.get(index));
        }

        Map<String, List<Integer>> byName = new HashMap<String, List<Integer>>();
        for (int index = 0; index < chosen.length; index++) {
            if (chosen[index] != null) {
                List<Integer> indexes = byName.get(chosen[index]);
                if (indexes == null) {
                    indexes = new ArrayList<Integer>();
                    byName.put(chosen[index], indexes);
                }
                indexes.add(index);
            }
        }
        for (List<Integer> indexes : byName.values()) {
            if (indexes.size() <= 1) {
                continue;
            }
            int keep = pickLargest(components, indexes);
            for (Integer index : indexes) {
                if (index != keep) {
                    chosen[index] = null;
                }
            }
        }

        Set<String> used = new HashSet<String>();
        for (String name : chosen) {
            if (name != null) {
                used.add(name);
            }
        }

        Map<NodeKey, String> newNames = new HashMap<NodeKey, String>();
        Map<String, Long> newBuffers = new HashMap<String, Long>();
        for (int index = 0; index < components.size(); index++) {
            String name = chosen[index];
            if (name == null) {
                name = freshName(used);
                used.add(name);
                newBuffers.put(name, 0L);
            } else {
                long total = 0L;
                for (String voted : votes.get(index).keySet()) {
                    if (!voted.equals(name) && used.contains(voted)) {
                        continue;
                    }
                    Long value = buffers.get(voted);
                    total += value == null ? 0L : value;
                }
                newBuffers.put(name, total);
            }
            for (NodeKey key : components.get(index)) {
                newNames.put(key, name);
            }
        }
        names.clear();
        names.putAll(newNames);
        buffers.clear();
        buffers.putAll(newBuffers);
    }

    private static int pickLargest(List<Set<NodeKey>> components, List<Integer> indexes) {
        int keep = indexes.get(0);
        int bestSize = -1;
        int ties = 0;
        for (Integer boxed : indexes) {
            int size = components.get(boxed).size();
            if (size > bestSize) {
                bestSize = size;
                keep = boxed;
                ties = 1;
            } else if (size == bestSize) {
                ties++;
                if (ThreadLocalRandom.current().nextInt(ties) == 0) {
                    keep = boxed;
                }
            }
        }
        return keep;
    }

    private static String pickMost(Map<String, Integer> votes) {
        String best = null;
        int bestCount = 0;
        int ties = 0;
        for (Map.Entry<String, Integer> entry : votes.entrySet()) {
            int count = entry.getValue();
            if (count > bestCount) {
                best = entry.getKey();
                bestCount = count;
                ties = 1;
            } else if (count == bestCount) {
                ties++;
                if (ThreadLocalRandom.current().nextInt(ties) == 0) {
                    best = entry.getKey();
                }
            }
        }
        return best;
    }

    private static String freshName(Set<String> used) {
        for (int attempt = 0; attempt < 1000; attempt++) {
            String name = String.format("Channel-%06X",
                    ThreadLocalRandom.current().nextInt(0x1000000));
            if (!used.contains(name)) {
                return name;
            }
        }
        return "Channel-" + UUID.randomUUID().toString().substring(0, 6);
    }
}
