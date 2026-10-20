package thaumcraft.research;

import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.lib.research.DeconstructionRules;

class DeconstructionRulesTest {
    @Test void overlappingComponentPathsSumInsteadOfTakingTheirMaximum() {
        var input = new AspectList().add(Aspect.METAL, 3).add(Aspect.EARTH, 2);
        var primals = DeconstructionRules.primals(input);
        // Original metallum = terra + vitreus; vitreus = terra + ordo.
        // Three metal therefore contributes SIX earth, plus the direct two.
        assertEquals(8, primals.getAmount(Aspect.EARTH)); assertEquals(3, primals.getAmount(Aspect.ORDER));
        assertEquals(11, primals.visSize()); assertEquals(2, primals.size());
        assertEquals(3, input.getAmount(Aspect.METAL)); assertEquals(2, input.getAmount(Aspect.EARTH));
    }
    @Test void everyBuiltInCompoundMatchesOriginalRecursiveSumReduction() {
        for (Aspect aspect : Aspect.aspects.values()) {
            var input = new AspectList().add(aspect, 3).add(Aspect.AIR, 1);
            assertEquals(input.reduceToPrimals(false).aspects, DeconstructionRules.primals(input).aspects, aspect.getTag());
        }
    }
    @Test void chanceThresholdIsStrictAndFailureDoesNotDrawARewardType() {
        var calls = new ArrayList<Integer>();
        var primals = new AspectList().add(Aspect.EARTH, 5).add(Aspect.ORDER, 3);
        assertNull(DeconstructionRules.roll(primals, bound -> { calls.add(bound); return 8; }));
        assertEquals(List.of(80), calls);
        calls.clear();
        assertEquals(Aspect.ORDER, DeconstructionRules.roll(primals, bound -> { calls.add(bound); return calls.size() == 1 ? 7 : 1; }));
        assertEquals(List.of(80, 2), calls);
    }
    @Test void rewardTypesAreUniformEvenWhenTheirQuantitiesAreVeryDifferent() {
        var primals = new AspectList().add(Aspect.AIR, 100).add(Aspect.FIRE, 1);
        var bounds = new ArrayList<Integer>();
        assertEquals(Aspect.FIRE, DeconstructionRules.roll(primals, bound -> { bounds.add(bound); return bounds.size() == 1 ? 79 : 1; }));
        assertEquals(List.of(80, 2), bounds);
        assertNull(DeconstructionRules.roll(new AspectList(), bound -> { fail("Empty input consumes no RNG"); return 0; }));
    }
    @Test void malformedAmountsAndUnrepresentablePathsFailBeforeConsumption() {
        assertThrows(IllegalArgumentException.class, () -> DeconstructionRules.primals(new AspectList().add(Aspect.AIR, -1)));
        assertThrows(IllegalArgumentException.class, () -> DeconstructionRules.primals(new AspectList().add(Aspect.METAL, Integer.MAX_VALUE)));
        var cyclic = new Aspect("deconstruction_test_cycle", 0x123456, new Aspect[]{Aspect.AIR, Aspect.FIRE}) {
            @Override public Aspect[] getComponents() { return new Aspect[]{this, Aspect.AIR}; }
        };
        try { assertThrows(IllegalArgumentException.class, () -> DeconstructionRules.primals(new AspectList().add(cyclic, 1))); }
        finally { Aspect.aspects.remove(cyclic.getTag()); }
    }
}
