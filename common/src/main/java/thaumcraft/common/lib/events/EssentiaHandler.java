package thaumcraft.common.lib.events;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.IAspectSource;
import thaumcraft.common.lib.essentia.EssentiaTransfers;
import thaumcraft.common.tiles.TileInfusionMatrix;

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
        List<BlockPos> found = new ArrayList<>();
        // H96-125 UNKNOWN -> UP with start=-12: x, then z, then y[-12,11].
        // This is a rectangular search, not distance/LOS/nearest-source sorting.
        for (int dx = -RANGE; dx <= RANGE; dx++) {
            for (int dz = -RANGE; dz <= RANGE; dz++) {
                for (int dy = -RANGE; dy < RANGE; dy++) {
                    if (dx == 0 && dz == 0 && dy == 0) continue;
                    BlockPos pos = origin.offset(dx, dy, dz);
                    if (!level.isInWorldBounds(pos) || !level.hasChunkAt(pos)) continue;
                    BlockEntity source = level.getBlockEntity(pos);
                    if (source instanceof IAspectSource && current(source, level)) found.add(pos);
                }
            }
        }
        if (found.isEmpty()) cache.retryAfter.put(origin.immutable(), System.currentTimeMillis() + DELAY_MILLIS);
        else cache.sources.put(origin.immutable(), List.copyOf(found));
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
    }
}
