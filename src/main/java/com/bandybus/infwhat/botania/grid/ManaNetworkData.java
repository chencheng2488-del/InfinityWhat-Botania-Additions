package com.bandybus.infwhat.botania.grid;

import com.bandybus.infwhat.botania.block.tile.ManaAnchorTileEntity;
import com.bandybus.infwhat.botania.block.tile.ManaBeaconTileEntity;
import com.bandybus.infwhat.botania.block.tile.ManaWellTileEntity;
import com.bandybus.infwhat.botania.device.LinkEntry;
import com.bandybus.infwhat.botania.device.ManaNodeKind;
import com.bandybus.infwhat.botania.util.ManaReceiverHelper;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagLong;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;
import vazkii.botania.api.internal.VanillaPacketDispatcher;

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
 *
 * <p>Merging keeps the name of the component with more nodes (a coin flip on a tie). Splitting
 * keeps the large component and hands fresh names to the smaller ones.
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
	private static final int TAG_LONG = 4;

	private static final Set<ManaNodeKind> WELL_OUTPUTS =
			EnumSet.of(ManaNodeKind.BEACON, ManaNodeKind.STORAGE);
	private static final Set<ManaNodeKind> BEACON_INPUTS =
			EnumSet.of(ManaNodeKind.STORAGE);
	private static final Set<ManaNodeKind> ANCHOR_OUTPUTS =
			EnumSet.of(ManaNodeKind.STORAGE);

	/** Identifies a node across dimensions. */
	public static final class NodeKey {

		private final int dimension;
		private final BlockPos pos;

		public NodeKey(int dimension, BlockPos pos) {
			this.dimension = dimension;
			this.pos = pos.toImmutable();
		}

		public int dimension() {
			return dimension;
		}

		public BlockPos pos() {
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

	/** One directed link: {@code from} pushes mana towards {@code to}. */
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

	/** Required by {@link MapStorage#getOrLoadData(Class, String)}. */
	public ManaNetworkData() {
		this(DATA_NAME);
	}

	public ManaNetworkData(String name) {
		super(name);
	}

	public static ManaNetworkData get(World world) {
		if (world == null || world.isRemote) {
			return null;
		}
		MinecraftServer server = world.getMinecraftServer();
		if (server == null) {
			return null;
		}
		World overworld = server.getWorld(0);
		MapStorage storage = overworld.getMapStorage();
		ManaNetworkData data = (ManaNetworkData) storage.getOrLoadData(
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
	public NBTTagCompound writeToNBT(NBTTagCompound tag) {
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
		return tag;
	}

	private static NodeKey readKey(NBTTagCompound tag, String dimensionKey, String posKey) {
		if (!tag.hasKey(dimensionKey) || !tag.hasKey(posKey)) {
			return null;
		}
		return new NodeKey(tag.getInteger(dimensionKey),
				BlockPos.fromLong(tag.getLong(posKey)));
	}

	private static void writeKey(NBTTagCompound tag, NodeKey key, String dimensionKey,
								 String posKey) {
		tag.setInteger(dimensionKey, key.dimension);
		tag.setLong(posKey, key.pos.toLong());
	}

	private static ManaNodeKind readKind(String value) {
		try {
			return ManaNodeKind.valueOf(value);
		} catch (IllegalArgumentException exception) {
			return null;
		}
	}

	// ---- lookups ----

	public String networkName(World world, BlockPos pos) {
		return names.get(key(world, pos));
	}

	public long buffer(World world, BlockPos pos) {
		String name = names.get(key(world, pos));
		return name == null ? 0L : value(buffers.get(name));
	}

	public long deposit(World world, BlockPos pos, long amount, long capacity) {
		if (amount <= 0L) {
			return 0L;
		}
		String name = names.get(key(world, pos));
		if (name == null) {
			return 0L;
		}
		long current = value(buffers.get(name));
		long accepted = Math.min(amount, Math.max(0L, capacity - current));
		if (accepted <= 0L) {
			return 0L;
		}
		buffers.put(name, current + accepted);
		markDirty();
		return accepted;
	}

	public long withdraw(World world, BlockPos pos, long amount) {
		if (amount <= 0L) {
			return 0L;
		}
		String name = names.get(key(world, pos));
		if (name == null) {
			return 0L;
		}
		long current = value(buffers.get(name));
		long withdrawn = Math.min(amount, current);
		if (withdrawn <= 0L) {
			return 0L;
		}
		buffers.put(name, current - withdrawn);
		markDirty();
		return withdrawn;
	}

	public List<BlockPos> outgoing(World world, BlockPos pos, Set<ManaNodeKind> filter) {
		return neighbors(world, pos, outgoing, filter);
	}

	public List<BlockPos> incoming(World world, BlockPos pos, Set<ManaNodeKind> filter) {
		return neighbors(world, pos, incoming, filter);
	}

	public List<BlockPos> wellOutputs(World world, BlockPos pos) {
		return outgoing(world, pos, WELL_OUTPUTS);
	}

	public List<BlockPos> beaconInputs(World world, BlockPos pos) {
		return incoming(world, pos, BEACON_INPUTS);
	}

	public List<BlockPos> anchorOutputs(World world, BlockPos pos) {
		return outgoing(world, pos, ANCHOR_OUTPUTS);
	}

	private List<BlockPos> neighbors(World world, BlockPos pos,
									 Map<NodeKey, Set<NodeKey>> side, Set<ManaNodeKind> filter) {
		NodeKey key = key(world, pos);
		Set<NodeKey> targets = side.get(key);
		if (targets == null || targets.isEmpty()) {
			return Collections.emptyList();
		}
		List<BlockPos> result = new ArrayList<BlockPos>();
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

	/** Both directions collapsed into GUI rows. */
	public List<LinkEntry> links(World world, BlockPos pos) {
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
			boolean out = set(outgoing.get(key)).contains(other);
			boolean in = set(incoming.get(key)).contains(other);
			int direction = out && in ? LinkEntry.PEER : out ? LinkEntry.OUT : LinkEntry.IN;
			boolean auto = autoEdges.contains(new Edge(key, other))
					|| autoEdges.contains(new Edge(other, key));
			result.add(new LinkEntry(other.pos, label(world, other.pos), direction, auto));
		}
		return result;
	}

	private static String label(World world, BlockPos pos) {
		return com.bandybus.infwhat.botania.util.BlockLabels.of(world, pos);
	}

	/** Used by the tile entities to ship their edges to the client renderer. */
	public List<BlockPos> neighborPositions(World world, BlockPos pos) {
		NodeKey key = key(world, pos);
		LinkedHashSet<BlockPos> result = new LinkedHashSet<BlockPos>();
		for (NodeKey other : neighborsOf(key)) {
			if (other.dimension == key.dimension) {
				result.add(other.pos);
			}
		}
		return new ArrayList<BlockPos>(result);
	}

	/**
	 * Drops links to storage blocks that have been broken.
	 *
	 * <p>Storage blocks are not ours, so no block callback removes them. Every device calls this
	 * once per tick, even while disabled, so a broken pool does not leave a stale beam behind.
	 */
	public void validateLinks(World world, BlockPos pos) {
		NodeKey key = key(world, pos);
		for (NodeKey other : new ArrayList<NodeKey>(neighborsOf(key))) {
			if (other.dimension == key.dimension && kinds.get(other) == ManaNodeKind.STORAGE) {
				storageValid(world, other);
			}
		}
	}

	public static void writeLinks(NBTTagCompound tag, World world, BlockPos pos) {
		ManaNetworkData data = get(world);
		List<BlockPos> links = data == null
				? Collections.<BlockPos>emptyList()
				: data.neighborPositions(world, pos);
		writePositions(tag, TAG_CLIENT_LINKS, links);
	}

	public static List<BlockPos> readLinks(NBTTagCompound tag) {
		return readPositions(tag, TAG_CLIENT_LINKS);
	}

	public static void writePositions(NBTTagCompound tag, String key, List<BlockPos> positions) {
		NBTTagList list = new NBTTagList();
		for (BlockPos pos : positions) {
			list.appendTag(new NBTTagLong(pos.toLong()));
		}
		tag.setTag(key, list);
	}

	public static List<BlockPos> readPositions(NBTTagCompound tag, String key) {
		List<BlockPos> result = new ArrayList<BlockPos>();
		NBTTagList list = tag.getTagList(key, TAG_LONG);
		for (int index = 0; index < list.tagCount(); index++) {
			result.add(BlockPos.fromLong(((NBTTagLong) list.get(index)).getLong()));
		}
		return result;
	}

	// ---- topology ----

	/** Resolves the node kind of a clicked block; {@code null} means it cannot join a network. */
	public static ManaNodeKind kindAt(World world, BlockPos pos) {
		TileEntity tileEntity = world.getTileEntity(pos);
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

	public void register(World world, BlockPos pos, ManaNodeKind kind) {
		NodeKey key = key(world, pos);
		ManaNodeKind previous = kinds.get(key);
		if (previous == kind && names.containsKey(key)) {
			return;
		}
		ensureNode(key, kind);
		rebuild();
		markDirty();
	}

	public void removeNode(World world, BlockPos pos) {
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

	/**
	 * Tries to link two blocks.
	 *
	 * <p>The mana direction is derived from the block kinds, not from the click order, so selecting
	 * a storage block first and a beacon second still creates the legal storage-to-beacon input.
	 */
	public ConnectResult connect(World world, BlockPos first, BlockPos second) {
		return link(world, first, second, false);
	}

	/**
	 * Links a well/anchor to a nearby pool on behalf of the range scan.
	 *
	 * <p>Conflicts are reported exactly like explicit links, so the scan can skip the occupied pool
	 * and try the other pools in range. An existing explicit link is never downgraded to automatic.
	 */
	public ConnectResult autoConnect(World world, BlockPos first, BlockPos second) {
		return link(world, first, second, true);
	}

	private ConnectResult link(World world, BlockPos first, BlockPos second, boolean auto) {
		int dimension = world.provider.getDimension();
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
			// The scan never hooks a nearby pool into a well network; it simply tries another pool.
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
		refresh(world, singleton(a, b));
		return new ConnectResult(Result.OK, names.get(a), names.get(b));
	}

	public boolean isAutoEdge(World world, BlockPos first, BlockPos second) {
		return autoEdges.contains(new Edge(key(world, first), key(world, second)));
	}

	public boolean hasDirectEdge(World world, BlockPos first, BlockPos second) {
		return set(outgoing.get(key(world, first))).contains(key(world, second))
				|| set(outgoing.get(key(world, second))).contains(key(world, first));
	}

	/** Removes an edge only when it was created by the range scan. */
	public boolean autoDisconnect(World world, BlockPos first, BlockPos second) {
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
		refresh(world, singleton(a, b));
		return true;
	}

	/**
	 * Removes the direct link between two blocks, if any.
	 *
	 * @return true when at least one directed edge was removed
	 */
	public boolean disconnect(World world, BlockPos first, BlockPos second) {
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
		refresh(world, singleton(a, b));
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

	private static List<Edge> planEdges(NodeKey a, NodeKey b, ManaNodeKind kindA,
										ManaNodeKind kindB) {
		if (kindA == ManaNodeKind.BEACON && kindB == ManaNodeKind.BEACON) {
			return pair(new Edge(a, b), new Edge(b, a));
		}
		if (kindA == ManaNodeKind.WELL && kindB == ManaNodeKind.BEACON) {
			return singleton(new Edge(a, b));
		}
		if (kindA == ManaNodeKind.BEACON && kindB == ManaNodeKind.WELL) {
			return singleton(new Edge(b, a));
		}
		if (kindA == ManaNodeKind.WELL && kindB == ManaNodeKind.STORAGE) {
			return singleton(new Edge(a, b));
		}
		if (kindA == ManaNodeKind.STORAGE && kindB == ManaNodeKind.WELL) {
			return singleton(new Edge(b, a));
		}
		if (kindA == ManaNodeKind.BEACON && kindB == ManaNodeKind.ANCHOR) {
			return singleton(new Edge(a, b));
		}
		if (kindA == ManaNodeKind.ANCHOR && kindB == ManaNodeKind.BEACON) {
			return singleton(new Edge(b, a));
		}
		if (kindA == ManaNodeKind.BEACON && kindB == ManaNodeKind.STORAGE) {
			return singleton(new Edge(b, a));
		}
		if (kindA == ManaNodeKind.STORAGE && kindB == ManaNodeKind.BEACON) {
			return singleton(new Edge(a, b));
		}
		if (kindA == ManaNodeKind.ANCHOR && kindB == ManaNodeKind.STORAGE) {
			return singleton(new Edge(a, b));
		}
		if (kindA == ManaNodeKind.STORAGE && kindB == ManaNodeKind.ANCHOR) {
			return singleton(new Edge(b, a));
		}
		return null;
	}

	private static List<Edge> singleton(Edge edge) {
		List<Edge> result = new ArrayList<Edge>(1);
		result.add(edge);
		return result;
	}

	private static List<Edge> pair(Edge first, Edge second) {
		List<Edge> result = new ArrayList<Edge>(2);
		result.add(first);
		result.add(second);
		return result;
	}

	private static List<NodeKey> singleton(NodeKey first, NodeKey second) {
		List<NodeKey> result = new ArrayList<NodeKey>(2);
		result.add(first);
		result.add(second);
		return result;
	}

	private static Set<NodeKey> set(Set<NodeKey> value) {
		return value == null ? Collections.<NodeKey>emptySet() : value;
	}

	private static long value(Long value) {
		return value == null ? 0L : value;
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
		Set<NodeKey> result = new LinkedHashSet<NodeKey>(set(outgoing.get(key)));
		result.addAll(set(incoming.get(key)));
		return result;
	}

	/** A storage block stays registered across chunk unloads; only breakage removes it. */
	private boolean storageValid(World world, NodeKey key) {
		if (key.dimension != world.provider.getDimension()) {
			return true;
		}
		BlockPos pos = key.pos;
		if (!world.isBlockLoaded(pos)) {
			return true;
		}
		if (ManaReceiverHelper.find(world, pos) != null) {
			return true;
		}
		removeNode(world, pos);
		return false;
	}

	/** Refreshes the tile-entity payload of our own devices after their edges changed. */
	private void refresh(World world, Collection<NodeKey> keys) {
		int dimension = world.provider.getDimension();
		for (NodeKey key : keys) {
			if (key.dimension != dimension || !world.isBlockLoaded(key.pos)) {
				continue;
			}
			TileEntity tileEntity = world.getTileEntity(key.pos);
			if (tileEntity instanceof ManaWellTileEntity
					|| tileEntity instanceof ManaBeaconTileEntity
					|| tileEntity instanceof ManaAnchorTileEntity) {
				VanillaPacketDispatcher.dispatchTEToNearbyPlayers(world, key.pos);
			}
		}
	}

	/** Removes storage nodes that no longer participate in any link. */
	private void pruneIsolatedStorage(NodeKey keepA, NodeKey keepB) {
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

	private static NodeKey key(World world, BlockPos pos) {
		return new NodeKey(world.provider.getDimension(), pos);
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

	/**
	 * Recomputes component names and buffers after a structural change.
	 *
	 * <p>Each component keeps the name most of its nodes already had. When two components claim the
	 * same name (a split), the larger one keeps it and the other receives a fresh name with an
	 * empty buffer. This automatically implements "merge keeps the larger name" as well.
	 */
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
			for (int index : indexes) {
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
					total += value(buffers.get(voted));
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
