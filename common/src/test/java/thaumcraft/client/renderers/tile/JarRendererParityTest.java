package thaumcraft.client.renderers.tile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * The original brain jar and node jar.
 *
 * <p>Oracle {@code thaumcraft/client/renderers/tile/TileJarRenderer.java:155-175} ({@code renderBrain}):
 * the brain bobs with {@code sin(ticksSinceLevel / 14) * 0.03 + 0.03}, it sits {@code 0.8} below the
 * centre of the content frame, it is turned by the interpolated {@code rota}/{@code rotb} around the Y
 * axis and by minus ninety degrees around the Y axis as well, and it is drawn with the scale {@code 0.4} on
 * the {@code brain2.png} sheet. The brine of the jar is the {@code jarbrine.png} volume.</p>
 *
 * <p>Oracle {@code thaumcraft/client/renderers/tile/TileJarRenderer.java:60-64}: the content frame
 * of the jar is the block centre, one hundredth of a block above the block floor, turned by one
 * hundred and eighty degrees around the X axis.</p>
 *
 * <p>Oracle {@code thaumcraft/client/renderers/tile/TileNodeRenderer.java:186-189}: the node of a
 * node jar is always visible, it has the size {@code 0.7f} and it keeps the depth test.</p>
 */
class JarRendererParityTest {
    @Test void theBrainBobsWithTheOriginalSine() {
        // sin(0/14) * 0.03 + 0.03
        assertEquals(.03F, TileJarBrainRenderer.bob(0), 1.0E-6F);
        int[] ticks = {1, 14, 22, 44, 100, 511};
        float[] expected = {.032140955F, .05524354F, .059999995F, .029962609F, .05272753F, .0020476487F};
        for (int i = 0; i < ticks.length; i++)
            assertEquals(expected[i], TileJarBrainRenderer.bob(ticks[i]), 1e-7F);
        // The sine of the original is -1 at the tick 66, so the bob of the original is 0 there.
        assertEquals(0F, TileJarBrainRenderer.bob(66), 1.0E-4F,
                "the bob of the original is 0 at the tick 66");
    }

    @Test void theBrainTurnWrapsTheDifferenceIntoOneCircle() {
        // The original TileJarRenderer.java:160-165: the difference of the two angles is wrapped
        // into [-pi, pi] and the turn is the second angle plus the wrapped difference times the
        // frame. Two equal angles therefore turn to the angle itself.
        assertEquals(1F, TileJarBrainRenderer.turn(1F, 1F, .5F), 1.0E-6F);
        assertEquals(1.5F, TileJarBrainRenderer.turn(2F, 1F, .5F), 1.0E-6F);
        // 5 - 1 = 4 is above pi, so it wraps to 4 - 2*pi.
        float wrapped = TileJarBrainRenderer.turn(5F, 1F, 1F);
        assertTrue(wrapped < 3.141593F && wrapped > -3.141593F,
                "the original wraps the turn into one circle, not " + wrapped);
        assertEquals(-1.283185F, wrapped, 0F);
        assertEquals(7.283185F, TileJarBrainRenderer.turn(1F, 5F, 1F), 0F);
        assertEquals(-.1415925F, TileJarBrainRenderer.turn(5F, 1F, .5F), 0F);
    }

    @Test void theContentFrameAndTheBrainScaleAreTheOriginalOnes() {
        assertEquals(.01F, TileJarBrainRenderer.CONTENT_OFFSET, 1.0E-6F,
                "the original content frame is one hundredth of a block above the floor");
        assertEquals(.4F, TileJarBrainRenderer.BRAIN_SCALE, 1.0E-6F,
                "the original brain is drawn with the scale 0.4");
        assertEquals(-.8F, TileJarBrainRenderer.BRAIN_HEIGHT, 1.0E-6F,
                "the original brain sits 0.8 below the content frame");
    }

    @Test void theNodeOfTheJarKeepsTheOriginalSizeAndDistance() {
        assertEquals(.7F, TileNodeRenderer.JAR_NODE_SIZE, 1.0E-6F,
                "the original node jar draws its node with the size 0.7f");
        assertEquals(64.0D, TileNodeRenderer.JAR_VIEW_DISTANCE, 1.0E-9D,
                "the original node jar keeps the sixty four blocks of a node");
    }
}
