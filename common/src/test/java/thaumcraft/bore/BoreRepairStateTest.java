package thaumcraft.bore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import thaumcraft.common.lib.bore.BoreRepairState;
import static org.junit.jupiter.api.Assertions.*;

class BoreRepairStateTest {
    @Test void postIncrementMakesTheFirstRepairAttemptPrecedeTheFirstFill() {
        var state = BoreRepairState.EMPTY;
        var attempts = new ArrayList<Long>(); var fills = new ArrayList<Long>();
        for (int tick = 0; tick <= 40; tick++) {
            if (state.isAttemptTick()) attempts.add(state.counter());
            state = state.advance();
            if (state.isFillTick()) fills.add(state.counter());
        }
        assertEquals(List.of(0L, 40L), attempts);
        assertEquals(List.of(5L, 10L, 15L, 20L, 25L, 30L, 35L, 40L), fills);
    }
    @Test void partialFundingCannotPayAndFullPriceRefillCanOvershootTheDeficit() {
        var state = BoreRepairState.EMPTY.mergeCost(Map.of("aer", 4, "terra", 8)).addVis("aer", 3).addVis("terra", 8);
        assertFalse(state.canPay()); assertThrows(IllegalStateException.class, state::paid);
        var paid = state.addVis("aer", 4).paid();
        assertEquals(3, paid.available("aer")); assertEquals(0, paid.available("terra"));
        assertEquals(3, state.available("aer")); assertEquals(8, state.available("terra"));
    }
    @Test void originalMaximumCostsSurviveAChangeToACheaperPick() {
        var state = BoreRepairState.EMPTY.mergeCost(Map.of("aer", 4, "terra", 8)).mergeCost(Map.of("aer", 2, "terra", 6, "ignis", 3));
        assertEquals(Map.of("aer", 4, "terra", 8, "ignis", 3), state.cost());
        assertEquals(Map.of(), state.clearCost().cost());
        assertThrows(UnsupportedOperationException.class, () -> state.cost().put("aer", 100));
    }
}
