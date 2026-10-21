package thaumcraft.common.lib.events;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.IAspectSource;
import thaumcraft.common.lib.essentia.EssentiaSourceSearch;
import thaumcraft.common.lib.essentia.EssentiaTransfers;
import thaumcraft.common.lib.world.EldritchBlockRules;
import thaumcraft.common.tiles.TileInfusionMatrix;
import thaumcraft.common.tiles.TileMirrorEssentia;

/** TC4's unsided range-12 infusion source search, with level-local coordinate caches. */
public final class EssentiaHandler {
    private static final int RANGE = 12;
    private static final long DELAY_MILLIS = 5000L;
    // Values contain positions/timestamps only, never tiles or their level.
    private static final Map<ServerLevel, LevelSources> LEVELS = new WeakHashMap<>();

    private EssentiaHandler() {}

    public static boolean drainEssentia(TileInfusionMatrix matrix, Aspect aspect) {
        if (aspect == null || !(matrix.getLevel() instanceof ServerLevel level) || !current(matrix, level)
                || EssentiaTransfers.isLocked(matrix) || matrix.getPendingEssentia(aspect) <= 0) return false;
        requireThread(level);
        LevelSources cache = LEVELS.computeIfAbsent(level, ignored -> new LevelSources());
        BlockPos origin = matrix.getBlockPos();
        if (!cache.sources.containsKey(origin)) findSources(level, origin, cache);
        List<BlockPos> sources = cache.sources.get(origin);
        if (sources == null) return false;
        for (BlockPos sourcePos : sources) {
            // Original H49-53: a missing/non-source entry BREAKS the scan. A
            // different live source at the same coordinates is intentionally OK.
            if (!level.hasChunkAt(sourcePos)) break;
            BlockEntity source = level.getBlockEntity(sourcePos);
            if (!(source instanceof IAspectSource) || !current(source, level)) break;
            // No sided transport/minimum suction/filter calls, nor take/refund.
            // Unknown addon endpoints are refused by the atomic native boundary.
            if (EssentiaTransfers.consumeForInfusion(matrix, source, aspect) != 1) continue;
            matrix.sendEffect(1, sourcePos, aspect.getColor());
            return true;
        }
        cache.sources.remove(origin);
        cache.retryAfter.put(origin.immutable(), System.currentTimeMillis() + DELAY_MILLIS);
        return false;
    }

    private static void findSources(ServerLevel level, BlockPos origin, LevelSources cache) {
        Long retry = cache.retryAfter.get(origin);
        if (retry != null) {
            if (retry > System.currentTimeMillis()) return;
            cache.retryAfter.remove(origin);
        }
        // H96-125 UNKNOWN -> UP with start=-12: x, then z, then y[-12,11].
        // This is a rectangular search, not distance/LOS/nearest-source sorting.
        // The shape is the shared one of the essentia helper, so the matrix and the
        // public helper walk the very same cells in the very same order.
        List<BlockPos> found = EssentiaSourceSearch.sources(level, origin, null, RANGE, source -> current(source, level));
        if (found.isEmpty()) cache.retryAfter.put(origin.immutable(), System.currentTimeMillis() + DELAY_MILLIS);
        else cache.sources.put(origin.immutable(), List.copyOf(found));
    }

