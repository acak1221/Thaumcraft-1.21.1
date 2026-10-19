package thaumcraft.foci;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.lib.foci.FocalUpgradeCosts;
import static org.junit.jupiter.api.Assertions.*;

class FocalUpgradeCostsTest {
    @Test void literalOriginalCostVectorsAndLevelPrices() {
        vector(Aspect.HUNGER, 1, 800, 200, 200, 0, 200, 0, 200);
        vector(Aspect.WEAPON, 1, 2400, 200, 600, 400, 600, 400, 200);
        vector(Aspect.GREED, 1, 2800, 400, 800, 200, 800, 200, 400);
        vector(Aspect.TRAVEL, 2, 1200, 400, 400, 0, 0, 400, 0);
        // Planner semantics are not an admission whitelist: MINE/GREED also
        // prove future original costs without offering paid Dowsing/Silk now.
        vector(Aspect.MINE, 3, 8800, 800, 3200, 800, 2400, 800, 800);
        vector(Aspect.GREED, 5, 44800, 6400, 12800, 3200, 12800, 3200, 6400);
        for (int rank = 1; rank <= 5; rank++) {
            var plan = FocalUpgradeCosts.plan(new AspectList().add(Aspect.AIR, 1), rank);
            assertEquals(rank * 8, plan.xpLevels());
            assertEquals(200 << (rank - 1), plan.initialTotal());
            assertEquals(rank, plan.rank());
        }
    }

    @Test void declaredValuesAreIgnoredButDistinctKeysAreSummedAndCopied() {
        var declared = new AspectList().add(Aspect.HUNGER, -7).add(Aspect.AIR, 0);
        var plan = FocalUpgradeCosts.plan(declared, 1);
        assertEquals(400, plan.aspects().getAmount(Aspect.AIR));
        assertEquals(1000, plan.initialTotal());
        assertEquals(plan.initialTotal(), plan.total());
        assertEquals(List.of(Aspect.WATER, Aspect.EARTH, Aspect.AIR, Aspect.ENTROPY), new ArrayList<>(plan.aspects().aspects.keySet()));
        declared.aspects.clear(); plan.aspects().aspects.clear();
        assertEquals(1000, plan.aspects().visSize());
        for (int rank : new int[]{-1, 0, 6, Integer.MAX_VALUE})
            assertThrows(IllegalArgumentException.class, () -> FocalUpgradeCosts.plan(new AspectList().add(Aspect.AIR, 1), rank));
        assertThrows(IllegalArgumentException.class, () -> FocalUpgradeCosts.plan(new AspectList(), 1));
        assertThrows(IllegalArgumentException.class, () -> FocalUpgradeCosts.plan((AspectList) null, 1));
    }

    @Test void overlapsAndIdenticalParentsSumRatherThanTakingEnergizedMaximum() {
        var input = new AspectList().add(Aspect.EARTH, 9).add(Aspect.METAL, 16);
        var result = FocalUpgradeCosts.reduceToPrimals(input);
        assertEquals(41, result.getAmount(Aspect.EARTH));
        assertEquals(16, result.getAmount(Aspect.ORDER));
        assertEquals(9, input.getAmount(Aspect.EARTH));
        var doubled = new Aspect("focal_cost_double", 0x123456, new Aspect[]{Aspect.AIR, Aspect.AIR});
        try {
            result = FocalUpgradeCosts.reduceToPrimals(new AspectList().add(doubled, 7).add(Aspect.AIR, 9));
            assertEquals(23, result.getAmount(Aspect.AIR));
            assertThrows(IllegalArgumentException.class, () -> FocalUpgradeCosts.reduceToPrimals(new AspectList().add(doubled, Integer.MAX_VALUE)));
        } finally { Aspect.aspects.remove(doubled.getTag(), doubled); }
    }

