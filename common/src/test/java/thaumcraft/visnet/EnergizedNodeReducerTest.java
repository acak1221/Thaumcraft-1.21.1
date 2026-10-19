package thaumcraft.visnet;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.lib.visnet.EnergizedNodeReducer;
import static org.junit.jupiter.api.Assertions.*;

class EnergizedNodeReducerTest {
    @Test void originalMaxReturnMergesDoNotBorrowTheAcceptedSummingHelper() {
        var input = new AspectList().add(Aspect.EARTH, 9).add(Aspect.METAL, 16);
        var result = EnergizedNodeReducer.reduceToPrimals(input);
        assertEquals(16, result.getAmount(Aspect.EARTH));
        assertEquals(16, result.getAmount(Aspect.ORDER));
        assertEquals(2, result.size());
        // Accepted API remains unchanged; the source deliberately uses its own
        // anchored MAX reducer rather than silently changing every old caller.
        assertEquals(41, input.reduceToPrimals(true).getAmount(Aspect.EARTH));
        assertEquals(9, input.getAmount(Aspect.EARTH));
        assertEquals(16, input.getAmount(Aspect.METAL));
        result.aspects.clear();
        assertEquals(16, EnergizedNodeReducer.reduceToPrimals(input).getAmount(Aspect.EARTH));
    }

    @Test void identicalImmediateParentsAreAddedBeforeRecursiveMaxMerges() {
        var doubled = new Aspect("visnet_test_double_parent", 0x123456, new Aspect[]{Aspect.AIR, Aspect.AIR});
        try {
            var input = new AspectList().add(Aspect.AIR, 9).add(doubled, 7);
            var result = EnergizedNodeReducer.reduceToPrimals(input);
            assertEquals(14, result.getAmount(Aspect.AIR));
            assertEquals(1, result.size());
            assertEquals(7, input.getAmount(doubled));
            assertThrows(IllegalArgumentException.class, () -> EnergizedNodeReducer.reduceToPrimals(new AspectList().add(doubled, Integer.MAX_VALUE)));
        } finally { Aspect.aspects.remove(doubled.getTag(), doubled); }
    }

    @Test void iterativeSharedDagDoesNotNeedTheLegacyRecursiveStackOrAnArbitraryDepthCap() {
        var created = new ArrayList<Aspect>();
        try {
            Aspect previous = Aspect.FIRE;
            for (int i = 0; i < 1100; i++) {
                previous = new Aspect("visnet_test_deep_" + i, 0x123456, new Aspect[]{Aspect.AIR, previous});
                created.add(previous);
            }
            var result = EnergizedNodeReducer.reduceToPrimals(new AspectList().add(previous, 13).add(Aspect.FIRE, 3));
            assertEquals(13, result.getAmount(Aspect.AIR));
            assertEquals(13, result.getAmount(Aspect.FIRE));
            assertEquals(List.of(Aspect.AIR, Aspect.FIRE), new ArrayList<>(result.aspects.keySet()));
        } finally { for (Aspect value : created) Aspect.aspects.remove(value.getTag(), value); }
    }

    @Test void invalidCanonicalInputsAndCyclesHaveNoUsablePartialReduction() {
        for (int amount : List.of(0, -1)) {
            var invalid = new AspectList().add(Aspect.FIRE, 8).add(Aspect.AIR, amount);
            assertThrows(IllegalArgumentException.class, () -> EnergizedNodeReducer.reduceToPrimals(invalid));
        }
        var cyclic = new Aspect("visnet_test_cycle", 0x123456, new Aspect[]{Aspect.AIR, Aspect.FIRE}) {
            @Override public Aspect[] getComponents() { return new Aspect[]{this, Aspect.AIR}; }
        };
        try {
            assertThrows(IllegalArgumentException.class, () -> EnergizedNodeReducer.reduceToPrimals(new AspectList().add(Aspect.FIRE, 8).add(cyclic, 3)));
            Aspect.aspects.remove(cyclic.getTag(), cyclic);
            assertThrows(IllegalArgumentException.class, () -> EnergizedNodeReducer.checkedCopy(new AspectList().add(cyclic, 3)));
        } finally { Aspect.aspects.remove(cyclic.getTag(), cyclic); }
        assertThrows(IllegalArgumentException.class, () -> EnergizedNodeReducer.checkedCopy(null));
    }
}
