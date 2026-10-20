package thaumcraft.warp;

import org.junit.jupiter.api.Test;
import thaumcraft.common.lib.loot.OriginalMobLoot;
import static org.junit.jupiter.api.Assertions.*;

class OriginalMobLootTest {
    @Test void rareGateUsesOneIntegerOfTwoHundredAndOriginalStrictBoundary() {
        for (int looting : new int[]{0, 1, 3, 5}) {
            final int level = looting;
            assertTrue(OriginalMobLoot.rare(level, bound -> { assertEquals(200, bound); return level + 4; }));
            assertFalse(OriginalMobLoot.rare(level, bound -> { assertEquals(200, bound); return level + 5; }));
        }
    }
    @Test void brainGateIsInclusiveFourAfterLootingNotTheOrdinaryZombieTenPercentRoll() {
        for (int looting : new int[]{0, 1, 3, 4}) {
            final int level = looting;
            assertTrue(OriginalMobLoot.brain(level, bound -> { assertEquals(10, bound); return level + 4; }));
            assertFalse(OriginalMobLoot.brain(level, bound -> { assertEquals(10, bound); return level + 5; }));
        }
    }
    @Test void highLootingStillConsumesTheOriginalRollInsteadOfShortCircuiting() {
        int[] calls = {0};
        assertTrue(OriginalMobLoot.rare(200, bound -> { calls[0]++; assertEquals(200, bound); return 199; }));
        assertTrue(OriginalMobLoot.brain(10, bound -> { calls[0]++; assertEquals(10, bound); return 9; }));
        assertEquals(2, calls[0]);
    }
}
