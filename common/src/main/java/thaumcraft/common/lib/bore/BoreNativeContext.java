package thaumcraft.common.lib.bore;

import java.lang.StackWalker.StackFrame;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.function.BooleanSupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.TickingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import thaumcraft.common.tiles.TileArcaneBore;

/** Native invocation provenance and exact-cell observation ONLY. No stock,
 * harvesting, output, fake player, permission bypass or mutation capability.
 * Native bridges require the actual transformed declaring class/call chain;
 * private frames never escape. Root must install REQUIRED mixins/lifecycle hooks. */
public final class BoreNativeContext {
    private static final StackWalker WALKER = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
    private static final Map<ServerLevel, Boolean> REVOKED_LEVELS = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<MinecraftServer, Boolean> REVOKED_SERVERS = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<BlockEntity, Boolean> REVOKED_OWNERS = Collections.synchronizedMap(new WeakHashMap<>());
    private static final ThreadLocal<Dispatch> DISPATCH = new ThreadLocal<>();
    private static final ThreadLocal<Operation> OPERATION = new ThreadLocal<>();
    private static final ThreadLocal<InventoryObservation> OBSERVATION = new ThreadLocal<>();
    // Interface has exactly ONE ()void method. Resolve its actual runtime name,
    // so production intermediary names do not depend on a literal Mojmap "tick".
    private static final String NATIVE_TICK = tickingMethodName();

    private BoreNativeContext() {}

    /** Injected native raw READ adapters. No mutable map/list is exposed. */
    public interface Owners {
        BlockEntity thaumcraft$boreExistingOwner(BlockPos pos);
        boolean thaumcraft$boreHasPendingOwner(BlockPos pos);
    }
    public interface ChunkAccess extends Owners {
        boolean thaumcraft$boreRegisteredTicker(BlockPos pos, Object wrapper);
    }
    public interface LoadedChunks {
        LevelChunk thaumcraft$boreLoadedChunk(int x, int z);
    }
    public interface BoundAccess {
        LevelChunk thaumcraft$boreBoundChunk();
        BlockEntity thaumcraft$boreBoundOwner();
        BlockEntityTicker<BlockEntity> thaumcraft$boreBoundTicker();
    }
    public interface WrapperAccess {
        TickingBlockEntity thaumcraft$boreWrappedTicker();
    }
    public interface InventoryAccess {
        void thaumcraft$boreObserveInventory(InventoryObservation observation);
    }

