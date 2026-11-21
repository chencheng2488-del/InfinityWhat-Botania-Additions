package com.bandybus.infwhat.botania.grid;

import com.bandybus.infwhat.botania.block.entity.ManaAnchorBlockEntity;
import com.bandybus.infwhat.botania.block.entity.ManaBeaconBlockEntity;
import com.bandybus.infwhat.botania.block.entity.ManaWellBlockEntity;
import com.bandybus.infwhat.botania.device.LinkEntry;
import com.bandybus.infwhat.botania.device.ManaNodeKind;
import com.bandybus.infwhat.botania.util.ManaReceiverHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
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
 *
 * <p>Merging keeps the name of the component with more nodes (a coin flip on a tie). Splitting
 * keeps the large component and hands fresh names to the smaller ones.
 */
public final class ManaNetworkData extends SavedData {

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
    public record NodeKey(ResourceLocation dimension, BlockPos pos) {
        public NodeKey {
            pos = pos.immutable();
        }
    }

    /** One directed link: {@code from} pushes mana towards {@code to}. */
    private record Edge(NodeKey from, NodeKey to) {
    }

    public enum Result {
        OK,
        ALREADY,
        CONFLICT,
        INVALID_PAIR,
        INVALID_NODE,
        SAME_NODE
    }

    public record ConnectResult(Result result, @Nullable String firstName, @Nullable String secondName) {
    }

    private final Map<NodeKey, ManaNodeKind> kinds = new HashMap<>();
    private final Map<NodeKey, Set<NodeKey>> outgoing = new HashMap<>();
    private final Map<NodeKey, Set<NodeKey>> incoming = new HashMap<>();
    private final Set<Edge> autoEdges = new HashSet<>();
    private final Map<NodeKey, String> names = new HashMap<>();
    private final Map<String, Long> buffers = new HashMap<>();

    public static ManaNetworkData get(ServerLevel level) {
        ServerLevel overworld = level.getServer().overworld();
        return overworld.getDataStorage().computeIfAbsent(
                ManaNetworkData::load, ManaNetworkData::new, DATA_NAME);
    }

    // ---- persistence ----

    private static ManaNetworkData load(CompoundTag tag) {
        ManaNetworkData data = new ManaNetworkData();

        ListTag nodeTags = tag.getList(TAG_NODES, Tag.TAG_COMPOUND);
        for (int index = 0; index < nodeTags.size(); index++) {
            CompoundTag entry = nodeTags.getCompound(index);
            NodeKey key = readKey(entry, TAG_DIMENSION, TAG_POS);
            ManaNodeKind kind = readKind(entry.getString(TAG_KIND));
            if (key == null || kind == null) {
                continue;
            }
            data.kinds.put(key, kind);
            String name = entry.getString(TAG_NETWORK);
            if (!name.isEmpty()) {
                data.names.put(key, name);
            }
        }

        ListTag edgeTags = tag.getList(TAG_EDGES, Tag.TAG_COMPOUND);
        for (int index = 0; index < edgeTags.size(); index++) {
            CompoundTag entry = edgeTags.getCompound(index);
            NodeKey from = readKey(entry, TAG_FROM_DIMENSION, TAG_FROM_POS);
            NodeKey to = readKey(entry, TAG_TO_DIMENSION, TAG_TO_POS);
            if (from != null && to != null && data.kinds.containsKey(from) && data.kinds.containsKey(to)) {
                data.addEdge(from, to, entry.getBoolean(TAG_AUTO));
            }
        }

        ListTag bufferTags = tag.getList(TAG_BUFFERS, Tag.TAG_COMPOUND);
        for (int index = 0; index < bufferTags.size(); index++) {
            CompoundTag entry = bufferTags.getCompound(index);
            String name = entry.getString(TAG_NAME);
            if (!name.isEmpty()) {
                data.buffers.put(name, Math.max(0L, entry.getLong(TAG_MANA)));
            }
        }

        data.rebuild();
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag nodeTags = new ListTag();
        for (Map.Entry<NodeKey, ManaNodeKind> entry : kinds.entrySet()) {
            CompoundTag node = new CompoundTag();
            writeKey(node, entry.getKey(), TAG_DIMENSION, TAG_POS);
            node.putString(TAG_KIND, entry.getValue().name());
            String name = names.get(entry.getKey());
            if (name != null) {
                node.putString(TAG_NETWORK, name);
            }
            nodeTags.add(node);
        }
        tag.put(TAG_NODES, nodeTags);

        ListTag edgeTags = new ListTag();
        for (Map.Entry<NodeKey, Set<NodeKey>> entry : outgoing.entrySet()) {
            for (NodeKey target : entry.getValue()) {
                CompoundTag edge = new CompoundTag();
                writeKey(edge, entry.getKey(), TAG_FROM_DIMENSION, TAG_FROM_POS);
                writeKey(edge, target, TAG_TO_DIMENSION, TAG_TO_POS);
                edge.putBoolean(TAG_AUTO, autoEdges.contains(new Edge(entry.getKey(), target)));
                edgeTags.add(edge);
            }
        }
        tag.put(TAG_EDGES, edgeTags);

        ListTag bufferTags = new ListTag();
        for (Map.Entry<String, Long> entry : buffers.entrySet()) {
            CompoundTag buffer = new CompoundTag();
            buffer.putString(TAG_NAME, entry.getKey());
            buffer.putLong(TAG_MANA, entry.getValue());
            bufferTags.add(buffer);
        }
        tag.put(TAG_BUFFERS, bufferTags);
        return tag;
    }

