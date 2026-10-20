package thaumcraft.api.visnet;

import java.lang.ref.WeakReference;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thaumcraft.api.WorldCoordinates;
import thaumcraft.api.aspects.Aspect;

/** Original root-biased attachment and one-nearest-node-per-root consumption.
 * Raw network amounts are NOT multiplied by100. No graph selection uses RNG.
 * All persistent static references to worlds/nodes are weak; root lifecycle hooks
 * still explicitly clear the complete level/server lifetime. */
public final class VisNetHandler {
    private record WorkbenchGrant(thaumcraft.common.tiles.TileMagicWorkbenchCharger charger, Aspect aspect, int units) {}
    private static final ThreadLocal<WorkbenchGrant> WORKBENCH_GRANT = new ThreadLocal<>();
    private static final class AmuletGrant {
        final thaumcraft.common.tiles.TileVisRelay relay; final Aspect aspect; final int units; boolean used;
        AmuletGrant(thaumcraft.common.tiles.TileVisRelay relay,Aspect aspect,int units){this.relay=relay;this.aspect=aspect;this.units=units;}
    }
    private static final ThreadLocal<AmuletGrant> AMULET_GRANT=new ThreadLocal<>();
    public static void requireAmuletGrant(thaumcraft.common.tiles.TileVisRelay relay,Aspect aspect,int units) {
        AmuletGrant grant=AMULET_GRANT.get();
        if(grant==null||grant.used||grant.relay!=relay||grant.aspect!=aspect||grant.units!=units)
            throw new IllegalStateException("Amulet credit requires executor's exact unused committed grant");
        grant.used=true;
    }

    /** Closed native receive frame: no public callback can forge or replay a paid workbench-wand credit. */
    public static void requireWorkbenchGrant(thaumcraft.common.tiles.TileMagicWorkbenchCharger charger, Aspect aspect, int units) {
        WorkbenchGrant grant = WORKBENCH_GRANT.get();
        if (grant == null || grant.charger() != charger || grant.aspect() != aspect || grant.units() != units) {
            throw new IllegalStateException("Workbench wand credit requires the executor's exact committed grant");
        }
    }
    private static final class AdvancedFurnaceGrant {
        final thaumcraft.common.tiles.TileAlchemyFurnaceAdvanced furnace;
        final Aspect aspect;
        int units;
        boolean accepted;
        AdvancedFurnaceGrant(thaumcraft.common.tiles.TileAlchemyFurnaceAdvanced furnace,Aspect aspect) { this.furnace=furnace; this.aspect=aspect; }
    }
    private static final ThreadLocal<AdvancedFurnaceGrant> ADVANCED_FURNACE_GRANT=new ThreadLocal<>();
    public static void requireAdvancedFurnaceGrant(thaumcraft.common.tiles.TileAlchemyFurnaceAdvanced furnace,Aspect aspect,int units) {
        AdvancedFurnaceGrant grant=ADVANCED_FURNACE_GRANT.get();
        if(grant==null || grant.accepted || grant.furnace!=furnace || grant.aspect!=aspect || grant.units!=units)
            throw new IllegalStateException("Advanced furnace power requires the exact native committed grant");
        grant.accepted=true;
    }
    /** Server-side post-debit beam effect. The packet sender lives in common and is registered at startup. */
    @FunctionalInterface public interface EffectSink { void send(ServerLevel level, BlockPos consumer, BlockPos selectedNode, Aspect aspect); }
    private static final Logger LOG = LoggerFactory.getLogger("Thaumcraft");
    private static volatile EffectSink effectSink = (level, consumer, node, aspect) -> {};
    public static void setEffectSink(EffectSink sink) { effectSink = Objects.requireNonNull(sink, "sink"); }
    private static final List<Network> NETWORKS = new ArrayList<>();
    private static final AtomicLong LIFETIMES = new AtomicLong();
    private static final ThreadLocal<Set<Level>> VALIDATING = ThreadLocal.withInitial(VisNetHandler::identities);
    private static final ThreadLocal<Set<TileVisNode>> CHECKING_NODES = ThreadLocal.withInitial(VisNetHandler::identities);
    private static final ThreadLocal<Set<TileVisNode>> CHECKING_METADATA = ThreadLocal.withInitial(VisNetHandler::identities);
    private static final int DRAINER_CACHE_SIZE = 1024; // Eviction recomputes, NEVER truncates roots or grants.