    @Test void deepSharedDagIsIterativeAndRetainsFirstPrimalEncounterOrder() {
        var created = new ArrayList<Aspect>();
        try {
            Aspect previous = Aspect.FIRE;
            for (int i = 0; i < 1400; i++) {
                previous = new Aspect("focal_cost_deep_" + i, 0x123456, new Aspect[]{Aspect.AIR, previous});
                created.add(previous);
            }
            var result = FocalUpgradeCosts.reduceToPrimals(new AspectList().add(previous, 13).add(Aspect.FIRE, 3));
            assertEquals(18200, result.getAmount(Aspect.AIR));
            assertEquals(16, result.getAmount(Aspect.FIRE));
            assertEquals(List.of(Aspect.AIR, Aspect.FIRE), new ArrayList<>(result.aspects.keySet()));
        } finally { for (Aspect aspect : created) Aspect.aspects.remove(aspect.getTag(), aspect); }
    }

    @Test void invalidGraphAndAmountsHaveNoUsablePartialResult() {
        assertThrows(IllegalArgumentException.class, () -> FocalUpgradeCosts.reduceToPrimals(null));
        for (int amount : List.of(0, -1)) assertThrows(IllegalArgumentException.class,
                () -> FocalUpgradeCosts.reduceToPrimals(new AspectList().add(Aspect.FIRE, 1).add(Aspect.AIR, amount)));
        assertThrows(IllegalArgumentException.class, () -> FocalUpgradeCosts.reduceToPrimals(new AspectList().add(Aspect.AIR, Integer.MAX_VALUE).add(Aspect.FIRE, 1)));
        var cyclic = new Aspect("focal_cost_cycle", 0x123456, new Aspect[]{Aspect.AIR, Aspect.FIRE}) {
            @Override public Aspect[] getComponents() { return new Aspect[]{this, Aspect.AIR}; }
        };
        var malformed = new Aspect("focal_cost_bad_parent", 0x123456, new Aspect[]{Aspect.AIR, Aspect.FIRE}) {
            @Override public Aspect[] getComponents() { return new Aspect[]{Aspect.AIR, null}; }
        };
        try {
            assertThrows(IllegalArgumentException.class, () -> FocalUpgradeCosts.reduceToPrimals(new AspectList().add(Aspect.FIRE, 1).add(cyclic, 1)));
            assertThrows(IllegalArgumentException.class, () -> FocalUpgradeCosts.reduceToPrimals(new AspectList().add(malformed, 1)));
            Aspect.aspects.remove(cyclic.getTag(), cyclic);
            assertThrows(IllegalArgumentException.class, () -> FocalUpgradeCosts.plan(new AspectList().add(cyclic, 1), 1));
        } finally {
            Aspect.aspects.remove(cyclic.getTag(), cyclic); Aspect.aspects.remove(malformed.getTag(), malformed);
        }
    }

    @Test void drainOrderingPreservesZeroBarriersAndInsertionTies() {
        var remaining = new AspectList().add(Aspect.AIR, 0).add(Aspect.EARTH, 2).add(Aspect.FIRE, 5)
                .add(Aspect.WATER, 0).add(Aspect.ORDER, 1).add(Aspect.ENTROPY, 1);
        assertArrayEquals(new Aspect[]{Aspect.AIR, Aspect.FIRE, Aspect.EARTH, Aspect.WATER, Aspect.ORDER, Aspect.ENTROPY}, FocalUpgradeCosts.drainOrder(remaining));
        assertArrayEquals(new Aspect[]{Aspect.AIR, Aspect.EARTH, Aspect.FIRE, Aspect.WATER, Aspect.ORDER, Aspect.ENTROPY}, remaining.getAspects());
        assertThrows(IllegalArgumentException.class, () -> FocalUpgradeCosts.drainOrder(new AspectList().add(Aspect.AIR, -1)));
        assertArrayEquals(new Aspect[0], FocalUpgradeCosts.drainOrder(new AspectList()));
    }

    private static void vector(Aspect declared, int rank, int total, int... expected) {
        var plan = FocalUpgradeCosts.plan(new AspectList().add(declared, 1), rank);
        List<Aspect> primals = List.of(Aspect.AIR, Aspect.EARTH, Aspect.FIRE, Aspect.WATER, Aspect.ORDER, Aspect.ENTROPY);
        for (int i = 0; i < expected.length; i++) assertEquals(expected[i], plan.aspects().getAmount(primals.get(i)), declared.getTag() + "/" + primals.get(i).getTag());
        assertEquals(total, plan.initialTotal());
    }
}