    @Nullable
    private static NodeKey readKey(CompoundTag tag, String dimensionKey, String posKey) {
        ResourceLocation dimension = ResourceLocation.tryParse(tag.getString(dimensionKey));
        return dimension == null ? null : new NodeKey(dimension, BlockPos.of(tag.getLong(posKey)));
    }

    private static void writeKey(CompoundTag tag, NodeKey key, String dimensionKey, String posKey) {
        tag.putString(dimensionKey, key.dimension().toString());
        tag.putLong(posKey, key.pos().asLong());
    }

    @Nullable
    private static ManaNodeKind readKind(String value) {
        try {
            return ManaNodeKind.valueOf(value);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    // ---- lookups ----

    @Nullable
    public String networkName(ServerLevel level, BlockPos pos) {
        return names.get(key(level, pos));
    }

    public long buffer(ServerLevel level, BlockPos pos) {
        String name = names.get(key(level, pos));
        return name == null ? 0L : buffers.getOrDefault(name, 0L);
    }

    public long deposit(ServerLevel level, BlockPos pos, long amount, long capacity) {
        if (amount <= 0L) {
            return 0L;
        }
        String name = names.get(key(level, pos));
        if (name == null) {
            return 0L;
        }
        long current = buffers.getOrDefault(name, 0L);
        long accepted = Math.min(amount, Math.max(0L, capacity - current));
        if (accepted <= 0L) {
            return 0L;
        }
        buffers.put(name, current + accepted);
        setDirty();
        return accepted;
    }

    public long withdraw(ServerLevel level, BlockPos pos, long amount) {
        if (amount <= 0L) {
            return 0L;
        }
        String name = names.get(key(level, pos));
        if (name == null) {
            return 0L;
        }
        long current = buffers.getOrDefault(name, 0L);
        long withdrawn = Math.min(amount, current);
        if (withdrawn <= 0L) {
            return 0L;
        }
        buffers.put(name, current - withdrawn);
        setDirty();
        return withdrawn;
    }

    public List<BlockPos> outgoing(ServerLevel level, BlockPos pos, Set<ManaNodeKind> filter) {
        return neighbors(level, pos, outgoing, filter);
    }

    public List<BlockPos> incoming(ServerLevel level, BlockPos pos, Set<ManaNodeKind> filter) {
        return neighbors(level, pos, incoming, filter);
    }

    public List<BlockPos> wellOutputs(ServerLevel level, BlockPos pos) {
        return outgoing(level, pos, WELL_OUTPUTS);
    }

    public List<BlockPos> beaconInputs(ServerLevel level, BlockPos pos) {
        return incoming(level, pos, BEACON_INPUTS);
    }

    public List<BlockPos> anchorOutputs(ServerLevel level, BlockPos pos) {
        return outgoing(level, pos, ANCHOR_OUTPUTS);
    }

    private List<BlockPos> neighbors(ServerLevel level, BlockPos pos,
                                     Map<NodeKey, Set<NodeKey>> side, Set<ManaNodeKind> filter) {
        NodeKey key = key(level, pos);
        Set<NodeKey> targets = side.get(key);
        if (targets == null || targets.isEmpty()) {
            return List.of();
        }
        List<BlockPos> result = new ArrayList<>();
        for (NodeKey target : new ArrayList<>(targets)) {
            if (!target.dimension().equals(key.dimension())) {
                continue;
            }
            ManaNodeKind kind = kinds.get(target);
            if (kind == null || !filter.contains(kind)) {
                continue;
            }
            if (kind == ManaNodeKind.STORAGE && !storageValid(level, target)) {
                continue;
            }
            result.add(target.pos());
        }
        return result;
    }

    /** Both directions collapsed into GUI rows. */
    public List<LinkEntry> links(ServerLevel level, BlockPos pos) {
        NodeKey key = key(level, pos);
        List<LinkEntry> result = new ArrayList<>();
        for (NodeKey other : new LinkedHashSet<>(neighborsOf(key))) {
            if (!other.dimension().equals(key.dimension())) {
                continue;
            }
            ManaNodeKind kind = kinds.get(other);
            if (kind == null) {
                continue;
            }
            if (kind == ManaNodeKind.STORAGE && !storageValid(level, other)) {
                continue;
            }
            boolean out = outgoing.getOrDefault(key, Set.of()).contains(other);
            boolean in = incoming.getOrDefault(key, Set.of()).contains(other);
            int direction = out && in ? LinkEntry.PEER : out ? LinkEntry.OUT : LinkEntry.IN;
            boolean auto = autoEdges.contains(new Edge(key, other))
                    || autoEdges.contains(new Edge(other, key));
            result.add(new LinkEntry(other.pos(), label(level, other.pos()), direction, auto));
        }
        return result;
    }

    private static String label(ServerLevel level, BlockPos pos) {
        if (!level.isLoaded(pos)) {
            return "?";
        }
        return level.getBlockState(pos).getBlock().getName().getString();
    }

    /** Used by the block entities to ship their edges to the client renderer. */
    public List<BlockPos> neighborPositions(ServerLevel level, BlockPos pos) {
        NodeKey key = key(level, pos);
        LinkedHashSet<BlockPos> result = new LinkedHashSet<>();
        for (NodeKey other : neighborsOf(key)) {
            if (other.dimension().equals(key.dimension())) {
                result.add(other.pos());
            }
        }
        return new ArrayList<>(result);
    }

    /**
     * Drops links to storage blocks that have been broken.
     *
     * <p>Storage blocks are not ours, so no block callback removes them. Every device calls this
     * once per tick, even while disabled, so a broken pool does not leave a stale beam behind.
     */
    public void validateLinks(ServerLevel level, BlockPos pos) {
        NodeKey key = key(level, pos);
        for (NodeKey other : new ArrayList<>(neighborsOf(key))) {
            if (other.dimension().equals(key.dimension())
                    && kinds.get(other) == ManaNodeKind.STORAGE) {
                storageValid(level, other);
            }
        }
    }

    public static void writeLinks(CompoundTag tag, ServerLevel level, BlockPos pos) {
        writePositions(tag, TAG_CLIENT_LINKS, get(level).neighborPositions(level, pos));
    }

    public static List<BlockPos> readLinks(CompoundTag tag) {
        return readPositions(tag, TAG_CLIENT_LINKS);
    }

    public static void writePositions(CompoundTag tag, String key, List<BlockPos> positions) {
        ListTag list = new ListTag();
        for (BlockPos pos : positions) {
            list.add(LongTag.valueOf(pos.asLong()));
        }
        tag.put(key, list);
    }

    public static List<BlockPos> readPositions(CompoundTag tag, String key) {
        List<BlockPos> result = new ArrayList<>();
        ListTag list = tag.getList(key, Tag.TAG_LONG);
        for (int index = 0; index < list.size(); index++) {
            result.add(BlockPos.of(((LongTag) list.get(index)).getAsLong()));
        }
        return result;
    }

    // ---- topology ----

    /** Resolves the node kind of a clicked block; {@code null} means it cannot join a network. */
    @Nullable
    public static ManaNodeKind kindAt(ServerLevel level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof ManaWellBlockEntity) {
            return ManaNodeKind.WELL;
        }
        if (blockEntity instanceof ManaBeaconBlockEntity) {
            return ManaNodeKind.BEACON;
        }
        if (blockEntity instanceof ManaAnchorBlockEntity) {
            return ManaNodeKind.ANCHOR;
        }
        if (ManaReceiverHelper.find(level, pos) != null) {
            return ManaNodeKind.STORAGE;
        }
        return null;
    }

    public void register(ServerLevel level, BlockPos pos, ManaNodeKind kind) {
        NodeKey key = key(level, pos);
        ManaNodeKind previous = kinds.get(key);
        if (previous == kind && names.containsKey(key)) {
            return;
        }
        ensureNode(key, kind);
        rebuild();
        setDirty();
    }

    public void removeNode(ServerLevel level, BlockPos pos) {
        NodeKey key = key(level, pos);
        if (!kinds.containsKey(key)) {
            return;
        }
        Set<NodeKey> affected = neighborsOf(key);
        kinds.remove(key);
        removeEdges(key);
        rebuild();
        pruneIsolatedStorage(null, null);
        setDirty();
        refresh(level, affected);
    }

    /**
     * Tries to link two blocks.
     *
     * <p>The mana direction is derived from the block kinds, not from the click order, so selecting
     * a storage block first and a beacon second still creates the legal storage-to-beacon input.
     */
    public ConnectResult connect(ServerLevel level, BlockPos first, BlockPos second) {
        return link(level, first, second, false);
    }

    /**
     * Links a well/anchor to a nearby pool on behalf of the range scan.
     *
     * <p>Conflicts are reported exactly like explicit links, so the scan can skip the occupied pool
     * and try the other pools in range. An existing explicit link is never downgraded to automatic.
     */
    public ConnectResult autoConnect(ServerLevel level, BlockPos first, BlockPos second) {
        return link(level, first, second, true);
    }

    private ConnectResult link(ServerLevel level, BlockPos first, BlockPos second, boolean auto) {
        ResourceLocation dimension = level.dimension().location();
        NodeKey a = new NodeKey(dimension, first);
        NodeKey b = new NodeKey(dimension, second);
        if (a.equals(b)) {
            return new ConnectResult(Result.SAME_NODE, null, null);
        }
        ManaNodeKind kindA = kindAt(level, first);
        ManaNodeKind kindB = kindAt(level, second);
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
            setDirty();
        }

        String nameA = names.get(a);
        String nameB = names.get(b);
        if (auto && nameB != null && countWells(nameB) > 0) {
            // The scan never hooks a nearby pool into a well network; it simply tries another pool.
            return new ConnectResult(Result.CONFLICT, nameA, nameB);
        }
        if (nameA != null && nameB != null && !nameA.equals(nameB)
                && countWells(nameA) > 0 && countWells(nameB) > 0) {
            return new ConnectResult(Result.CONFLICT, nameA, nameB);
        }

        boolean changed = false;
        for (Edge edge : edges) {
            changed |= addEdge(edge.from(), edge.to(), auto);
        }
        if (!changed) {
            return new ConnectResult(Result.ALREADY, nameA, nameB);
        }
        rebuild();
        setDirty();
        refresh(level, List.of(a, b));
        return new ConnectResult(Result.OK, names.get(a), names.get(b));
    }