    /** Exact native canonical world/thread/lifetime, NOT a VisNet epoch. */
    private static boolean live(ServerLevel level) {
        if (level == null || level.getClass() != ServerLevel.class) return false;
        MinecraftServer server = level.getServer();
        return server != null && server.isSameThread() && !server.isStopped()
                && !REVOKED_SERVERS.containsKey(server) && !REVOKED_LEVELS.containsKey(level)
                && server.getLevel(level.dimension()) == level;
    }
    public static boolean isLiveLevel(ServerLevel level) { return live(level); }
    /** Read-only post-invocation delivery gate. Evacuation can retire the source
     * BEFORE its native onRemove/enclosing remove call returns; that is not yet
     * permission to recursively flush the new durable queue. No source BE needed. */
    public static boolean canDispatchOutputs(ServerLevel level) {
        if (!live(level) || DISPATCH.get() != null || OPERATION.get() != null) return false;
        return WALKER.walk(stream -> stream.noneMatch(BoreNativeContext::nativeMutationOnStack));
    }
    private static boolean nativeMutationOnStack(StackFrame frame) {
        Class<?> owner = frame.getDeclaringClass(); MethodType type = frame.getMethodType();
        if (owner == LevelChunk.class)
            return type.equals(MethodType.methodType(BlockState.class, BlockPos.class, BlockState.class, boolean.class));
        if (owner == Level.class)
            return type.equals(MethodType.methodType(boolean.class, BlockPos.class, BlockState.class, int.class, int.class))
                    || type.equals(MethodType.methodType(boolean.class, BlockPos.class, BlockState.class, int.class))
                    || type.equals(MethodType.methodType(boolean.class, BlockPos.class, boolean.class))
                    // Native destroyBlock emits BLOCK_DESTROY AFTER the inner
                    // setBlock/onRemove returns. Its outer callback must not
                    // recursively dispatch freshly evacuated paid outputs.
                    || type.equals(MethodType.methodType(boolean.class, BlockPos.class, boolean.class, Entity.class, int.class));
        return false;
    }
    private static LevelChunk loadedChunk(ServerLevel level, BlockPos pos) {
        if (!live(level) || !level.isInWorldBounds(pos)) return null;
        ServerChunkCache cache = level.getChunkSource();
        if (cache.getClass() != ServerChunkCache.class || !((Object) cache instanceof LoadedChunks access)) return null;
        // getChunkNow increments the virtual profiler: use the native existing
        // holder/FULL result adapter instead, with no request/generation/counter.
        LevelChunk chunk = access.thaumcraft$boreLoadedChunk(pos.getX() >> 4, pos.getZ() >> 4);
        return chunk != null && chunk.getClass() == LevelChunk.class && chunk.getLevel() == level && live(level) ? chunk : null;
    }
    public static boolean isCurrentOwner(BlockEntity owner) {
        if (owner == null || !(owner.getLevel() instanceof ServerLevel level) || !live(level)
                || owner.isRemoved() || REVOKED_OWNERS.containsKey(owner)) return false;
        BlockPos pos = owner.getBlockPos(); LevelChunk chunk = loadedChunk(level, pos);
        // Raw map read, NOT getBlockEntity's lazy creation/promotion path.
        return chunk instanceof ChunkAccess access && access.thaumcraft$boreExistingOwner(pos) == owner
                && chunk.getBlockState(pos) == owner.getBlockState() && !owner.isRemoved()
                && !REVOKED_OWNERS.containsKey(owner) && live(level);
    }
    public static boolean isNativeTick(TileArcaneBore top) {
        return OPERATION.get() instanceof Tick tick && tick.top == top && tick.open && !tick.revoked
                && DISPATCH.get() == tick.dispatch && tick.dispatch.running && isCurrentOwner(top)
                && top.getLevel() == tick.level && top.lifetimeId().equals(tick.lifetime)
                && top.getBlockState() == tick.state && registered(tick.dispatch);
    }

    /** ONLY Level's actual required redirect may call this bridge. Every native
     * ticker still executes once. An invalid origin never gains a Bore frame. */
    public static void nativeLevelTick(Level world, TickingBlockEntity wrapper) {
        if (!(world instanceof ServerLevel level) || !live(level)
                || !(wrapper instanceof WrapperAccess access)
                || !(access.thaumcraft$boreWrappedTicker() instanceof BoundAccess bound)
                || !(bound.thaumcraft$boreBoundOwner() instanceof TileArcaneBore top)) {
            wrapper.tick(); return;
        }
        List<StackFrame> callers = callers(4);
        requireHandler(callers, 0, Level.class, "thaumcraft$boreLevelTicker", void.class, TickingBlockEntity.class);
        boolean nativeLoop = frame(callers, 1, Level.class, void.class)
                && frame(callers, 2, ServerLevel.class, void.class, BooleanSupplier.class)
                && frame(callers, 3, MinecraftServer.class, void.class, BooleanSupplier.class);
        if (!nativeLoop || DISPATCH.get() != null) { wrapper.tick(); return; }
        Dispatch dispatch = new Dispatch(level, wrapper, bound, top);
        if (!registered(dispatch) || !isCurrentOwner(top)) { wrapper.tick(); return; }
        DISPATCH.set(dispatch);
        try { wrapper.tick(); }
        finally { dispatch.open = false; DISPATCH.remove(); }
    }
    private static boolean registered(Dispatch dispatch) {
        if (!dispatch.open || !live(dispatch.level) || dispatch.top.getLevel() != dispatch.level
                || dispatch.wrapper.getClass().getEnclosingClass() != LevelChunk.class
                || !(dispatch.wrapper instanceof WrapperAccess wrapper)
                || wrapper.thaumcraft$boreWrappedTicker() != dispatch.bound) return false;
        LevelChunk chunk = loadedChunk(dispatch.level, dispatch.top.getBlockPos());
        return chunk == dispatch.bound.thaumcraft$boreBoundChunk() && chunk instanceof ChunkAccess access
                && access.thaumcraft$boreExistingOwner(dispatch.top.getBlockPos()) == dispatch.top
                && access.thaumcraft$boreRegisteredTicker(dispatch.top.getBlockPos(), dispatch.wrapper)
                && dispatch.bound.thaumcraft$boreBoundOwner() == dispatch.top;
    }
    /** ONLY the native Bound wrapper's exact ticker invocation. No supplied
     * target/boolean/tick permit; the callback is the actual final bound ticker. */
    public static void nativeBoundTick(BoundAccess bound, BlockEntityTicker<BlockEntity> ticker, Level world,
                                       BlockPos pos, BlockState state, BlockEntity owner) {
        if (!(owner instanceof TileArcaneBore top) || !(world instanceof ServerLevel level) || !live(level)) {
            ticker.tick(world, pos, state, owner); return;
        }
        List<StackFrame> callers = callers(3);
        Class<?> boundClass = bound.getClass();
        if (boundClass.getEnclosingClass() != LevelChunk.class)
            throw new IllegalStateException("Not a native bound ticker class");
        requireHandler(callers, 0, boundClass, "thaumcraft$boreBoundTicker", void.class,
                BlockEntityTicker.class, Level.class, BlockPos.class, BlockState.class, BlockEntity.class);
        Dispatch dispatch = DISPATCH.get();
        boolean nativeCall = frame(callers, 1, boundClass, void.class)
                && callers.get(1).getMethodName().equals(NATIVE_TICK) && dispatch != null
                && frame(callers, 2, dispatch.wrapper.getClass(), void.class)
                && callers.get(2).getMethodName().equals(NATIVE_TICK);
        if (!nativeCall || dispatch.bound != bound || dispatch.top != top || dispatch.level != level || dispatch.running
                || bound.thaumcraft$boreBoundOwner() != owner || bound.thaumcraft$boreBoundTicker() != ticker
                || !pos.equals(top.getBlockPos()) || state != top.getBlockState()
                || !registered(dispatch) || !isCurrentOwner(top) || OPERATION.get() != null) {
            ticker.tick(world, pos, state, owner); return; // No context -> Bore's own native-origin guard refuses.
        }
        Tick tick = new Tick(level, top, state, dispatch);
        dispatch.running = true; OPERATION.set(tick);
        try { ticker.tick(world, pos, state, owner); }
        finally { closeOperation(tick); OPERATION.remove(); dispatch.running = false; }
    }

