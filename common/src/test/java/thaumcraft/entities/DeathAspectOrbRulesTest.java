package thaumcraft.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.lib.entities.DeathAspectOrbRules;

/**
 * The original TC4 death aspect orbs.
 *
 * <p>Oracle: {@code thaumcraft/common/lib/events/EventHandlerEntity.java:534-546} and
 * {@code thaumcraft/common/lib/research/ResearchManager.java:525-558}. Every value and every
 * draw order below is the original one.</p>
 */
class DeathAspectOrbRulesTest {

    /** A deterministic stand in for the world random of the original. */
    private static final class Draws {
        private final List<Boolean> booleans;
        private final List<Integer> bounds = new ArrayList<>();
        private final List<Integer> values = new ArrayList<>();
        private int booleanIndex;

        Draws(boolean... booleans) { this.booleans = List.of(booleans); }

        boolean nextBoolean() { return booleans.get(booleanIndex++); }

        int nextInt(int bound) {
            bounds.add(bound);
            // The original nextInt(bound) is a uniform draw in [0,bound); the test uses the
            // lowest legal value so the value of the orb is deterministic.
            values.add(0);
            return 0;
        }

        List<Integer> bounds() { return bounds; }
    }

    @Test void aMobWithoutAspectsDropsNothing() {
        // Original EventHandlerEntity:536 - the aspect list of the dead mob has to exist.
        assertTrue(DeathAspectOrbRules.drops(null, () -> true, bound -> 0).isEmpty());
        assertTrue(DeathAspectOrbRules.drops(new AspectList(), () -> true, bound -> 0).isEmpty());
    }

    @Test void everyPrimalAspectHasItsOwnFiftyPercentRoll() {
        // Original EventHandlerEntity:541 - one nextBoolean per primal aspect.
        var draws = new Draws(true, false, true);
        var aspects = new AspectList().add(Aspect.EARTH, 1).add(Aspect.FIRE, 1).add(Aspect.AIR, 1);
        var drops = DeathAspectOrbRules.drops(aspects, draws::nextBoolean, draws::nextInt);
        assertEquals(2, drops.size());
        assertEquals(Aspect.EARTH, drops.get(0).aspect());
        assertEquals(Aspect.AIR, drops.get(1).aspect());
        // The rolled aspect never draws its value roll.
        assertEquals(List.of(1, 1), draws.bounds());
    }

    @Test void theOrbCarriesOneToTheAmountOfItsAspect() {
        // Original EventHandlerEntity:543 - the value is 1 + rand.nextInt(amount of the aspect).
        var aspects = new AspectList().add(Aspect.EARTH, 4);
        var drops = DeathAspectOrbRules.drops(aspects, () -> true, bound -> bound - 1);
        assertEquals(1, drops.size());
        assertEquals(4, drops.get(0).value());
        var lower = DeathAspectOrbRules.drops(aspects, () -> true, bound -> 0);
        assertEquals(1, lower.get(0).value());
    }

    @Test void aCompoundAspectIsReducedToItsPrimalParents() {
        // Original ResearchManager.reduceToPrimals:529-558 - a compound aspect gives its amount
        // to each of its two primal components.
        var aspects = new AspectList().add(Aspect.MOTION, 3);
        var draws = new Draws(true, true);
        var drops = DeathAspectOrbRules.drops(aspects, draws::nextBoolean, draws::nextInt);
        assertEquals(2, drops.size());
        assertEquals(Aspect.AIR, drops.get(0).aspect());
        assertEquals(Aspect.ORDER, drops.get(1).aspect());
        for (var drop : drops) assertTrue(drop.aspect().isPrimal());
        // The amount of the REDUCED list drives the value roll.
        assertEquals(List.of(3, 3), draws.bounds());
    }

    @Test void theAmountOfTheReducedListDrivesTheValueRoll() {
        // Original EventHandlerEntity:543 - the amount is read from the REDUCED list, so a
        // compound aspect of three gives a value roll of three, not of one.
        var aspects = new AspectList().add(Aspect.MOTION, 3);
        var draws = new Draws(true, true);
        DeathAspectOrbRules.drops(aspects, draws::nextBoolean, draws::nextInt);
        assertEquals(List.of(3, 3), draws.bounds());
    }
}