    public boolean isAutoEdge(ServerLevel level, BlockPos first, BlockPos second) {
        return autoEdges.contains(new Edge(key(level, first), key(level, second)));
    }

    public boolean hasDirectEdge(ServerLevel level, BlockPos first, BlockPos second) {
        return outgoing.getOrDefault(key(level, first), Set.of()).contains(key(level, second))
                || outgoing.getOrDefault(key(level, second), Set.of()).contains(key(level, first));
    }

    /** Removes an edge only when it was created by the range scan. */
    public boolean autoDisconnect(ServerLevel level, BlockPos first, BlockPos second) {
        NodeKey a = key(level, first);
        NodeKey b = key(level, second);
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
        setDirty();
        refresh(level, List.of(a, b));
        return true;
    }

    /**
     * Removes the direct link between two blocks, if any.
     *
     * @return true when at least one directed edge was removed
     */
    public boolean disconnect(ServerLevel level, BlockPos first, BlockPos second) {
        NodeKey a = key(level, first);
        NodeKey b = key(level, second);
        boolean removed = removeEdge(a, b);
        removed |= removeEdge(b, a);
        if (!removed) {
            return false;
        }
        rebuild();
        pruneIsolatedStorage(null, null);
        setDirty();
        refresh(level, List.of(a, b));
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

    @Nullable
    private static List<Edge> planEdges(NodeKey a, NodeKey b, ManaNodeKind kindA, ManaNodeKind kindB) {
        if (kindA == ManaNodeKind.BEACON && kindB == ManaNodeKind.BEACON) {
            return List.of(new Edge(a, b), new Edge(b, a));
        }
        if (kindA == ManaNodeKind.WELL && kindB == ManaNodeKind.BEACON) {
            return List.of(new Edge(a, b));
        }
        if (kindA == ManaNodeKind.BEACON && kindB == ManaNodeKind.WELL) {
            return List.of(new Edge(b, a));
        }
        if (kindA == ManaNodeKind.WELL && kindB == ManaNodeKind.STORAGE) {
            return List.of(new Edge(a, b));
        }
        if (kindA == ManaNodeKind.STORAGE && kindB == ManaNodeKind.WELL) {
            return List.of(new Edge(b, a));
        }
        if (kindA == ManaNodeKind.BEACON && kindB == ManaNodeKind.ANCHOR) {
            return List.of(new Edge(a, b));
        }
        if (kindA == ManaNodeKind.ANCHOR && kindB == ManaNodeKind.BEACON) {
            return List.of(new Edge(b, a));
        }
        if (kindA == ManaNodeKind.BEACON && kindB == ManaNodeKind.STORAGE) {
            return List.of(new Edge(b, a));
        }
        if (kindA == ManaNodeKind.STORAGE && kindB == ManaNodeKind.BEACON) {
            return List.of(new Edge(a, b));
        }
        if (kindA == ManaNodeKind.ANCHOR && kindB == ManaNodeKind.STORAGE) {
            return List.of(new Edge(a, b));
        }
        if (kindA == ManaNodeKind.STORAGE && kindB == ManaNodeKind.ANCHOR) {
            return List.of(new Edge(b, a));
        }
        return null;
    }

    /** Returns true when the node was not already registered with this kind. */
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
        Set<NodeKey> outs = outgoing.computeIfAbsent(from, key -> new HashSet<>());
        boolean added = outs.add(to);
        incoming.computeIfAbsent(to, key -> new HashSet<>()).add(from);
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
        Set<NodeKey> result = new LinkedHashSet<>(outgoing.getOrDefault(key, Set.of()));
        result.addAll(incoming.getOrDefault(key, Set.of()));
        return result;
    }