    /** Native LevelChunk redirect captures old map owner BEFORE oldState.onRemove.
     * The section already contains replacement; only this exact frame admits it. */
    public static void nativeOnRemove(LevelChunk chunk, BlockState oldState, Level world, BlockPos pos,
                                      BlockState replacement, boolean moving) {
        if (!(world instanceof ServerLevel level) || !live(level) || chunk != loadedChunk(level, pos)
                || !(chunk instanceof ChunkAccess access)
                || !(access.thaumcraft$boreExistingOwner(pos) instanceof TileArcaneBore top)
                || top.getLevel() != world || top.isRemoved() || REVOKED_OWNERS.containsKey(top)
                || !pos.equals(top.getBlockPos()) || top.getBlockState() != oldState
                || chunk.getBlockState(pos) != replacement || oldState.getBlock() == replacement.getBlock()) {
            oldState.onRemove(world, pos, replacement, moving); return;
        }
        List<StackFrame> callers = callers(2);
        requireHandler(callers, 0, LevelChunk.class, "thaumcraft$boreOnRemove", void.class,
                BlockState.class, Level.class, BlockPos.class, BlockState.class, boolean.class);
        if (!frame(callers, 1, LevelChunk.class, BlockState.class, BlockPos.class, BlockState.class, boolean.class))
            throw new IllegalStateException("Not native chunk state replacement");
        Operation parent = OPERATION.get();
        Retirement retirement = new Retirement(level, top, oldState, replacement, chunk, parent, captureSlots(top));
        OPERATION.set(retirement);
        try { oldState.onRemove(world, pos, replacement, moving); }
        finally {
            closeOperation(retirement);
            if (parent == null) OPERATION.remove(); else OPERATION.set(parent);
        }
    }
    public static void requireRetirement(TileArcaneBore top) {
        if (!(OPERATION.get() instanceof Retirement retirement) || retirement.top != top || !retirementMatches(retirement))
            throw new IllegalStateException("No exact native bore retirement");
    }
    /** Retention ONLY, never gameplay/output admission. A codec callback may
     * already have removed/revoked the old BE. Its still-open native retirement
     * (or ancestor during reentry) remains evidence of WHICH dying slot objects
     * need opaque custody. No current-map, unretire or world-lifetime permission
     * is returned, and ordinary requireRetirement stays strict. */
    public static void requireRetirementRetention(TileArcaneBore top) {
        for (Operation operation = OPERATION.get(); operation != null; operation = operation.parent) {
            if (operation instanceof Retirement retirement && retirement.top == top && retirement.open
                    && retirement.level.getServer().isSameThread() && top.getLevel() == retirement.level
                    && retirement.pos.equals(top.getBlockPos()) && retirement.lifetime.equals(top.lifetimeId())
                    && retirement.machine.equals(top.machineIdentity()) && retirement.revision == top.revision()
                    && retirement.slots.matches(captureSlots(top))) return;
        }
        throw new IllegalStateException("No open native dying-owner retention binding");
    }
    public static boolean matchesRetirement(TileArcaneBore top, TileArcaneBore.WorkSnapshot snapshot) {
        if (!(OPERATION.get() instanceof Retirement retirement) || retirement.top != top || snapshot == null
                || snapshot.phase() != TileArcaneBore.WorkPhase.RETIRE || !retirementMatches(retirement)) return false;
        return retirement.pos.equals(snapshot.topPos()) && snapshot.topState() == retirement.state
                && retirement.lifetime.equals(snapshot.lifetimeId()) && retirement.machine.equals(snapshot.machineIdentity())
                && retirement.revision == snapshot.revision(); // NO snapshot.matchesCurrent recursion.
    }
    private static boolean retirementMatches(Retirement retirement) {
        TileArcaneBore top = retirement.top;
        return retirement.open && !retirement.revoked && live(retirement.level) && !top.isRemoved()
                && !REVOKED_OWNERS.containsKey(top) && top.getLevel() == retirement.level
                && top.getBlockPos().equals(retirement.pos) && top.getBlockState() == retirement.state
                && top.lifetimeId().equals(retirement.lifetime) && top.machineIdentity().equals(retirement.machine)
                && top.revision() == retirement.revision && loadedChunk(retirement.level, retirement.pos) == retirement.chunk
                && retirement.chunk instanceof ChunkAccess access && access.thaumcraft$boreExistingOwner(retirement.pos) == top
                && retirement.chunk.getBlockState(retirement.pos) == retirement.replacement
                && retirement.slots.matches(captureSlots(top));
    }

