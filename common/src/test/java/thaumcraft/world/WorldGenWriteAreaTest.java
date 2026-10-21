package thaumcraft.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import org.junit.jupiter.api.Test;
import thaumcraft.common.lib.world.WorldGenWriteArea;

/**
 * The write area of a features step and the reason a Greatwood has to start inside its chunk.
 *
 * <p>{@code WorldGenRegion#ensureCanWrite} accepts the chunks within {@code blockStateWriteRadius} (one, for
 * the features step) of the chunk being decorated: the centre chunk and one on every side, from {@code -16} to
 * {@code +31} blocks around the centre chunk's corner.
 */
class WorldGenWriteAreaTest {
    private static final ChunkPos CENTER = new ChunkPos(0, 0);
    /**
     * The widest magical tree: a Greatwood leaf node lies at most 12 blocks from its base
     * (1.66 * 5.25 * 1.328 rounded up by the half block of the floor), the leaves reach 3 more and the lower tier
     * may be shifted one block by the 2x2 trunk search.
     */
    private static final int GREATWOOD_REACH = 12 + 3 + 1;

    private static boolean inside(int x, int z) { return WorldGenWriteArea.inside(CENTER, 1, new BlockPos(x, 64, z)); }

    @Test void theWriteAreaIsTheCentreChunkAndOneChunkAround() {
        assertTrue(inside(-16, -16));
        assertTrue(inside(31, 31));
        assertFalse(inside(-17, 0));
        assertFalse(inside(32, 0));
        assertFalse(inside(0, -17));
        assertFalse(inside(0, 32));
    }

    @Test void theAreaMovesWithTheCentreChunk() {
        ChunkPos center = new ChunkPos(-24, 13);
        assertTrue(WorldGenWriteArea.inside(center, 1, new BlockPos(-384, 86, 220)), "x -384 is the first block of chunk -24, the centre's neighbour");
        assertFalse(WorldGenWriteArea.inside(new ChunkPos(-26, 13), 1, new BlockPos(-384, 86, 220)), "two chunks away from the centre -26");
        assertTrue(WorldGenWriteArea.inside(new ChunkPos(-26, 13), 2, new BlockPos(-384, 86, 220)), "a wider radius reaches it");
    }

    @Test void aTreeDrawnInsideItsChunkAlwaysFitsOnEverySide() {
        for (int origin = 0; origin < 16; origin++) {
            assertTrue(inside(origin + GREATWOOD_REACH, origin + GREATWOOD_REACH), "east/south reach of origin " + origin);
            assertTrue(inside(origin - GREATWOOD_REACH, origin - GREATWOOD_REACH), "west/north reach of origin " + origin);
        }
    }

    @Test void theOriginalPlusEightDrawLeavesNoRoomToTheEastAndSouth() {
        int refused = 0;
        for (int origin = 8; origin < 24; origin++) if (!inside(origin + GREATWOOD_REACH, 0)) refused++;
        assertEquals(8, refused, "origins 16..23 plus a canopy of " + GREATWOOD_REACH + " end past block 31");
    }
}
