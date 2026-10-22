package thaumcraft.common.lib.world;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * The sparkles around the portable hole of the original (TC4 4.2.3.5) against the 1.21.1 port.
 *
 * <p>Every expectation below is a call of the original
 * ({@code thaumcraft/common/tiles/TileHole.java:145-210}); the rule is a pure function of the
 * six neighbours, so no world and no block is needed.</p>
 */
class HoleSparkleRulesParityTest {

    private static HoleSparkleRules.Neighbours neighbours(boolean upOpaque, boolean eastOpaque,
            boolean southOpaque, boolean downOpaque, boolean westOpaque, boolean northOpaque) {
        return new HoleSparkleRules.Neighbours(upOpaque, eastOpaque, southOpaque, downOpaque,
                westOpaque, northOpaque, false, false, false, false, false, false);
    }

    /**
     * The hole stands in the ground: the block below and the two sides are opaque, the block
     * above and the two other sides are not. The original draws eight sparkles there.
     */
    @Test void theSparklesOfTheHoleAreTheEdgesThatTouchASolidBlock() {
        List<HoleSparkleRules.Sparkle> sparkles = HoleSparkleRules.sparkles(
                neighbours(false, true, false, true, true, false));
        assertEquals(8, sparkles.size(), "the eight edges of the original");
        assertEquals(new HoleSparkleRules.Sparkle(0, 0, 1, HoleSparkleRules.Axis.X), sparkles.get(0));
        assertEquals(new HoleSparkleRules.Sparkle(0, 0, 0, HoleSparkleRules.Axis.X), sparkles.get(1));
        assertEquals(new HoleSparkleRules.Sparkle(1, 1, 0, HoleSparkleRules.Axis.Z), sparkles.get(2));
        assertEquals(new HoleSparkleRules.Sparkle(1, 0, 1, HoleSparkleRules.Axis.Y), sparkles.get(3));
        assertEquals(new HoleSparkleRules.Sparkle(1, 0, 0, HoleSparkleRules.Axis.Y), sparkles.get(4));
        assertEquals(new HoleSparkleRules.Sparkle(0, 1, 0, HoleSparkleRules.Axis.Z), sparkles.get(5));
        assertEquals(new HoleSparkleRules.Sparkle(0, 0, 1, HoleSparkleRules.Axis.Y), sparkles.get(6));
        assertEquals(new HoleSparkleRules.Sparkle(0, 0, 0, HoleSparkleRules.Axis.Y), sparkles.get(7));
    }

    /**
     * A hole that touches nothing solid has no sparkle: every call of the original asks for a
     * vertical neighbour that is an opaque cube.
     */
    @Test void aHoleWithoutASolidNeighbourHasNoSparkle() {
        assertEquals(0, HoleSparkleRules.sparkles(neighbours(false, false, false, false, false, false)).size());
        assertEquals(0, HoleSparkleRules.sparkles(neighbours(true, true, true, true, true, true)).size());
    }

    /**
     * A neighbour that is the hole block itself suppresses the sparkles of that side: the b1 to
     * b6 flags of the original ask whether the neighbour is not the hole block.
     */
    @Test void aHoleNeighbourSuppressesTheSparklesOfThatSide() {
        // The hole stands in the ground and the block at +X is not opaque.
        HoleSparkleRules.Neighbours open = neighbours(false, false, false, true, true, false);
        assertEquals(6, HoleSparkleRules.sparkles(open).size(),
                "the east side asks for the b6 flag and the b6 flag is true");
        // The same hole, but the block at +X is a hole block as well: the b6 flag is false and the
        // sparkle of the east edge is gone.
        HoleSparkleRules.Neighbours withHoleEast = new HoleSparkleRules.Neighbours(
                false, false, false, true, true, false,
                false, false, false, false, false, true);
        assertEquals(5, HoleSparkleRules.sparkles(withHoleEast).size(),
                "the sparkle of the east edge asks for the b6 flag");
    }
}