    private static final class Network {
        final WeakReference<ServerLevel> world;
        final long lifetime = LIFETIMES.incrementAndGet();
        final Map<WorldCoordinates, WeakReference<TileVisNode>> sources = new HashMap<>();
        final Map<WorldCoordinates, WeakReference<TileVisNode>> nodes = new HashMap<>();
        final Map<WorldCoordinates, Observation> observations = new HashMap<>();
        final Map<BlockPos, CachedRoutes> nearby = new LinkedHashMap<>(16, .75F, true) {
            @Override protected boolean removeEldestEntry(Map.Entry<BlockPos, CachedRoutes> eldest) { return size() > DRAINER_CACHE_SIZE; }
        };
        long version;
        volatile boolean consuming;
        boolean changing, closed;
        Network(ServerLevel world) { this.world = new WeakReference<>(world); }
        void changed() { ++version; nearby.clear(); }
    }
    private record Metadata(int range, byte attunement, boolean source) {}
    private record Observation(WeakReference<TileVisNode> node, Metadata metadata, long version, boolean edgeLoaded) {}
    private record Choice(WeakReference<TileVisNode> root, WeakReference<TileVisNode> node) {}
    private record CachedRoutes(long version, List<Choice> choices) {}
    private record Step(TileVisNode node, long version, Metadata metadata) {}
    private record Path(List<Step> steps, TileVisNode root) {}
    private record Consumer(BlockPos pos, BlockState state, BlockEntity owner) {
        boolean current(ServerLevel level) {
            return level.isInWorldBounds(pos) && level.hasChunkAt(pos) && level.getBlockEntity(pos) == owner
                    && level.getBlockState(pos) == state && (owner == null || !owner.isRemoved() && owner.getLevel() == level);
        }
    }

    private VisNetHandler() {}
    private static <T> Set<T> identities() { return Collections.newSetFromMap(new IdentityHashMap<>()); }
    private static synchronized Network network(Level level, boolean create) {
        Network found = null;
        for (Iterator<Network> it = NETWORKS.iterator(); it.hasNext();) {
            Network net = it.next(); ServerLevel value = net.world.get();
            if (value == null) it.remove();
            else if (value == level) found = net;
        }
        if (found == null && create && level instanceof ServerLevel server) { found = new Network(server); NETWORKS.add(found); }
        return found;
    }
    private static synchronized void forget(Network net) { NETWORKS.remove(net); }
    public static boolean isConsumeActive(Level level) { Network net = network(level, false); return net != null && net.consuming; }
    public static long getTopologyVersion(ServerLevel level) { Network net = network(level, false); return net == null ? 0 : net.version; }
    public static long getLevelLifetime(ServerLevel level) { Network net = network(level, false); return net == null ? 0 : net.lifetime; }
    public static List<WeakReference<TileVisNode>> getSources(ServerLevel level) {
        requireThread(level); Network net = network(level, false);
        if (net == null) return List.of();
        List<WeakReference<TileVisNode>> snapshot = new ArrayList<>();
        for (WeakReference<TileVisNode> ref : net.sources.values()) { TileVisNode node = ref.get(); if (node != null) snapshot.add(new WeakReference<>(node)); }
        return List.copyOf(snapshot);
    }

