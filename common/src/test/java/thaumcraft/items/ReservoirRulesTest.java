package thaumcraft.items;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;
import thaumcraft.common.lib.essentia.ReservoirRules;

/** The numbers of the Essentia Reservoir (original TileEssentiaReservoir and BlockEssentiaReservoir) without a world. */
class ReservoirRulesTest {
    @Test void theNumbersAreTheOriginals() {
        assertEquals(256, ReservoirRules.CAPACITY);
        assertEquals(24, ReservoirRules.SUCTION);
        assertEquals(5, ReservoirRules.PULL_INTERVAL);
        assertEquals(16, ReservoirRules.BURST_UNIT);
        assertEquals(50, ReservoirRules.BURST_TRIES);
        assertEquals(5, ReservoirRules.BURST_SPREAD);
        assertEquals(8, ReservoirRules.FLUX_LEVEL);
        assertEquals(1.0F, ReservoirRules.BURST_POWER);
    }

    @Test void itWantsEssentiaWithSuction24UntilItIsFull() {
        assertEquals(24, ReservoirRules.suction(0));
        assertEquals(24, ReservoirRules.suction(255));
        assertEquals(0, ReservoirRules.suction(256), "a full reservoir asks for nothing");
        assertEquals(256, ReservoirRules.space(0));
        assertEquals(1, ReservoirRules.space(255));
        assertEquals(0, ReservoirRules.space(256));
    }

    @Test void theComparatorReadsFourteenStepsPlusOneForAnyStock() {
        assertEquals(0, ReservoirRules.comparator(0));
        assertEquals(1, ReservoirRules.comparator(1), "one unit is already 1");
        assertEquals(1, ReservoirRules.comparator(18), "floor(18 / 256 * 14) = 0, plus one");
        assertEquals(2, ReservoirRules.comparator(19), "floor(19 / 256 * 14) = 1, plus one");
        assertEquals(8, ReservoirRules.comparator(128), "half of 14 is 7, plus one");
        assertEquals(14, ReservoirRules.comparator(240), "floor(240 / 256 * 14) = 13");
        assertEquals(15, ReservoirRules.comparator(256), "full is 15");
        for (int units = 1; units < 256; units++) assertTrue(ReservoirRules.comparator(units) >= ReservoirRules.comparator(units - 1), "monotone at " + units);
    }

    @Test void aReservoirThatHeldSixteenUnitsBurstsWithOneSprayPerSixteen() {
        assertEquals(0, ReservoirRules.sprays(0));
        assertEquals(0, ReservoirRules.sprays(15), "under 16 nothing bursts");
        assertEquals(1, ReservoirRules.sprays(16));
        assertEquals(1, ReservoirRules.sprays(31));
        assertEquals(2, ReservoirRules.sprays(32));
        assertEquals(16, ReservoirRules.sprays(256));
    }

    @Test void theBurstStopsAfterSpraysPlusOnePlacements() {
        for (int sprays = 1; sprays <= 16; sprays++) {
            int placed = 0;
            while (true) { if (ReservoirRules.burstDone(placed++, sprays)) break; }
            assertEquals(sprays + 1, placed, "sprays " + sprays);
        }
    }

    @Test void theBurstOffsetTakesTheFirstDrawMinusTheSecond() {
        List<Integer> bounds = new ArrayList<>();
        int[] draws = {4, 1};
        int[] index = {0};
        int offset = ReservoirRules.burstOffset(bound -> { bounds.add(bound); return draws[index[0]++]; });
        assertEquals(3, offset, "4 - 1");
        assertEquals(List.of(5, 5), bounds, "two draws below 5");
        index[0] = 0;
        draws[0] = 0; draws[1] = 4;
        assertEquals(-4, ReservoirRules.burstOffset(bound -> draws[index[0]++]), "0 - 4: the first draw comes first");
    }

    @Test void flowsBelowItsOwnLevelAreGooAndTheRestGas() {
        assertTrue(ReservoirRules.gooAt(63, 64));
        assertFalse(ReservoirRules.gooAt(64, 64), "its own level is gas");
        assertFalse(ReservoirRules.gooAt(65, 64));
    }

    @Test void theWandOpensTowardTheClickedSideWhileSneakingAndAwayFromItOtherwise() {
        for (Direction side : Direction.values()) {
            assertEquals(side, ReservoirRules.wandFacing(side, true));
            assertEquals(side.getOpposite(), ReservoirRules.wandFacing(side, false));
            assertEquals(side.getOpposite(), ReservoirRules.placedFacing(side), "a placed block opens toward what was clicked");
        }
    }

    @Test void theCreakGetsMoreLikelyAsItFills() {
        assertEquals(499, ReservoirRules.creakBound(1));
        assertEquals(244, ReservoirRules.creakBound(256));
        assertTrue(ReservoirRules.creaks(0));
        assertFalse(ReservoirRules.creaks(1));
    }
}
