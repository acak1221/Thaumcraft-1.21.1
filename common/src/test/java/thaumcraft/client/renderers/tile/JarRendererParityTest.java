package thaumcraft.client.renderers.tile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * The original brain jar and node jar.
 *
 * <p>Oracle {@code thaumcraft/client/renderers/tile/TileJarRenderer.java:155-175} ({@code renderBrain}):
 * the brain bobs with {@code sin(ticksSinceLevel / 14) * 0.03 + 0.03}, it sits {@code 0.8} below the
 * centre of the content frame, it is turned by the interpolated {@code rota}/{@code rotb} around the Z
 * axis and by minus ninety degrees around the Y axis, and it is drawn with the scale {@code 0.4} on
 * the {@code brain2.png} sheet. The brine of the jar is the {@code jarbrine.png} volume.</p>
 *
 * <p>The sine of the original is the 1.7.10 {@code MathHelper.func_76126_a}, the 65536 entry
 * lookup table {@code SIN[(int)(f * 10430.378f) & 0xFFFF]} with
 * {@code SIN[i] = (float)Math.sin((double)i * Math.PI * 2.0 / 65536.0)}. The native
 * {@code Mth.sin} is that same table, so the native bob is the exact float of the original and
 * not a {@code Math.sin}. The table is negative in the second half of every period, so the
 * original bob runs from zero to {@code 0.06} and it is <b>not</b> bounded below by
 * {@code 0.03}.</p>
 *
 * <p>Oracle {@code thaumcraft/client/renderers/tile/TileJarRenderer.java:160-166}: the difference
 * of the two angles is wrapped into one circle and the drawn angle is
 * {@code rotb + difference * partialTick}, so the turn of the brain is the absolute interpolated
 * angle and not the difference of the two angles.</p>
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
        // Oracle TileJarRenderer.java:157 - MathHelper.func_76126_a(ticksExisted / 14.0f) * 0.03f + 0.03f.
        assertEquals(.03F, TileJarBrainRenderer.bob(0), 1.0E-6F);
        // The exact floats of the original 1.7.10 sine table.
        assertEquals(0.04438229F, TileJarBrainRenderer.bob(7), 1.0E-6F);
        assertEquals(0.05524354F, TileJarBrainRenderer.bob(14), 1.0E-6F);
        assertEquals(0.059999995F, TileJarBrainRenderer.bob(22), 1.0E-6F);
        assertEquals(0.029962609F, TileJarBrainRenderer.bob(44), 1.0E-6F);
        assertEquals(0.057279825F, TileJarBrainRenderer.bob(28), 1.0E-6F);
        // The sine of the original is negative in the second half of every period, so the brain
        // sinks to zero every period: the bob is NOT bounded below by 0.03.
        assertEquals(5.029142E-8F, TileJarBrainRenderer.bob(66), 1.0E-12F);
        for (int tick = 0; tick < 512; tick++) {
            float bob = TileJarBrainRenderer.bob(tick);
            assertTrue(bob >= -1.0E-6F && bob <= .06F + 1.0E-6F,
                    "the original brain bob runs between 0 and 0.06, not " + bob);
        }
    }

    @Test void theBrainTurnWrapsTheDifferenceIntoOneCircle() {
        // The original: the difference of the two angles is wrapped into [-pi, pi] and the drawn
        // angle is rotb + difference * partialTick - the absolute angle of the brain.
        assertEquals(1F, TileJarBrainRenderer.turn(1F, 1F, .5F), 1.0E-6F);
        assertEquals(1.5F, TileJarBrainRenderer.turn(2F, 1F, .5F), 1.0E-6F);
        // 5 - 1 = 4 is above pi, so it wraps to 4 - 6.283185.
        float wrapped = TileJarBrainRenderer.turn(5F, 1F, 1F);
        assertTrue(wrapped < 3.141593F && wrapped > -3.141593F,
                "the original wraps the turn into one circle, not " + wrapped);
        assertEquals(1F + (4F - 6.283185F), wrapped, 1.0E-6F);
        // -5 - 1 = -6 is below -pi, so it wraps to -6 + 6.283185.
        assertEquals(1F + (-6F + 6.283185F), TileJarBrainRenderer.turn(-5F, 1F, 1F), 1.0E-6F);
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