    /** Only exact copied cells in a synchronous real Bore invocation. No permit.
     * Own native removal invalidates a watch too: validate BEFORE delegate entry. */
    public static Watch watch(ServerLevel level, List<BlockPos> cells) {
        Operation operation = OPERATION.get();
        if (!live(level) || operation == null || !operation.open || operation.revoked || operation.level != level)
            throw new IllegalStateException("A bore watch requires its real native operation");
        if (cells == null || cells.isEmpty()) throw new IllegalArgumentException("Empty bore watch");
        Map<BlockPos, LevelChunk> copied = new HashMap<>();
        for (BlockPos cell : cells) {
            LevelChunk chunk = cell == null ? null : loadedChunk(level, cell);
            if (chunk == null) throw new IllegalArgumentException("Unavailable bore watch cell");
            copied.put(cell.immutable(), chunk);
        }
        Watch watch = new Watch(operation, Map.copyOf(copied)); operation.watches.add(watch); return watch;
    }
    public static final class Watch implements AutoCloseable {
        private Operation operation;
        private Map<BlockPos, LevelChunk> cells;
        private final Thread thread = Thread.currentThread();
        private boolean invalidated;
        private Watch(Operation operation, Map<BlockPos, LevelChunk> cells) { this.operation = operation; this.cells = cells; }
        public boolean valid() {
            Operation owner = operation;
            if (owner == null || invalidated || thread != Thread.currentThread() || !owner.open || owner.revoked || !live(owner.level)) return false;
            if (owner instanceof Tick tick && (!isCurrentOwner(tick.top) || !tick.top.lifetimeId().equals(tick.lifetime))) return false;
            if (owner instanceof Retirement retirement && REVOKED_OWNERS.containsKey(retirement.top)) return false;
            for (var entry : cells.entrySet()) if (loadedChunk(owner.level, entry.getKey()) != entry.getValue()) return false;
            for (Operation current = OPERATION.get(); current != null; current = current.parent) if (current == owner) return true;
            return false;
        }
        @Override public void close() {
            if (thread != Thread.currentThread()) throw new IllegalStateException("Off-thread bore watch close");
            Operation owner = operation;
            if (owner != null) { owner.watches.remove(this); operation = null; cells = Map.of(); }
        }
    }
    /** Invalidation only. These native hooks never cancel/change vanilla calls. */
    public static void nativeLevelRequest(Level world, BlockPos pos) {
        if (watched(world, pos)) invalidate(world, pos);
    }
    public static void nativeChunkRequest(LevelChunk chunk, BlockPos pos) {
        // Denial-only bridge: even an explicit external observation can only
        // invalidate a watch. No exception/cancellation is added to native work.
        if (watched(chunk.getLevel(), pos)) invalidate(chunk.getLevel(), pos);
    }
    public static void nativeChunkClear(LevelChunk chunk) {
        if (OPERATION.get() == null) return;
        for (Operation operation = OPERATION.get(); operation != null; operation = operation.parent)
            if (operation.level == chunk.getLevel()) for (Watch watch : operation.watches)
                for (BlockPos pos : watch.cells.keySet())
                    if ((pos.getX() >> 4) == chunk.getPos().x && (pos.getZ() >> 4) == chunk.getPos().z) watch.invalidated = true;
    }
    private static boolean watched(Level world, BlockPos pos) {
        if (!(world instanceof ServerLevel) || pos == null) return false;
        for (Operation operation = OPERATION.get(); operation != null; operation = operation.parent)
            if (operation.level == world) for (Watch watch : operation.watches) if (watch.cells.containsKey(pos)) return true;
        return false;
    }
    private static void invalidate(Level world, BlockPos pos) {
        if (!(world instanceof ServerLevel) || pos == null) return;
        for (Operation operation = OPERATION.get(); operation != null; operation = operation.parent)
            if (operation.level == world) for (Watch watch : operation.watches)
                if (watch.cells.containsKey(pos)) watch.invalidated = true;
    }
    /** Revocation is denial-only. An arbitrary call cannot authorize extraction. */
    public static void retired(BlockEntity owner) {
        if (owner == null) return;
        REVOKED_OWNERS.put(owner, Boolean.TRUE);
        Level world = owner.getLevel(); BlockPos pos = owner.getBlockPos(); invalidate(world, pos);
        for (Operation operation = OPERATION.get(); operation != null; operation = operation.parent) {
            if (operation instanceof Tick tick && tick.level == world && (tick.top == owner || tick.basePos.equals(pos))) revoke(tick);
            if (operation instanceof Retirement retirement && retirement.top == owner) revoke(retirement);
        }
    }
    public static void clearLevel(ServerLevel level) {
        if (level == null) return;
        REVOKED_LEVELS.put(level, Boolean.TRUE);
        for (Operation operation = OPERATION.get(); operation != null; operation = operation.parent)
            if (operation.level == level) revoke(operation);
    }
    public static void clearServer(MinecraftServer server) {
        if (server == null) return;
        REVOKED_SERVERS.put(server, Boolean.TRUE);
        for (Operation operation = OPERATION.get(); operation != null; operation = operation.parent)
            if (operation.level.getServer() == server) revoke(operation);
    }
    private static void revoke(Operation operation) {
        operation.revoked = true;
        for (Watch watch : operation.watches) watch.invalidated = true;
    }
    private static void closeOperation(Operation operation) {
        operation.open = false;
        while (!operation.watches.isEmpty()) operation.watches.get(operation.watches.size() - 1).close();
    }

