package thaumcraft.warp;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import thaumcraft.common.lib.WarpRules;

/** The warp-dependent potion strength: one amplifier step per fifteen warp, capped at three. */
class WarpAmplifierTest {
    @Test void theAmplifierRisesOneStepPerFifteenWarpAndStopsAtThree() {
        int[][] cases = {{0, 0}, {1, 0}, {14, 0}, {15, 1}, {29, 1}, {30, 2}, {44, 2}, {45, 3}, {46, 3}, {60, 3}, {100, 3}, {Integer.MAX_VALUE, 3}};
        for (int[] c : cases) assertEquals(c[1], WarpRules.amplifier(c[0]), "warp " + c[0]);
    }

    @Test void theGazeRangeGrowsWithTheAmplifierByThreeBlocksUpToTwentyFour() {
        assertEquals(8, WarpRules.gazeRange(WarpRules.amplifier(0)));
        assertEquals(11, WarpRules.gazeRange(WarpRules.amplifier(15)));
        assertEquals(14, WarpRules.gazeRange(WarpRules.amplifier(30)));
        assertEquals(17, WarpRules.gazeRange(WarpRules.amplifier(45)));
        assertEquals(17, WarpRules.gazeRange(WarpRules.amplifier(500)), "warp past 45 adds nothing more");
    }
}
