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
 */
final class GolemChunks {
    private static final int REACH = 48;

    private GolemChunks() {}

    /** Loads and forces the ring around {@code fixture}; returns only the chunks forced here, for the caller to release. */
    static List<ChunkPos> hold(ServerLevel level, Iterable<BlockPos> fixture) {
        int minX = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (BlockPos pos : fixture) {
            minX = Math.min(minX, pos.getX()); maxX = Math.max(maxX, pos.getX());
            minZ = Math.min(minZ, pos.getZ()); maxZ = Math.max(maxZ, pos.getZ());
        }
        if (minX > maxX) throw new IllegalArgumentException("an empty fixture holds no chunks");
        List<ChunkPos> forced = new ArrayList<>();
        for (int x = Math.floorDiv(minX - REACH, 16); x <= Math.floorDiv(maxX + REACH, 16); x++) {
            for (int z = Math.floorDiv(minZ - REACH, 16); z <= Math.floorDiv(maxZ + REACH, 16); z++) {
                if (!level.getForcedChunks().contains(ChunkPos.asLong(x, z))) { level.setChunkForced(x, z, true); forced.add(new ChunkPos(x, z)); }
                level.getChunk(x, z);
            }
        }
        return forced;
    }
}
