package thaumcraft.common.lib.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;

/**
 * The original death aspect orbs.
 *
 * <p>Oracle {@code thaumcraft/common/lib/events/EventHandlerEntity.java:508-516} (CFR 0.152
 * decompilation of {@code Thaumcraft-1.7.10-4.2.3.5.jar}): the death of a mob that was recently
 * hit drops one aspect orb per <em>primal</em> aspect of its scanned aspects, with a fifty percent
 * roll per aspect and a value of {@code 1 + nextInt(amount of that aspect)}, both drawn from the
 * WORLD random and before the loot of the same death.</p>
 */
class DeathAspectOrbRulesTest {
    /** A deterministic stand-in for the world random of the original. */
    private static final class Draws {
        private final List<Object> values = new ArrayList<>();
        private int at;
        Draws(Object... values) { this.values.addAll(List.of(values)); }
        boolean nextBoolean() { return (Boolean) values.get(at++); }
        int nextInt(int bound) {
            Object value = values.get(at++);
            if (!(value instanceof Roll roll) || roll.bound() != bound)
                throw new AssertionError("Unexpected nextInt(" + bound + ") at " + (at - 1) + ": " + value);
            return roll.result();
        }
    }

    private record Roll(int bound, int result) {}

    @Test void everyPrimalAspectOfTheDeadMobRollsForItsOwnOrb() {
        // The original brainy zombie: UNDEAD 3, MAN 1, MIND 1, EARTH 1 (oracle ConfigAspects:88).
        AspectList aspects = new AspectList()
                .add(Aspect.UNDEAD, 3).add(Aspect.MAN, 1).add(Aspect.MIND, 1).add(Aspect.EARTH, 1);
        // The reduced primals of the original, in the original order.
        AspectList primals = aspects.reduceToPrimals(false);
        assertEquals(6, primals.size(), "the original brainy zombie has six primal aspects");
        assertEquals(Aspect.AIR, primals.getAspects()[0]);
        assertEquals(Aspect.ORDER, primals.getAspects()[1]);
        assertEquals(Aspect.WATER, primals.getAspects()[2]);
        assertEquals(Aspect.EARTH, primals.getAspects()[3]);
        assertEquals(Aspect.ENTROPY, primals.getAspects()[4]);
        assertEquals(Aspect.FIRE, primals.getAspects()[5]);
        assertEquals(4, primals.getAmount(Aspect.AIR));
        assertEquals(9, primals.getAmount(Aspect.EARTH));
        assertEquals(2, primals.getAmount(Aspect.FIRE));

        // Every roll succeeds, so every aspect draws its value right after its own boolean.
        Draws draws = new Draws(true, new Roll(4, 3), true, new Roll(4, 0), true, new Roll(8, 7),
                true, new Roll(9, 8), true, new Roll(5, 1), true, new Roll(2, 0));
        var drops = DeathAspectOrbRules.drops(aspects, draws::nextBoolean, draws::nextInt);
        assertEquals(6, drops.size(), "the original drops one orb per primal aspect");
        assertEquals(Aspect.AIR, drops.get(0).aspect());
        assertEquals(4, drops.get(0).value(), "the value of an orb is one plus the drawn amount");
        assertEquals(Aspect.FIRE, drops.get(5).aspect());
        assertEquals(1, drops.get(5).value());
    }

    @Test void aRefusedRollDropsNoOrbAndDrawsNoValue() {
        AspectList aspects = new AspectList().add(Aspect.TAINT, 2).add(Aspect.AIR, 2);
        // The reduced primals are air 4, entropy 4, order 2 and fire 2.
        assertEquals(4, aspects.reduceToPrimals(false).size());
        // The first two primals refuse, the third one succeeds with the value zero.
        Draws draws = new Draws(false, false, true, new Roll(2, 0));
        var drops = DeathAspectOrbRules.drops(aspects, draws::nextBoolean, draws::nextInt);
        assertEquals(1, drops.size(), "a refused roll drops no orb");
        assertEquals(Aspect.ORDER, drops.get(0).aspect());
        assertEquals(1, drops.get(0).value());
    }

    @Test void aMobWithoutAspectsDropsNoOrb() {
        Draws draws = new Draws();
        assertTrue(DeathAspectOrbRules.drops(new AspectList(), draws::nextBoolean, draws::nextInt).isEmpty(),
                "a mob without aspects drops no orb");
        assertTrue(DeathAspectOrbRules.drops(null, draws::nextBoolean, draws::nextInt).isEmpty(),
                "a mob without scanned aspects drops no orb");
    }

    @Test void theValueOfAnOrbIsBoundedByTheAmountOfItsAspect() {
        AspectList aspects = new AspectList().add(Aspect.FIRE, 3);
        // The single primal of the original is fire with the amount three.
        AspectList primals = aspects.reduceToPrimals(false);
        assertEquals(1, primals.size());
        assertEquals(Aspect.FIRE, primals.getAspects()[0]);
        assertEquals(3, primals.getAmount(Aspect.FIRE));
        Draws draws = new Draws(true, new Roll(3, 2));
        var drops = DeathAspectOrbRules.drops(aspects, draws::nextBoolean, draws::nextInt);
        assertEquals(1, drops.size());
        assertEquals(3, drops.get(0).value(), "the value of an orb is one plus the drawn amount");
    }
}
