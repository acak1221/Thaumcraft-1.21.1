package thaumcraft.items;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import thaumcraft.common.items.equipment.ElementalSwordRules;

/** The numbers of the Sword of the Zephyr (original ItemElementalSword) without a world. */
class ElementalSwordRulesTest {
    @Test void aHeldSwordTakesBackWhatGravityTakes() {
        assertEquals(0.08F, ElementalSwordRules.rise(0), 1e-6, "at rest it rises by one tick of gravity");
        assertEquals(-0.6D / 1.2 + 0.08F, ElementalSwordRules.rise(-0.6), 1e-6, "a fall is slowed by 1.2 and lifted");
        assertEquals(0.2F, ElementalSwordRules.rise(0.45), 1e-6, "above 0.5 it is reset to 0.2");
        assertEquals(0.5F + 0.0F, ElementalSwordRules.rise(0.42), 1e-6, "0.42 + 0.08 is exactly 0.5, not above it");
        assertEquals(0.2F, ElementalSwordRules.rise(0.421), 1e-6);
    }

    @Test void onlyAFallingUserHasTheFallShortened() {
        assertEquals(5.0F, ElementalSwordRules.fall(6.0F, -0.1), 1e-6F);
        assertEquals(6.0F, ElementalSwordRules.fall(6.0F, 0.0), 1e-6F, "standing still: unchanged");
        assertEquals(6.0F, ElementalSwordRules.fall(6.0F, 0.3), 1e-6F, "rising: unchanged");
    }

    @Test void anEntityIsBlownAwayByAFifthOfTheDistance() {
        Vec3 push = ElementalSwordRules.push(new Vec3(0, 0, 0), new Vec3(1.5, 0, 0));
        assertEquals(1.5 / 2.5 / 1.6, push.x, 1e-9);
        assertEquals(0, push.y, 1e-9);
        assertEquals(0, push.z, 1e-9);
        Vec3 below = ElementalSwordRules.push(new Vec3(0, 10, 0), new Vec3(0, 8, 0));
        assertTrue(below.y < 0 && below.x == 0 && below.z == 0, "an entity below is pushed down");
        assertEquals(-2.0 / 2.5 / 2.1, below.y, 1e-9);
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
