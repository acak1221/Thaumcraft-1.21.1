package thaumcraft.client.fx;

import java.util.List;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import thaumcraft.client.fx.bolt.WispLightningShape;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Parity of the native bolt with the original {@code thaumcraft/client/fx/bolt/FXLightningBoltCommon.java} and
 * {@code thaumcraft/client/fx/WRVector3.java} of the TC4 4.2.3.5 oracle.
 *
 * <p>The expected values were produced by a float32 transcription of the original algorithm, taken from the
 * oracle file line by line, and they are the values the original produces for the same seed.</p>
 */
class BoltShapeParityTest {
    private static final Vec3 FROM = new Vec3(0, 0, 0);
    private static final Vec3 TO = new Vec3(3, 1, 2);
    private static final double EPS = 1.0E-4;

    private record NodeBolt(int segments, int maxAge, int notRenderable,
                            double firstEndX, double firstEndY, double firstEndZ) {}

    /**
     * The original {@code ClientProxy.nodeBolt(world, x, y, z, x2, y2, z2)} of the oracle
     * {@code thaumcraft/client/ClientProxy.java:798-803}: the {@code FXLightningBolt} with the duration ten, the
     * multiplier four and the age step five.
     */
    private static final List<NodeBolt> NODE_BOLT = List.of(
            new NodeBolt(424, 13, 0, 0.026410, 0.019324, 0.002933),
            new NodeBolt(288, 13, 0, -0.018067, 0.010658, 0.051690),
            new NodeBolt(400, 7, 0, 0.041515, -0.021870, 0.041884),
            new NodeBolt(392, 5, 0, 0.040631, -0.001623, -0.013937),
            new NodeBolt(232, 7, 0, 0.028631, 0.031738, 0.003336),
            new NodeBolt(392, 7, 0, 0.029255, 0.008476, 0.006215),
            new NodeBolt(256, 11, 0, 0.039636, 0.004276, 0.001149),
            new NodeBolt(304, 9, 0, 0.043876, -0.000798, -0.009803));

    /**
     * The original {@code ClientProxy.bolt(world, source, target)} of the oracle
     * {@code thaumcraft/client/ClientProxy.java:783-788}: the {@code FXLightningBolt} with the age step four and
     * the multiplier {@code 0.4f} of the entity to entity flavour of {@code FXLightningBoltCommon}.
     */
    private static final List<NodeBolt> WISP_BOLT = List.of(
            new NodeBolt(384, 2, 0, 0.025383, 0.007816, 0.012540),
            new NodeBolt(384, 2, 0, 0.023492, 0.009402, 0.014777),
            new NodeBolt(336, 3, 1, 0.025790, 0.004343, 0.013522),
            new NodeBolt(240, 4, 0, 0.024397, 0.009342, 0.013449),
            new NodeBolt(208, 4, 0, 0.022378, 0.007807, 0.017446),
            new NodeBolt(456, 4, 2, 0.020943, 0.005960, 0.019807),
            new NodeBolt(456, 3, 8, 0.023362, 0.006238, 0.016567),
            new NodeBolt(256, 3, 0, 0.022177, 0.008880, 0.017059));

    private static void assertPoint(Vec3 actual, double x, double y, double z) {
        assertEquals(x, actual.x, EPS, "x");
        assertEquals(y, actual.y, EPS, "y");
        assertEquals(z, actual.z, EPS, "z");
    }