    /** Opaque read sink manufactured only during this class's private capture.
     * The adapter can pass identities INTO it but cannot obtain installed stacks. */
    public static final class InventoryObservation {
        private final TileArcaneBore top;
        private Slots result;
        private InventoryObservation(TileArcaneBore top) { this.top = top; }
        public void accept(TileArcaneBore actual, ItemStack focus, ItemStack pick) {
            List<StackFrame> callers = callers(1);
            requireHandler(callers, 0, TileArcaneBore.class, "thaumcraft$boreObserveInventory", void.class, InventoryObservation.class);
            if (OBSERVATION.get() != this || top != actual || result != null || focus == null || pick == null)
                throw new IllegalStateException("Not a closed bore identity observation");
            result = new Slots(focus, pick);
        }
    }
    private static Slots captureSlots(TileArcaneBore top) {
        if (!((Object) top instanceof InventoryAccess access)) throw new IllegalStateException("Required bore inventory identity mixin missing");
        if (OBSERVATION.get() != null) throw new IllegalStateException("Recursive bore inventory identity observation");
        InventoryObservation observation = new InventoryObservation(top); OBSERVATION.set(observation);
        try {
            access.thaumcraft$boreObserveInventory(observation);
            if (observation.result == null) throw new IllegalStateException("Missing native bore slot identities");
            return observation.result;
        } finally { OBSERVATION.remove(); }
    }
    private record Slots(ItemStack focus, ItemStack pick) {
        private boolean matches(Slots actual) { return focus == actual.focus && pick == actual.pick; }
    }
    private static class Operation {
        final ServerLevel level;
        final Operation parent;
        final List<Watch> watches = new ArrayList<>();
        boolean open = true, revoked;
        private Operation(ServerLevel level, Operation parent) { this.level = level; this.parent = parent; }
    }
    private static final class Tick extends Operation {
        final TileArcaneBore top;
        final BlockState state;
        final UUID lifetime;
        final BlockPos basePos;
        final Dispatch dispatch;
        private Tick(ServerLevel level, TileArcaneBore top, BlockState state, Dispatch dispatch) {
            super(level, null); this.top = top; this.state = state; this.dispatch = dispatch;
            lifetime = top.lifetimeId(); basePos = ArcaneBoreMath.basePosition(top.getBlockPos(), top.attachment());
        }
    }
    private static final class Retirement extends Operation {
        final TileArcaneBore top;
        final BlockState state, replacement;
        final LevelChunk chunk;
        final BlockPos pos;
        final UUID lifetime, machine;
        final long revision;
        final Slots slots;
        private Retirement(ServerLevel level, TileArcaneBore top, BlockState state, BlockState replacement,
                           LevelChunk chunk, Operation parent, Slots slots) {
            super(level, parent); this.top = top; this.state = state; this.replacement = replacement;
            this.chunk = chunk; this.slots = slots; pos = top.getBlockPos().immutable();
            lifetime = top.lifetimeId(); machine = top.machineIdentity(); revision = top.revision();
        }
    }
    private static final class Dispatch {
        final ServerLevel level;
        final TickingBlockEntity wrapper;
        final BoundAccess bound;
        final TileArcaneBore top;
        boolean open = true, running;
        private Dispatch(ServerLevel level, TickingBlockEntity wrapper, BoundAccess bound, TileArcaneBore top) {
            this.level = level; this.wrapper = wrapper; this.bound = bound; this.top = top;
        }
    }
    private static List<StackFrame> callers(int count) {
        return WALKER.walk(stream -> stream.dropWhile(frame -> frame.getDeclaringClass().getNestHost() == BoreNativeContext.class)
                .limit(count).toList());
    }
    private static boolean frame(List<StackFrame> frames, int index, Class<?> owner, Class<?> result, Class<?>... parameters) {
        return frames.size() > index && frames.get(index).getDeclaringClass() == owner
                && frames.get(index).getMethodType().equals(MethodType.methodType(result, parameters));
    }
    private static void requireHandler(List<StackFrame> frames, int index, Class<?> owner, String suffix,
                                       Class<?> result, Class<?>... parameters) {
        if (!handler(frames, index, owner, suffix, result, parameters))
            throw new IllegalStateException("Not the required native bore handler: " + suffix);
    }
    private static boolean handler(List<StackFrame> frames, int index, Class<?> owner, String suffix,
                                   Class<?> result, Class<?>... parameters) {
        return frame(frames, index, owner, result, parameters) && frames.get(index).getMethodName().endsWith(suffix);
    }
    private static String tickingMethodName() {
        String result = null;
        for (var method : TickingBlockEntity.class.getDeclaredMethods())
            if (method.getReturnType() == void.class && method.getParameterCount() == 0) {
                if (result != null) throw new IllegalStateException("Ambiguous native ticker method");
                result = method.getName();
            }
        if (result == null) throw new IllegalStateException("Missing native ticker method");
        return result;
    }
}
