package thaumcraft.wands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;
import thaumcraft.client.ThaumcraftArchitectClient;
import thaumcraft.common.lib.world.WardingOverlay;

/**
 * The world overlay of the Architect focus.
 *
 * <p>Oracle {@code thaumcraft/client/lib/REHWandHandler.java:297-479}: the area of the Architect
 * focus is drawn in the world, every block as a translucent cube whose face carries the warded
 * glass rune of its eight neighbours ({@code :311-313, :318-368}) and the inner faces of the area
 * are not drawn ({@code :370-372}).</p>
 *
 * <p>No test constructs a {@code Block}: every rule under test is a pure static function over
 * positions.</p>
 */
class ArchitectOverlayTest {
    private static Set<BlockPos> cells(BlockPos... positions) {
        Set<BlockPos> set = new HashSet<>();
        for (BlockPos pos : positions) set.add(pos.immutable());
        return set;
    }

    @Test void anIsolatedBlockHasNoNeighbourAndTheFirstRune() {
        BlockPos pos = new BlockPos(10, 64, -3);
        assertEquals(0, ThaumcraftArchitectClient.mask(cells(pos), pos, Direction.UP),
                "an isolated block of the area has no neighbour at all");
        assertEquals(1, WardingOverlay.texture(0),
                "the empty mask of the original is the first rune texture");
    }

    @Test void theMaskOfAFaceCountsItsEightNeighbours() {
        BlockPos pos = new BlockPos(0, 0, 0);
        // The original lists the eight neighbours of a face in the order of
        // REHWandHandler:326-355; the first one of the top face is the block
        // below and to the left.
        BlockPos first = WardingOverlay.neighbour(pos, Direction.UP, 0);
        assertEquals(new BlockPos(-1, 0, -1), first, "the first neighbour of the top face of the original");
        assertEquals(1, ThaumcraftArchitectClient.mask(cells(pos, first), pos, Direction.UP),
                "one neighbour of the face sets the first bit");
        assertEquals(2, ThaumcraftArchitectClient.mask(cells(pos, WardingOverlay.neighbour(pos, Direction.UP, 1)),
                        pos, Direction.UP),
                "the second neighbour of the face sets the second bit");
    }

    @Test void theInnerFacesOfTheAreaAreNotDrawn() {
        BlockPos pos = new BlockPos(4, 5, 6);
        BlockPos above = pos.above();
        Set<BlockPos> area = cells(pos, above);
        assertTrue(ThaumcraftArchitectClient.faceHidden(area, pos, Direction.UP),
                "the top face of the lower block of the area is hidden");
        assertFalse(ThaumcraftArchitectClient.faceHidden(area, pos, Direction.DOWN),
                "the bottom face of the lower block of the area is drawn");
        assertFalse(ThaumcraftArchitectClient.faceHidden(area, above, Direction.UP),
                "the top face of the upper block of the area is drawn");
    }

    @Test void theAlphasAndTheArrowAreTheOriginalOnes() {
        assertEquals(0.2F, ThaumcraftArchitectClient.BLOCK_ALPHA, 1.0E-6F,
                "the overlay block of the original is drawn with the alpha of a fifth");
        assertEquals(0.33F, ThaumcraftArchitectClient.ARROW_ALPHA, 1.0E-6F,
                "the arrow of the original is drawn with the alpha of a third");
        assertEquals(1.0F, ThaumcraftArchitectClient.ARROW_SIZE, 1.0E-6F,
                "the arrow quad of the original is one block wide");
        assertEquals("thaumcraft:textures/misc/architect_arrows.png", ThaumcraftArchitectClient.ARROWS.toString(),
                "the arrow sheet of the original");
    }
}
