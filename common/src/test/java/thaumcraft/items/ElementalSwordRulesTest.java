package thaumcraft.items;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import thaumcraft.common.items.equipment.ElementalSwordRules;

/** The numbers of the Sword of the Zephyr (original ItemElementalSword) without a world. */
class ElementalSwordRulesTest {
    @Test void aHeldSwordTakesBackWhatGravityTakes() {
        // Oracle ItemElementalSword:100-109: the motion is divided by (double)1.2f, raised by
        // (double)0.08f and reset to 0.2f above 0.5. The expectations are the original's own expressions.
        assertEquals((double) 0.08F, ElementalSwordRules.rise(0), "at rest it rises by one tick of gravity");
        assertEquals(-0.6D / (double) 1.2F + (double) 0.08F, ElementalSwordRules.rise(-0.6), "a fall is slowed by 1.2 and lifted");
        assertEquals((double) 0.2F, ElementalSwordRules.rise(0.45), "above 0.5 it is reset to 0.2");
        assertEquals(0.42 + (double) 0.08F, ElementalSwordRules.rise(0.42), "0.42 + 0.08 is just below 0.5, so nothing is reset");
        assertEquals((double) 0.2F, ElementalSwordRules.rise(0.421));
    }

    @Test void onlyAFallingUserHasTheFallShortened() {
        // Oracle ItemElementalSword:105: the fall distance is divided by the float 1.2f.
        assertEquals(6.0F / 1.2F, ElementalSwordRules.fall(6.0F, -0.1));
        assertEquals(6.0F, ElementalSwordRules.fall(6.0F, 0.0), "standing still: unchanged");
        assertEquals(6.0F, ElementalSwordRules.fall(6.0F, 0.3), "rising: unchanged");
    }

    @Test void anEntityIsBlownAwayByAFifthOfTheDistance() {
        // Oracle ItemElementalSword:117-124: the away vector is divided by 2.5 and by the distance plus 0.1.
        Vec3 push = ElementalSwordRules.push(new Vec3(0, 0, 0), new Vec3(1.5, 0, 0));
        assertEquals(1.5 / 2.5 / (1.5 + 0.1), push.x);
        assertEquals(0, push.y);
        assertEquals(0, push.z);
        Vec3 below = ElementalSwordRules.push(new Vec3(0, 10, 0), new Vec3(0, 8, 0));
        assertTrue(below.y < 0 && below.x == 0 && below.z == 0, "an entity below is pushed down");
        assertEquals(-2.0 / 2.5 / (2.0 + 0.1), below.y);
        // The farther, the nearer the push is to 0.4 of a unit but never reaches it.
        assertTrue(ElementalSwordRules.push(new Vec3(0, 0, 0), new Vec3(100, 0, 0)).x < 0.4);
        assertTrue(ElementalSwordRules.push(new Vec3(0, 0, 0), new Vec3(100, 0, 0)).x > 0.39);
    }

    @Test void theConstantsAreTheOriginalOnes() {
        assertEquals(72000, ElementalSwordRules.USE_DURATION);
        assertEquals(20, ElementalSwordRules.PERIOD);
        assertEquals(2.5, ElementalSwordRules.REACH);
        assertEquals(1.2, ElementalSwordRules.SWEEP_X);
        assertEquals(1.1, ElementalSwordRules.SWEEP_Y);
        assertEquals(1.2, ElementalSwordRules.SWEEP_Z);
        assertEquals(1.2F, ElementalSwordRules.DAMPING);
    }
}