    @Test void nodeBoltReproducesTheOriginalFractal() {
        for (int seed = 0; seed < NODE_BOLT.size(); seed++) {
            NodeBolt expected = NODE_BOLT.get(seed);
            WispLightningShape bolt = new WispLightningShape(FROM, TO, seed, 10, 4.0F, 5);
            assertEquals(expected.segments(), bolt.segments.size(), "segments of seed " + seed);
            assertEquals(expected.maxAge(), bolt.maxAge, "max age of seed " + seed);
            assertEquals(128, bolt.mainSegments, "main chain of seed " + seed);
            assertEquals(128, bolt.segments.stream().filter(segment -> segment.split == 0).count(),
                    "main chain segments of seed " + seed);
            assertEquals(5, bolt.increment, "age step of seed " + seed);
            assertEquals(4.0F, bolt.multiplier, "multiplier of seed " + seed);
            long notRenderable = bolt.segments.stream().filter(segment -> !segment.renderable()).count();
            assertEquals(expected.notRenderable(), notRenderable, "not renderable segments of seed " + seed);
            var first = bolt.segments.stream().filter(segment -> segment.number == 0 && segment.split == 0)
                    .findFirst().orElseThrow();
            assertPoint(first.start(), 0, 0, 0);
            assertPoint(first.end(), expected.firstEndX(), expected.firstEndY(), expected.firstEndZ());
            var last = bolt.segments.stream().filter(segment -> segment.number == 127 && segment.split == 0)
                    .findFirst().orElseThrow();
            assertPoint(last.end(), 3, 1, 2);
        }
    }

    @Test void wispBoltUsesTheOriginalEntityFlavour() {
        for (int seed = 0; seed < WISP_BOLT.size(); seed++) {
            NodeBolt expected = WISP_BOLT.get(seed);
            WispLightningShape bolt = new WispLightningShape(FROM, TO, seed, 4);
            assertEquals(expected.segments(), bolt.segments.size(), "segments of seed " + seed);
            assertEquals(expected.maxAge(), bolt.maxAge, "max age of seed " + seed);
            assertEquals(4, bolt.increment, "age step of seed " + seed);
            assertEquals(0.4F, bolt.multiplier, "multiplier of seed " + seed);
            long notRenderable = bolt.segments.stream().filter(segment -> !segment.renderable()).count();
            assertEquals(expected.notRenderable(), notRenderable, "not renderable segments of seed " + seed);
            var first = bolt.segments.stream().filter(segment -> segment.number == 0 && segment.split == 0)
                    .findFirst().orElseThrow();
            assertPoint(first.start(), 0, 0, 0);
            assertPoint(first.end(), expected.firstEndX(), expected.firstEndY(), expected.firstEndZ());
        }
    }

    @Test void theHarnessBoltKeepsTheOriginalDurationAndStep() {
        // The original ModelHoverHarness: FXLightningBolt(world, x, y, z, px, py, pz, seed, 1, 2.0f, 3).
        WispLightningShape bolt = new WispLightningShape(FROM, TO, 0L, 1, 2.0F, 3);
        assertEquals(1, bolt.maxAge, "the original duration one gives the maximum age one");
        assertEquals(3, bolt.increment, "the original age step three");
        assertEquals(424, bolt.segments.size(), "the same fractal topology as the node bolt of the same seed");
    }

    @Test void theCollisionPassNeverRemovesASegment() {
        // The original calculateCollisionAndDiffs stores the largest segment number of every split, so its
        // removal test can never succeed. The port keeps the test and must therefore keep every segment.
        WispLightningShape bolt = new WispLightningShape(FROM, TO, 7L, 10, 4.0F, 5);
        assertEquals(304, bolt.segments.size());
        assertEquals(128, bolt.segments.stream().filter(segment -> segment.split == 0).count());
        assertTrue(bolt.segments.stream().anyMatch(segment -> segment.split != 0), "the forks survive");
    }

    @Test void theForkSegmentsCarryTheOriginalHalfLight() {
        WispLightningShape bolt = new WispLightningShape(FROM, TO, 0L, 10, 4.0F, 5);
        for (var segment : bolt.segments) {
            if (segment.split == 0) assertEquals(1.0F, segment.light, "the main chain keeps the light one");
            else assertTrue(segment.light < 1.0F, "a fork is dimmer than the main chain");
        }
    }
}