    /** No overridable Level callback runs without a recursion fence. A subclass
     * borrowing a real server is not that server's actual installed world. */
    public static boolean isNativeServerLevel(Level level) {
        if (!(level instanceof ServerLevel server) || level.isClientSide) return false;
        Set<Level> checking = VALIDATING.get();
        if (!checking.add(level)) return false;
        try {
            MinecraftServer owner = server.getServer();
            if (owner == null || !owner.isSameThread()) throw new IllegalStateException("VisNet requires the server thread");
            return WorldCoordinates.isCanonicalDimension(server.dimension()) && owner.getLevel(server.dimension()) == server;
        } finally { checking.remove(level); if (checking.isEmpty()) VALIDATING.remove(); }
    }
    static void requireThread(Level level) {
        if (level == null || level.isClientSide) return;
        if (!(level instanceof ServerLevel server)) throw new IllegalStateException("VisNet requires a native server Level");
        Set<Level> checking = VALIDATING.get();
        if (!checking.add(level)) throw new IllegalStateException("Recursive VisNet world validation");
        try {
            MinecraftServer owner = server.getServer();
            if (owner == null || !owner.isSameThread()) throw new IllegalStateException("VisNet requires the server thread");
        } finally { checking.remove(level); if (checking.isEmpty()) VALIDATING.remove(); }
    }
    public static boolean isValidAspect(Aspect aspect) {
        if (aspect == null) return false;
        String tag = aspect.getTag();
        return tag != null && !tag.isBlank() && Aspect.getAspect(tag) == aspect;
    }
    static boolean isCurrent(TileVisNode node) {
        if (node == null || node.isRemoved()) return false;
        Set<TileVisNode> checking = CHECKING_NODES.get();
        if (!checking.add(node)) return false;
        try {
            if (!node.hasTickIdentity()) return false;
            Level world = node.getLevel(); BlockPos pos = node.getBlockPos(); BlockState state = node.getBlockState();
            if (world == null || !world.isClientSide && !isNativeServerLevel(world)
                    || !WorldCoordinates.isCanonicalDimension(world.dimension())) return false;
            return world.isInWorldBounds(pos) && world.hasChunkAt(pos) && node.getType().isValid(state)
                    && world.getBlockEntity(pos) == node && world.getBlockState(pos) == state
                    && !node.isRemoved() && node.getLevel() == world && node.getBlockState() == state && node.hasTickIdentity();
        } finally { checking.remove(node); if (checking.isEmpty()) CHECKING_NODES.remove(); }
    }
    private static Metadata metadata(TileVisNode node) {
        if (node == null) return null;
        Set<TileVisNode> checking = CHECKING_METADATA.get();
        if (!checking.add(node)) return null;
        try {
            if (!isCurrent(node) || !node.isNetworkEnabled()) return null;
            int range = node.getRange(); byte attunement = node.getAttunement(); boolean source = node.isSource();
            return range > 0 && attunement >= -1 && attunement <= 5 && isCurrent(node) ? new Metadata(range, attunement, source) : null;
        } finally { checking.remove(node); if (checking.isEmpty()) CHECKING_METADATA.remove(); }
    }
    public static boolean isNodeValid(WeakReference<TileVisNode> ref) {
        TileVisNode node = ref == null ? null : ref.get();
        return node != null && metadata(node) != null;
    }
    public static boolean isChunkLoaded(Level level, int x, int z) {
        return level != null && level.hasChunkAt(new BlockPos(x, level.getMinBuildHeight(), z));
    }
    public static double inRange(Level level, WorldCoordinates a, WorldCoordinates b, int range) {
        if (level == null || a == null || b == null || range <= 0 || a.dimension != level.dimension() || b.dimension != level.dimension()) return -1;
        double d = a.getDistanceSquaredToWorldCoordinates(b);
        return d > 0 && d <= (double) range * range ? d : -1;
    }
    private static boolean compatible(Metadata child, Metadata parent) {
        return child.attunement == -1 || parent.attunement == -1 || child.attunement == parent.attunement;
    }
    private static boolean close(TileVisNode child, TileVisNode parent, Metadata cm, Metadata pm) {
        return child != parent && child.getLevel() == parent.getLevel() && compatible(cm, pm)
                && inRange(child.getLevel(), child.getLocation(), parent.getLocation(), cm.range) > 0;
    }