    /** A storage block stays registered across chunk unloads; only breakage removes it. */
    private boolean storageValid(ServerLevel level, NodeKey key) {
        if (!key.dimension().equals(level.dimension().location())) {
            return true;
        }
        BlockPos pos = key.pos();
        if (!level.isLoaded(pos)) {
            return true;
        }
        if (ManaReceiverHelper.find(level, pos) != null) {
            return true;
        }
        removeNode(level, pos);
        return false;
    }

    /** Refreshes the block-entity payload of our own devices after their edges changed. */
    private void refresh(ServerLevel level, Collection<NodeKey> keys) {
        ResourceLocation dimension = level.dimension().location();
        for (NodeKey key : keys) {
            if (!key.dimension().equals(dimension) || !level.isLoaded(key.pos())) {
                continue;
            }
            BlockEntity blockEntity = level.getBlockEntity(key.pos());
            if (blockEntity instanceof ManaWellBlockEntity
                    || blockEntity instanceof ManaBeaconBlockEntity
                    || blockEntity instanceof ManaAnchorBlockEntity) {
                level.sendBlockUpdated(key.pos(), blockEntity.getBlockState(),
                        blockEntity.getBlockState(), 3);
            }
        }
    }

    /** Removes storage nodes that no longer participate in any link. */
    private void pruneIsolatedStorage(@Nullable NodeKey keepA, @Nullable NodeKey keepB) {
        boolean changed = false;
        Iterator<Map.Entry<NodeKey, ManaNodeKind>> iterator = kinds.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<NodeKey, ManaNodeKind> entry = iterator.next();
            NodeKey key = entry.getKey();
            if (entry.getValue() != ManaNodeKind.STORAGE || key.equals(keepA) || key.equals(keepB)) {
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

    private static NodeKey key(ServerLevel level, BlockPos pos) {
        return new NodeKey(level.dimension().location(), pos);
    }

    // ---- naming ----

    private List<Set<NodeKey>> components() {
        Set<NodeKey> remaining = new HashSet<>(kinds.keySet());
        List<Set<NodeKey>> result = new ArrayList<>();
        while (!remaining.isEmpty()) {
            Iterator<NodeKey> iterator = remaining.iterator();
            NodeKey start = iterator.next();
            iterator.remove();
            Set<NodeKey> component = new HashSet<>();
            Deque<NodeKey> queue = new ArrayDeque<>();
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

    /**
     * Recomputes component names and buffers after a structural change.
     *
     * <p>Each component keeps the name most of its nodes already had. When two components claim the
     * same name (a split), the larger one keeps it and the other receives a fresh name with an
     * empty buffer. This automatically implements "merge keeps the larger name" as well.
     */
    private void rebuild() {
        List<Set<NodeKey>> components = components();
        List<Map<String, Integer>> votes = new ArrayList<>(components.size());
        for (Set<NodeKey> component : components) {
            Map<String, Integer> count = new HashMap<>();
            for (NodeKey key : component) {
                String name = names.get(key);
                if (name != null) {
                    count.merge(name, 1, Integer::sum);
                }
            }
            votes.add(count);
        }

        String[] chosen = new String[components.size()];
        for (int index = 0; index < components.size(); index++) {
            chosen[index] = pickMost(votes.get(index));
        }

        Map<String, List<Integer>> byName = new HashMap<>();
        for (int index = 0; index < chosen.length; index++) {
            if (chosen[index] != null) {
                byName.computeIfAbsent(chosen[index], key -> new ArrayList<>()).add(index);
            }
        }
        for (List<Integer> indexes : byName.values()) {
            if (indexes.size() <= 1) {
                continue;
            }
            int keep = pickLargest(components, indexes);
            for (int index : indexes) {
                if (index != keep) {
                    chosen[index] = null;
                }
            }
        }

        Set<String> used = new HashSet<>();
        for (String name : chosen) {
            if (name != null) {
                used.add(name);
            }
        }

        Map<NodeKey, String> newNames = new HashMap<>();
        Map<String, Long> newBuffers = new HashMap<>();
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
                    total += buffers.getOrDefault(voted, 0L);
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
        for (int index : indexes) {
            int size = components.get(index).size();
            if (size > bestSize) {
                bestSize = size;
                keep = index;
                ties = 1;
            } else if (size == bestSize) {
                ties++;
                if (ThreadLocalRandom.current().nextInt(ties) == 0) {
                    keep = index;
                }
            }
        }
        return keep;
    }

    @Nullable
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
        for (int attempt = 0; attempt < 1_000; attempt++) {
            String name = String.format("Channel-%06X",
                    ThreadLocalRandom.current().nextInt(0x1000000));
            if (!used.contains(name)) {
                return name;
            }
        }
        return "Channel-" + UUID.randomUUID().toString().substring(0, 6);
    }
}
