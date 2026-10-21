package thaumcraft.client.renderers.tile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

/**
 * The glow of the arcane levitator, checked against the original renderer.
 *
 * <p>Original {@code BlockLifterRenderer} (oracle
 * {@code thaumcraft/client/renderers/block/BlockLifterRenderer.java:40-79}):
 * the top face of the levitator is drawn in the colour
 * {@code BlockCustomOreItem.colors[4]} and the four side faces in the colour
 * {@code 14488063}. The colour table of the original is
 * {@code {16777215, 16777086, 16727041, 37119, 40960, 15650047, 5592439}}
 * (oracle {@code thaumcraft/common/blocks/BlockCustomOreItem.java:17}), so the
 * top face is 40960 = 0x00A000, a green of 160, and the side faces are
 * 14488063 = 0xDD11FF, a magenta of 221, 17 and 255.
 *
 * <p>The original draws the +X face when the +X neighbour is renderable, the
 * -X face when the -X neighbour is renderable and so on
 * ({@code BlockLifterRenderer:59-73}), so the panel always belongs to the side
 * that is open. The brightness of every face is the fixed lightmap coordinate
 * 180 while the levitator is not powered ({@code BlockLifterRenderer:48-53}).
 */
class LevitatorGlowParityTest {
    /** The colour table of the original, oracle BlockCustomOreItem:17. */
    private static final int[] ORIGINAL_ORE_COLOURS =
            {16777215, 16777086, 16727041, 37119, 40960, 15650047, 5592439};
    /** The literal of the original side colour, oracle BlockLifterRenderer:58. */
    private static final int ORIGINAL_SIDE_COLOUR = 14488063;

    @Test void theTopPanelKeepsTheOriginalOreColour() {
        int original = ORIGINAL_ORE_COLOURS[4];
        assertEquals(40960, original, "the original colour table is the one of the 1.7.10 original");
        assertEquals((original >> 16) & 255, (TileLevitatorRenderer.TOP_COLOUR >> 16) & 255, "red of the top panel");
        assertEquals((original >> 8) & 255, (TileLevitatorRenderer.TOP_COLOUR >> 8) & 255, "green of the top panel");
        assertEquals(original & 255, TileLevitatorRenderer.TOP_COLOUR & 255, "blue of the top panel");
    }

    @Test void theSidePanelsKeepTheOriginalMagenta() {
        assertEquals(0xDD11FF, ORIGINAL_SIDE_COLOUR, "the original side colour is 14488063");
        assertEquals(221, (TileLevitatorRenderer.SIDE_COLOUR >> 16) & 255, "red of the side panel");
        assertEquals(17, (TileLevitatorRenderer.SIDE_COLOUR >> 8) & 255, "green of the side panel");
        assertEquals(255, TileLevitatorRenderer.SIDE_COLOUR & 255, "blue of the side panel");
        assertNotEquals(TileLevitatorRenderer.TOP_COLOUR, TileLevitatorRenderer.SIDE_COLOUR,
                "the top panel and the side panels of the original are two different colours");
    }

    @Test void thePanelOfANeighbourIsDrawnOnTheSideOfThatNeighbour() {
        // Original BlockLifterRenderer:59-73: the +X face for the +X neighbour,
        // the -X face for the -X neighbour, the +Z face for the +Z neighbour and
        // the -Z face for the -Z neighbour.
        assertEquals(Direction.EAST, TileLevitatorRenderer.panelFace(Direction.EAST));
        assertEquals(Direction.WEST, TileLevitatorRenderer.panelFace(Direction.WEST));
        assertEquals(Direction.SOUTH, TileLevitatorRenderer.panelFace(Direction.SOUTH));
        assertEquals(Direction.NORTH, TileLevitatorRenderer.panelFace(Direction.NORTH));
    }

    @Test void thePanelSitsOnItsOwnSideOfTheBlock() {
        // The original draws the +X panel at x + 0.99 and the -X panel at
        // x + 0.01, the port keeps the opaque cube of the levitator and draws
        // them one hundredth of a block outside their own face.
        assertEquals(1.01F, TileLevitatorRenderer.panelOffset(Direction.EAST), 1.0E-6F);
        assertEquals(-0.01F, TileLevitatorRenderer.panelOffset(Direction.WEST), 1.0E-6F);
        assertEquals(1.01F, TileLevitatorRenderer.panelOffset(Direction.SOUTH), 1.0E-6F);
        assertEquals(-0.01F, TileLevitatorRenderer.panelOffset(Direction.NORTH), 1.0E-6F);
        assertEquals(1.01F, TileLevitatorRenderer.panelOffset(Direction.UP), 1.0E-6F);
    }

    @Test void theActiveLevitatorKeepsTheOriginalLightmapCoordinate() {
        // Original BlockLifterRenderer:48-53: the fixed lightmap coordinate 180
        // while the levitator is not powered.
        assertEquals(180, TileLevitatorRenderer.ACTIVE_LIGHT);
        assertEquals(4, TileLevitatorRenderer.ACTIVE_LIGHT % 16, "the block light of the coordinate 180");
        assertEquals(11, TileLevitatorRenderer.ACTIVE_LIGHT / 16, "the sky light of the coordinate 180");
    }
}