    // Use the native voxel traversal and native collision/interaction ray, not
    // opacity, donor DDA, fluids, consumer LOS, or a new per-consume ray test.
    private static boolean rayLoaded(Level world, BlockPos from, BlockPos to) {
        return BlockGetter.traverseBlocks(Vec3.atCenterOf(from), Vec3.atCenterOf(to), world,
                (level, pos) -> !level.isInWorldBounds(pos) || !level.hasChunkAt(pos) ? Boolean.FALSE : null, level -> Boolean.TRUE);
    }
    private static final class UnavailableRay extends RuntimeException {
        private static final UnavailableRay INSTANCE = new UnavailableRay();
        private UnavailableRay() { super("Unloaded VisNet ray", null, false, false); }
    }
    private record LoadedRayView(Level level) implements BlockGetter {
        private void requireLoaded(BlockPos pos) { if (!level.isInWorldBounds(pos) || !level.hasChunkAt(pos)) throw UnavailableRay.INSTANCE; }
        @Override public BlockState getBlockState(BlockPos pos) { requireLoaded(pos); return level.getBlockState(pos); }
        @Override public FluidState getFluidState(BlockPos pos) { return getBlockState(pos).getFluidState(); }
        @Override public BlockEntity getBlockEntity(BlockPos pos) { requireLoaded(pos); return level.getBlockEntity(pos); }
        @Override public int getHeight() { return level.getHeight(); }
        @Override public int getMinBuildHeight() { return level.getMinBuildHeight(); }
    }
    public static boolean canNodeBeSeen(TileVisNode source, TileVisNode target) {
        if (source == null || target == null) return false;
        BlockState sourceState = source.getBlockState(), targetState = target.getBlockState();
        if (metadata(source) == null || metadata(target) == null || source.getLevel() != target.getLevel()) return false;
        Level level = source.getLevel(); BlockPos from = source.getBlockPos(), to = target.getBlockPos();
        if (from.equals(to) || !rayLoaded(level, from, to)) return false;
        Vec3 start = Vec3.atCenterOf(from), end = Vec3.atCenterOf(to);
        ClipContext context = new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty());
        LoadedRayView view = new LoadedRayView(level);
        try {
            BlockHitResult hit = BlockGetter.traverseBlocks(start, end, context, (ray, pos) -> {
                if (pos.equals(from)) return null; // Original ApiHelper ignores its initial source cell.
                BlockState state = view.getBlockState(pos);
                return view.clipWithInteractionOverride(start, end, pos, ray.getBlockShape(state, view, pos), state);
            }, ray -> null);
            return (hit == null || hit.getType() == HitResult.Type.BLOCK && hit.getBlockPos().equals(to)) && source.getLevel() == level && target.getLevel() == level
                    && source.getBlockState() == sourceState && target.getBlockState() == targetState && isCurrent(source) && isCurrent(target);
        } catch (UnavailableRay unavailable) { return false; }
    }

    static TileVisNode parentFor(TileVisNode child) {
        if (metadata(child) == null) return null;
        TileVisNode parent;
        Level world = child.getLevel();
        if (world.isClientSide) {
            WorldCoordinates at = child.clientParent();
            if (at == null || at.dimension != world.dimension() || !world.isInWorldBounds(at.pos()) || !world.hasChunkAt(at.pos())) return null;
            parent = world.getBlockEntity(at.pos()) instanceof TileVisNode value ? value : null;
        } else parent = child.rawParent();
        Metadata cm = metadata(child), pm = metadata(parent);
        return cm != null && !cm.source && pm != null && close(child, parent, cm, pm) && isCurrent(child) ? parent : null;
    }
    static TileVisNode rootFor(TileVisNode node) {
        if (node == null || metadata(node) == null) return null;
        if (!node.getLevel().isClientSide) {
            Network net = network(node.getLevel(), false);
            Path path = net == null || net.closed ? null : path(net, node);
            return path == null ? null : path.root;
        }
        Set<TileVisNode> seen = identities();
        for (TileVisNode at = node; at != null && seen.add(at); at = parentFor(at)) {
            Metadata meta = metadata(at);
            if (meta == null) return null;
            if (meta.source) return at;
        }
        return null;
    }
    private static Path path(Network net, TileVisNode node) {
        Set<TileVisNode> seen = identities(); List<Step> steps = new ArrayList<>();
        ServerLevel world = net.world.get();
        for (TileVisNode at = node; at != null && seen.add(at);) {
            if (net.closed || at.getLevel() != world) return null;
            Metadata meta = metadata(at);
            if (meta == null) return null;
            steps.add(new Step(at, at.getAttachmentVersion(), meta));
            if (meta.source) {
                WeakReference<TileVisNode> ref = net.sources.get(at.getLocation()); TileVisNode registered = ref == null ? null : ref.get();
                return at.rawParent() == null && registered == at ? new Path(List.copyOf(steps), at) : null;
            }
            TileVisNode parent = at.rawParent(); Metadata pm = metadata(parent);
            if (pm == null || !close(at, parent, meta, pm) || !rayLoaded(world, at.getBlockPos(), parent.getBlockPos())) return null;
            int edges = 0;
            for (TileVisNode value : parent.rawChildren()) if (value == at) ++edges;
            if (edges != 1) return null;
            at = parent;
        }
        return null; // Cycle/null parent: never recursive forwarding or a second root budget.
    }
    private static boolean unchanged(Network net, Path path) {
        if (net.closed) return false;
        for (Step step : path.steps) if (step.node.getAttachmentVersion() != step.version
                || !step.metadata.equals(metadata(step.node))) return false;
        Path now = path(net, path.steps.getFirst().node);
        return now != null && now.root == path.root;
    }

    private static void remember(Network net, TileVisNode node) {
        WorldCoordinates at = node.getLocation(); WeakReference<TileVisNode> ref = net.nodes.get(at);
        TileVisNode before = ref == null ? null : ref.get();
        if (before != node) {
            if (before != null) detachRaw(net, before);
            net.nodes.put(at, new WeakReference<>(node)); net.observations.remove(at); net.changed();
        }
    }
    private static void sourceRaw(Network net, TileVisNode source) {
        remember(net, source);
        WeakReference<TileVisNode> ref = net.sources.get(source.getLocation()); TileVisNode before = ref == null ? null : ref.get();
        if (before != source) { net.sources.put(source.getLocation(), new WeakReference<>(source)); net.changed(); }
        source.registered(true);
    }
    private static void attachRaw(Network net, TileVisNode child, TileVisNode parent) {
        TileVisNode old = child.rawParent();
        if (old == parent) return;
        if (old != null) old.rawRemoveChild(child);
        child.rawParent(parent);
        if (parent != null) parent.rawAddChild(child);
        net.changed();
    }
    /** Finite identity-visited iterative walks: no512-node global power cap. */
    private static List<TileVisNode> descendants(TileVisNode root) {
        List<TileVisNode> result = new ArrayList<>(); Set<TileVisNode> seen = identities(); seen.add(root);
        ArrayDeque<TileVisNode> work = new ArrayDeque<>(root.rawChildren());
        while (!work.isEmpty()) {
            TileVisNode node = work.removeLast();
            if (!seen.add(node)) continue;
            result.add(node); work.addAll(node.rawChildren());
        }
        return result;
    }
    private static List<TileVisNode> detachRaw(Network net, TileVisNode root) {
        List<TileVisNode> removed = descendants(root); removed.add(root);
        for (TileVisNode node : removed) {
            TileVisNode old = node.rawParent();
            if (old != null) { old.rawRemoveChild(node); old.nodeRefresh = true; }
            node.rawParent(null); node.rawClearChildren(); node.nodeRefresh = true; node.registered(false);
            WorldCoordinates at = node.getLocation();
            if (at != null) {
                WeakReference<TileVisNode> ref = net.sources.get(at); TileVisNode source = ref == null ? null : ref.get();
                if (source == node) net.sources.remove(at); // NEVER unregister a new owner at the same location.
                net.observations.remove(at);
            }
        }
        net.changed();
        return removed;
    }
    private static void notifyNode(TileVisNode node) {
        try { node.notifyLinkChange(); }
        catch (RuntimeException failure) { LOG.warn("VisNet link notification failed at {}", node.getBlockPos(), failure); }
    }
    private static void sweep(Network net) {
        ServerLevel world = net.world.get();
        for (var entry : new ArrayList<>(net.nodes.entrySet())) {
            TileVisNode node = entry.getValue().get(); Metadata meta = node == null ? null : metadata(node);
            if (meta == null || node.getLevel() != world) {
                if (node != null) detachRaw(net, node);
                net.nodes.remove(entry.getKey()); net.observations.remove(entry.getKey());
                WeakReference<TileVisNode> ref = net.sources.get(entry.getKey()); TileVisNode source = ref == null ? null : ref.get();
                if (source == null || source == node) net.sources.remove(entry.getKey());
                net.changed(); continue;
            }
            TileVisNode parent = node.rawParent();
            boolean ready = parent == null || isCurrent(parent) && rayLoaded(world, node.getBlockPos(), parent.getBlockPos());
            Observation old = net.observations.get(entry.getKey()); TileVisNode previous = old == null ? null : old.node.get();
            if (old == null || previous != node || !meta.equals(old.metadata) || node.getAttachmentVersion() != old.version || ready != old.edgeLoaded) {
                net.observations.put(entry.getKey(), new Observation(new WeakReference<>(node), meta, node.getAttachmentVersion(), ready)); net.changed();
            }
        }
        for (var entry : new ArrayList<>(net.sources.entrySet())) {
            TileVisNode node = entry.getValue().get();
            if (node == null || node.getLevel() != world || metadata(node) == null || !node.isSource()) {
                if (node != null) node.registered(false);
                net.sources.remove(entry.getKey()); net.changed();
            }
        }
    }

    static boolean canTick(TileVisNode node) {
        Level level = node.getLevel();
        if (!isNativeServerLevel(level)) return false;
        Network net = network(level, true);
        if (net.closed || net.consuming || net.changing) return false;
        net.changing = true;
        try { if (metadata(node) == null) return false; remember(net, node); return isCurrent(node); }
        finally { net.changing = false; }
    }
    static void refresh(TileVisNode node) {
        if (!isNativeServerLevel(node.getLevel())) return;
        Network net = network(node.getLevel(), true);
        if (net.closed || net.consuming || net.changing) return;
        net.changing = true;
        try {
            Metadata meta = metadata(node);
            if (meta == null) return;
            remember(net, node); sweep(net);
            if (!node.nodeRefresh) for (TileVisNode child : node.rawChildren()) {
                if (child.rawParent() != node || !canNodeBeSeen(node, child)) { node.nodeRefresh = true; break; }
            }
            boolean changed = node.nodeRefresh;
            if (node.nodeRefresh) {
                for (TileVisNode child : node.rawChildren()) { child.nodeRefresh = true; child.rawParent(null); }
                node.rawClearChildren(); attachRaw(net, node, null); net.changed();
            }
            if (!isCurrent(node)) return;
            if (meta.source) { if (!node.registered()) sourceRaw(net, node); }
            else if (path(net, node) == null) {
                attachRaw(net, node, null);
                TileVisNode parent = chooseParent(net, node);
                if (parent != null && isCurrent(node)) attachRaw(net, node, parent);
                changed = true;
            }
            node.nodeRefresh = false;
            if (changed) notifyNode(node);
        } finally { net.changing = false; }
    }
    private static TileVisNode chooseParent(Network net, TileVisNode child) {
        Metadata cm = metadata(child); if (cm == null || cm.source) return null;
        double best = Double.POSITIVE_INFINITY; TileVisNode closest = null;
        Set<TileVisNode> forbidden = identities(); forbidden.add(child); forbidden.addAll(descendants(child));
        for (WeakReference<TileVisNode> ref : new ArrayList<>(net.sources.values())) {
            TileVisNode root = ref.get(); if (root == null || path(net, root) == null) continue;
            List<TileVisNode> candidates = new ArrayList<>(); candidates.add(root); candidates.addAll(descendants(root));
            for (TileVisNode candidate : candidates) {
                if (forbidden.contains(candidate) || path(net, candidate) == null) continue;
                Metadata pm = metadata(candidate);
                if (pm == null || !close(child, candidate, cm, pm)) continue;
                double d = child.getLocation().getDistanceSquaredToWorldCoordinates(candidate.getLocation());
                double score = candidate == root ? d - (double) cm.range * 2 : d;
                if (score < best && canNodeBeSeen(child, candidate) && isCurrent(child)) { best = score; closest = candidate; }
            }
        }
        return closest;
    }
    public static void addSource(Level level, TileVisNode source) {
        if (!isNativeServerLevel(level) || source == null || source.getLevel() != level) return;
        BlockState state = source.getBlockState();
        Network net = network(level, true); if (net.closed || net.changing || net.consuming) return;
        net.changing = true;
        try {
            Metadata meta = metadata(source);
            if (meta != null && meta.source && source.rawParent() == null && source.getLevel() == level && source.getBlockState() == state && isCurrent(source)) sourceRaw(net, source);
        }
        finally { net.changing = false; }
    }
    public static WeakReference<TileVisNode> addNode(Level level, TileVisNode child) {
        if (!isNativeServerLevel(level) || child == null || child.getLevel() != level) return null;
        BlockState state = child.getBlockState();
        Network net = network(level, true); if (net.closed || net.changing || net.consuming) return null;
        net.changing = true;
        try {
            Metadata meta = metadata(child); if (meta == null || meta.source || child.getLevel() != level || child.getBlockState() != state) return null;
            remember(net, child); sweep(net);
            TileVisNode parent = chooseParent(net, child);
            if (parent != null && child.getLevel() == level && child.getBlockState() == state && isCurrent(child)) { attachRaw(net, child, parent); notifyNode(child); return new WeakReference<>(parent); }
            return null;
        } finally { net.changing = false; }
    }
    static void setParent(TileVisNode child, TileVisNode parent) {
        if (child.getLevel() == null || child.getLevel().isClientSide || !isNativeServerLevel(child.getLevel())) return;
        Network net = network(child.getLevel(), true); if (net.closed || net.changing || net.consuming) return;
        net.changing = true;
        try {
            if (metadata(child) == null) return;
            if (parent == null) { attachRaw(net, child, null); child.nodeRefresh = true; notifyNode(child); return; }
            Metadata cm = metadata(child), pm = metadata(parent);
            Path route = path(net, parent);
            if (cm == null || cm.source || pm == null || route == null || !close(child, parent, cm, pm)) return;
            for (Step step : route.steps) if (step.node == child) return;
            if (child.rawParent() == parent) return;
            if (canNodeBeSeen(child, parent) && isCurrent(child)) { remember(net, child); attachRaw(net, child, parent); notifyNode(child); }
        } finally { net.changing = false; }
    }
    static void removeNode(TileVisNode node, boolean notify) {
        Level level = node.getLevel(); requireThread(level);
        Network net = network(level, false);
        if (net == null) {
            TileVisNode parent = node.rawParent(); if (parent != null) parent.rawRemoveChild(node);
            for (TileVisNode child : descendants(node)) { child.rawParent(null); child.rawClearChildren(); child.nodeRefresh = true; child.registered(false); }
            node.rawParent(null); node.rawClearChildren(); node.registered(false); node.nodeRefresh = true;
            return;
        }
        // Retirement is permitted inside a consume callback, but raw detach and
        // cache invalidation commit before notifications. No refill is unlocked.
        boolean outer = !net.changing; net.changing = true;
        try { for (TileVisNode value : detachRaw(net, node)) if (notify) notifyNode(value); }
        finally { if (outer) net.changing = false; }
    }

    private static List<Choice> choices(Network net, BlockPos pos) {
        CachedRoutes cached = net.nearby.get(pos);
        if (cached != null && cached.version == net.version) return cached.choices;
        Level level = net.world.get(); List<Choice> found = new ArrayList<>();
        WorldCoordinates consumer = new WorldCoordinates(level.dimension(), pos);
        Set<TileVisNode> roots = identities();
        for (WeakReference<TileVisNode> ref : new ArrayList<>(net.sources.values())) {
            TileVisNode root = ref.get(); if (root == null || !roots.add(root) || path(net, root) == null) continue;
            TileVisNode closest = null; double best = Double.POSITIVE_INFINITY;
            List<TileVisNode> candidates = new ArrayList<>(); candidates.add(root); candidates.addAll(descendants(root));
            for (TileVisNode candidate : candidates) {
                Path route = path(net, candidate); Metadata meta = metadata(candidate);
                if (route == null || route.root != root || meta == null) continue;
                double d = inRange(level, consumer, candidate.getLocation(), meta.range);
                if (d > 0 && d < best) { best = d; closest = candidate; }
            }
            if (closest != null) found.add(new Choice(new WeakReference<>(root), new WeakReference<>(closest)));
        }
        List<Choice> result = List.copyOf(found);
        net.nearby.put(pos.immutable(), new CachedRoutes(net.version, result)); // Empty/negative entries share topology/loaded-state invalidation.
        return result;
    }
    public static int drainVis(Level level, int x, int y, int z, Aspect aspect, int amount) { return drainVis(level, new BlockPos(x, y, z), aspect, amount); }
    public static int drainVis(Level level, BlockPos pos, Aspect aspect, int amount) {
        if (amount <= 0 || pos == null || !isNativeServerLevel(level)) return 0;
        ServerLevel server = (ServerLevel) level; Network net = network(level, true);
        if (net.closed || net.consuming || net.changing) return 0;
        net.consuming = true;
        try {
            BlockPos at = pos.immutable();
            if (!isValidAspect(aspect) || !server.isInWorldBounds(at) || !server.hasChunkAt(at)) return 0;
            Consumer consumer = new Consumer(at, server.getBlockState(at), server.getBlockEntity(at));
            if (!consumer.current(server)) return 0;
            sweep(net);
            return drain(net, choices(net, at), consumer, aspect, amount);
        } finally { net.consuming = false; if (net.closed) forget(net); }
    }
    static int consume(TileVisNode node, Aspect aspect, int amount) {
        if (node == null || amount <= 0 || !isNativeServerLevel(node.getLevel())) return 0;
        Network net = network(node.getLevel(), true); if (net.closed || net.consuming || net.changing) return 0;
        net.consuming = true;
        try {
            if (!isValidAspect(aspect) || metadata(node) == null) return 0;
            sweep(net); Path path = path(net, node); if (path == null) return 0;
            return drain(net, List.of(new Choice(new WeakReference<>(path.root), new WeakReference<>(node))), null, aspect, amount);
        } finally { net.consuming = false; if (net.closed) forget(net); }
    }
    private static int drain(Network net, List<Choice> choices, Consumer consumer, Aspect aspect, int amount) {
        ServerLevel level = net.world.get(); long actual = 0; Set<TileVisNode> spentRoots = identities();
        try {
            for (Choice choice : choices) {
                if (actual >= amount || net.closed || consumer != null && !consumer.current(level)) break;
                TileVisNode root = choice.root.get(), node = choice.node.get();
                if (root == null || node == null || !spentRoots.add(root)) continue;
                Path route = path(net, node); if (route == null || route.root != root || !unchanged(net, route)) continue;
                if (consumer != null && inRange(level, new WorldCoordinates(level.dimension(), consumer.pos), node.getLocation(), route.steps.getFirst().metadata.range) <= 0) continue;
                int remaining = (int) ((long) amount - actual);
                // A charger is also an ordinary graph relay. Positional consumers and explicit API drains outside its
                // prepared native charge phase retain their original semantics; only the wand transfer owns this receipt.
                thaumcraft.common.tiles.TileMagicWorkbenchCharger workbench = consumer == null
                        && node instanceof thaumcraft.common.tiles.TileMagicWorkbenchCharger charger && charger.hasPreparedVisCharge() ? charger : null;
                if (workbench != null && !workbench.readyForVisGrant(aspect, remaining)) break;
                thaumcraft.common.tiles.TileVisRelay amulet=consumer==null && node instanceof thaumcraft.common.tiles.TileVisRelay relay
                        && thaumcraft.common.lib.baubles.PlayerAccessories.hasPreparedAmuletCharge(relay) ? relay : null;
                if(amulet!=null && !thaumcraft.common.lib.baubles.PlayerAccessories.readyForAmuletGrant(amulet,aspect,remaining))break;
                thaumcraft.common.tiles.TileAlchemyFurnaceAdvanced furnace=consumer!=null
                        && consumer.owner instanceof thaumcraft.common.tiles.TileAlchemyFurnaceAdvanced candidate && candidate.hasPreparedPowerGrant(aspect)?candidate:null;
                if(furnace!=null && !furnace.readyForPowerGrant(aspect,remaining)) break;
                AdvancedFurnaceGrant furnaceReceipt=furnace==null?null:new AdvancedFurnaceGrant(furnace,aspect);
                int grant;
                try { grant = root.debit(aspect, remaining); }
                catch (RuntimeException failure) {
                    LOG.warn("VisNet source debit violated its callback-free contract at {}", root.getBlockPos(), failure);
                    break; // Cannot infer/rollback arbitrary addon stock from a thrown callback.
                }
                if (grant < 0 || grant > remaining) {
                    LOG.warn("Refused malformed VisNet provider grant {} for {} at {}", grant, remaining, root.getBlockPos());
                    break; // Never clamp an invalid grant into a usable subset.
                }
                actual += grant; // COMMIT readback before ANY pulse/network callbacks.
                if (grant == 0) continue;
                if (workbench != null) {
                    WORKBENCH_GRANT.set(new WorkbenchGrant(workbench, aspect, grant));
                    try { workbench.acceptPreparedVisGrant(aspect, grant); }
                    finally { WORKBENCH_GRANT.remove(); }
                }
                if(amulet!=null) {
                    AMULET_GRANT.set(new AmuletGrant(amulet,aspect,grant));
                    try { thaumcraft.common.lib.baubles.PlayerAccessories.acceptPreparedAmuletGrant(amulet,aspect,grant); }
                    finally { AMULET_GRANT.remove(); }
                }
                if(furnaceReceipt!=null) {
                    furnaceReceipt.units=grant;
                    ADVANCED_FURNACE_GRANT.set(furnaceReceipt);
                    try { furnace.acceptPreparedPowerGrant(aspect,grant); }
                    finally { ADVANCED_FURNACE_GRANT.remove(); }
                }
                for (int i = route.steps.size() - 1; i >= 0; --i) {
                    Step step = route.steps.get(i); // Original recursive forwarding unwinds parent -> child.
                    try { if (isCurrent(step.node)) step.node.triggerConsumeEffect(aspect); }
                    catch (RuntimeException failure) { LOG.warn("VisNet post-debit pulse failed at {}", step.node.getBlockPos(), failure); }
                }
                if (consumer != null) {
                    try { generateVisEffect(level, consumer.pos, node.getBlockPos(), aspect); }
                    catch (RuntimeException failure) { LOG.warn("VisNet post-debit FX failed at {}", consumer.pos, failure); }
                }
            }
        } catch (RuntimeException failure) {
            // Later identity/provider observations are also post-commit call-outs.
            // Previously reported actual debits remain actual, never a refund0.
            LOG.warn("VisNet frame stopped after {} committed raw vis", actual, failure);
        }
        return (int) actual; // Bounded by the original positive int request using wide arithmetic.
    }
    public static void generateVisEffect(ServerLevel level, BlockPos consumer, BlockPos selectedNode, Aspect aspect) {
        // Root-owned actual packet/local-Random throttle; no world RNG here.
        effectSink.send(level, consumer, selectedNode, aspect);
    }

    public static void clearLevel(ServerLevel level) {
        requireThread(level); Network net = network(level, false); if (net == null) return;
        net.closed = true;
        for (WeakReference<TileVisNode> ref : new ArrayList<>(net.nodes.values())) {
            TileVisNode node = ref.get(); if (node == null) continue;
            node.rawParent(null); node.rawClearChildren(); node.registered(false); node.nodeRefresh = true;
        }
        net.sources.clear(); net.nodes.clear(); net.observations.clear(); net.changed();
        if (!net.consuming) forget(net); // An in-flight FX callback cannot reopen/refill this frame by clearing it.
    }
    public static void clearServer(MinecraftServer server) {
        if (server == null || !server.isSameThread()) throw new IllegalStateException("VisNet lifecycle requires the server thread");
        List<Network> copy;
        synchronized (VisNetHandler.class) { copy = List.copyOf(NETWORKS); }
        for (Network net : copy) { ServerLevel level = net.world.get(); if (level == null) forget(net); else if (level.getServer() == server) clearLevel(level); }
    }
}
