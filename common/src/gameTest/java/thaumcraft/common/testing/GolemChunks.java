package thaumcraft.common.testing;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

/**
 * A golem plans a route or accepts a Guard target only when every chunk within 48 blocks of it and of the place it is going to is
 * loaded ({@code EntityGolemBase.loadedNavigationArea}). A GameTest is built at a random place on every run, so a small forced square
 * around a fixture that happens to straddle a chunk border leaves part of that ring unloaded and the golems stand idle for the whole
 * test (seen as "area=false" on every lane). Holding the ring itself, loaded and forced, removes the dependence on where the test landed.
 *
 * <p><b>The ring is shared.</b> The vanilla GameTest grid packs the scenarios of a batch thirteen blocks apart, so the ring of one
 * scenario covers the ring of its neighbour. The forced chunks of the level are a set, not a counter: the scenario that forces a chunk
 * first owns it, and the scenarios that find it already forced do not take it over. When the owner releases its chunks at the end of its
 * test, the neighbour loses a chunk of its own ring - the chunk unloads and the golem of the neighbour cannot plan a route any more.
 * {@link #refresh} takes the ring back, so that a scenario never depends on the chunk tickets of another one.</p>
 */
final class GolemChunks {
    private static final int REACH = 48;

    private GolemChunks() {}

    /** Loads and forces the ring around {@code fixture}; returns only the chunks forced here, for the caller to release. */
    static List<ChunkPos> hold(ServerLevel level, Iterable<BlockPos> fixture) {
        List<ChunkPos> forced = new ArrayList<>();
        boolean acquired = false;
        try {
            for (ChunkPos chunk : ring(fixture)) {
                if (!level.getForcedChunks().contains(chunk.toLong())) { level.setChunkForced(chunk.x, chunk.z, true); forced.add(chunk); }
                level.getChunk(chunk.x, chunk.z);
            }
            acquired = true;
            return forced;
        } finally {
            // A caller cannot release a partially acquired list it never received.
            if (!acquired) for (ChunkPos chunk : forced) level.setChunkForced(chunk.x, chunk.z, false);
        }
    }

    /**
     * Forces the ring around {@code fixture} again and records the chunks that were taken away.
     *
     * <p>Another scenario of the same batch may release a chunk of the ring - it forced the chunk first and releases what it owns when
     * it ends. The chunk then unloads and the golem of this scenario cannot plan a route. The ring is taken back here, so the scenario
     * keeps its own area whatever the other scenarios do with their chunks.</p>
     */
    static void refresh(ServerLevel level, Iterable<BlockPos> fixture, List<ChunkPos> forced) {
        for (ChunkPos chunk : ring(fixture)) {
            if (level.getForcedChunks().contains(chunk.toLong())) continue;
            level.setChunkForced(chunk.x, chunk.z, true);
            forced.add(chunk);
        }
    }

    /** The chunks of the ring around the fixture. */
    private static List<ChunkPos> ring(Iterable<BlockPos> fixture) {
        int minX = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (BlockPos pos : fixture) {
            minX = Math.min(minX, pos.getX()); maxX = Math.max(maxX, pos.getX());
            minZ = Math.min(minZ, pos.getZ()); maxZ = Math.max(maxZ, pos.getZ());
        }
        if (minX > maxX) throw new IllegalArgumentException("an empty fixture holds no chunks");
        List<ChunkPos> ring = new ArrayList<>();
        for (int x = Math.floorDiv(minX - REACH, 16); x <= Math.floorDiv(maxX + REACH, 16); x++) {
            for (int z = Math.floorDiv(minZ - REACH, 16); z <= Math.floorDiv(maxZ + REACH, 16); z++) {
                ring.add(new ChunkPos(x, z));
            }
        }
        return ring;
    }
}