    /**
     * Oracle EssentiaHandler:39-60 with the slab of the essentia mirror: the
     * sources in front of the twin pay for the unit the mirror hands out. The
     * scan is the original one: a rectangle that is eight blocks deep in the
     * direction of the twin and seventeen blocks wide in the other two axes.
     */
    public static boolean drainEssentiaForMirror(BlockEntity source, Aspect aspect, Direction direction, int range, boolean ignoreMirror) {
        if (source == null || aspect == null || direction == null || range <= 0) return false;
        if (!(source.getLevel() instanceof ServerLevel level)) return false;
        if (!level.getServer().isSameThread()) throw new IllegalStateException("Mirror essentia drains require the server thread");
        BlockPos origin = source.getBlockPos();
        for (BlockPos sourcePos : mirrorSources(level, origin, direction, range)) {
            // Original H49-53: a missing/non-source entry BREAKS the scan.
            if (!level.hasChunkAt(sourcePos)) break;
            BlockEntity candidate = level.getBlockEntity(sourcePos);
            if (!(candidate instanceof IAspectSource aspectSource) || !current(candidate, level)) break;
            // The original skips every mirror when the mirror asks, so a pair of
            // mirrors never pays for itself.
            if (ignoreMirror && candidate instanceof TileMirrorEssentia) continue;
            if (!aspectSource.takeFromContainer(aspect, 1)) continue;
            return true;
        }
        return false;
    }

    /** Oracle EssentiaHandler:96-125 - the slab of the direction the twin looks at. */
    private static List<BlockPos> mirrorSources(ServerLevel level, BlockPos origin, Direction direction, int range) {
        LevelSources cache = LEVELS.computeIfAbsent(level, ignored -> new LevelSources());
        List<BlockPos> cached = cache.mirrorSources.get(origin);
        if (cached != null) return cached;
        Long retry = cache.mirrorRetryAfter.get(origin);
        if (retry != null) {
            if (retry > System.currentTimeMillis()) return List.of();
            cache.mirrorRetryAfter.remove(origin);
        }
        List<BlockPos> found = new ArrayList<>();
        for (int first = -range; first <= range; first++) {
            for (int second = -range; second <= range; second++) {
                for (int depth = 0; depth < range; depth++) {
                    if (first == 0 && second == 0 && depth == 0) continue;
                    BlockPos pos = slabCell(origin, direction, first, second, depth);
                    if (!level.isInWorldBounds(pos) || !level.hasChunkAt(pos)) continue;
                    BlockEntity source = level.getBlockEntity(pos);
                    if (source instanceof IAspectSource && current(source, level)) found.add(pos);
                }
            }
        }
        if (found.isEmpty()) cache.mirrorRetryAfter.put(origin.immutable(), System.currentTimeMillis() + DELAY_MILLIS);
        else cache.mirrorSources.put(origin.immutable(), List.copyOf(found));
        return found;
    }

    /** The original places the slab along the direction the twin looks at. */
    private static BlockPos slabCell(BlockPos origin, Direction direction, int first, int second, int depth) {
        int[] offset = EldritchBlockRules.mirrorEssentiaSlabOffset(direction, first, second, depth);
        return origin.offset(offset[0], offset[1], offset[2]);
    }

    /** Original H136-137 clears coordinates only, NEVER the wall-clock delay. */
    public static void refreshSources(TileInfusionMatrix matrix) {
        if (!(matrix.getLevel() instanceof ServerLevel level)) return;
        requireThread(level);
        LevelSources cache = LEVELS.get(level);
        if (cache != null) cache.sources.remove(matrix.getBlockPos());
    }

    /** Root lifecycle hook: forget a closed world, including its backoff timestamps. */
    public static void clear(ServerLevel level) { LEVELS.remove(level); }
    public static void clear() { LEVELS.clear(); }

    private static boolean current(BlockEntity tile, ServerLevel level) {
        return !tile.isRemoved() && tile.getLevel() == level && level.hasChunkAt(tile.getBlockPos())
                && level.getBlockEntity(tile.getBlockPos()) == tile;
    }

    private static void requireThread(ServerLevel level) {
        if (!level.getServer().isSameThread()) throw new IllegalStateException("Infusion sources require the server thread");
    }

    private static final class LevelSources {
        final Map<BlockPos, List<BlockPos>> sources = new HashMap<>();
        final Map<BlockPos, Long> retryAfter = new HashMap<>();
        final Map<BlockPos, List<BlockPos>> mirrorSources = new HashMap<>();
        final Map<BlockPos, Long> mirrorRetryAfter = new HashMap<>();
    }
}
