package thaumcraft.client.renderers.tile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;
import thaumcraft.client.renderers.tile.TileFluxScrubberRenderer.Orientation;
import thaumcraft.client.renderers.tile.TileFluxScrubberRenderer.RotationAxis;

/**
 * The turn and the bob of the cap of the flux scrubber.
 *
 * <p>Original {@code thaumcraft/client/renderers/tile/TileFluxScrubberRenderer.java:45-61}: the
 * {@code translateFromOrientation} of the original turns the facing zero (down) and one (up)
 * around the X axis by minus and plus ninety degrees, the facing three (south), four (west) and
 * five (east) around the Y axis by one hundred and eighty, plus and minus ninety degrees, and
 * the facing two (north) keeps the model as it is.
 *
 * <p>Original {@code TileFluxScrubberRenderer.java:38-40}: the tip of the cap bobs with
 * {@code sin(phase/8)*0.075 + 0.075}, where the phase is the age of the player, the partial tick
 * and the counter of the scrubber.
 *
 * <p>These tests were AUTHORED but NOT EXECUTED: the wave forbids a Gradle run, so the leader
 * has to run {@code :common:test} before the verdicts are trusted.</p>
 */
class FluxScrubberRendererParityTest {
    @Test void theDownAndUpFacingsTiltAroundTheHorizontalAxis() {
        // Original TileFluxScrubberRenderer:47-52.
        Orientation down = TileFluxScrubberRenderer.orientationOf(Direction.DOWN);
        assertEquals(RotationAxis.X, down.axis(), "the facing zero tilts around the X axis");
        assertEquals(-90.0F, down.degrees(), 1.0E-6F);
        Orientation up = TileFluxScrubberRenderer.orientationOf(Direction.UP);
        assertEquals(RotationAxis.X, up.axis(), "the facing one tilts around the X axis");
        assertEquals(90.0F, up.degrees(), 1.0E-6F);
    }

    @Test void theNorthFacingKeepsTheModelAsItIs() {
        // Original TileFluxScrubberRenderer:53 - the facing two is not turned.
        Orientation north = TileFluxScrubberRenderer.orientationOf(Direction.NORTH);
        assertEquals(RotationAxis.NONE, north.axis(), "the facing two is not turned");
        assertEquals(0.0F, north.degrees(), 1.0E-6F);
    }

    @Test void theSideFacingsTurnAroundTheVerticalAxis() {
        // Original TileFluxScrubberRenderer:54-59 - the three side facings turn
        // around the Y axis, not around the Z axis.
        Orientation south = TileFluxScrubberRenderer.orientationOf(Direction.SOUTH);
        assertEquals(RotationAxis.Y, south.axis(), "the facing three turns around the Y axis");
        assertEquals(180.0F, south.degrees(), 1.0E-6F);
        Orientation west = TileFluxScrubberRenderer.orientationOf(Direction.WEST);
        assertEquals(RotationAxis.Y, west.axis(), "the facing four turns around the Y axis");
        assertEquals(90.0F, west.degrees(), 1.0E-6F);
        Orientation east = TileFluxScrubberRenderer.orientationOf(Direction.EAST);
        assertEquals(RotationAxis.Y, east.axis(), "the facing five turns around the Y axis");
        assertEquals(-90.0F, east.degrees(), 1.0E-6F);
    }

    @Test void theTipOfTheCapBobsBetweenZeroAndATenthAndAHalf() {
        // Original TileFluxScrubberRenderer:39.
        for (int phase = 0; phase < 64; phase++) {
            float bob = TileFluxScrubberRenderer.bobFor(phase);
            assertTrue(bob >= 0.0F && bob <= 0.15F, "the bob of the tip is between zero and 0.15");
        }
        assertEquals(0.075F, TileFluxScrubberRenderer.bobFor(0.0F), 1.0E-3F,
                "the sine of zero is zero, so the bob starts at half of its amplitude");
    }
}
