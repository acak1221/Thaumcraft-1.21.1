package thaumcraft.research;

import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.lib.research.AspectOrbRules;

class AspectOrbRulesTest {
    @Test void aRetainedZeroPrimalParticipatesInTheDrawWithoutSpendingStock() {
        var current = new AspectList().add(Aspect.AIR, 0).add(Aspect.FIRE, 2).add(Aspect.METAL, 10);
        var bounds = new ArrayList<Integer>();
        assertNull(AspectOrbRules.chooseCurrentPrimal(current, bound -> { bounds.add(bound); return 0; }));
        assertEquals(Aspect.FIRE, AspectOrbRules.chooseCurrentPrimal(current, bound -> { bounds.add(bound); return 1; }));
        assertEquals(List.of(2, 2), bounds);
        assertEquals(0, current.getAmount(Aspect.AIR)); assertEquals(2, current.getAmount(Aspect.FIRE)); assertEquals(10, current.getAmount(Aspect.METAL));
    }
    @Test void compoundOnlyAndEmptyNodesHaveNoInvalidNextIntZeroDraw() {
        assertNull(AspectOrbRules.chooseCurrentPrimal(new AspectList().add(Aspect.METAL, 10), bound -> { fail("Original empty-primal crash must not reach RNG"); return 0; }));
        assertNull(AspectOrbRules.chooseCurrentPrimal(new AspectList(), bound -> { fail("Empty node has no emission draw"); return 0; }));
    }
    @Test void originalEightBlockAttractionUsesSquaredFalloffAndEyeDelta() {
        Vec3 atFour = AspectOrbRules.attraction(new Vec3(4, 0, 0));
        assertEquals(.025, atFour.x, 1e-12); assertEquals(0, atFour.y); assertEquals(0, atFour.z);
        assertEquals(.025, AspectOrbRules.attraction(new Vec3(0, 4, 0)).y, 1e-12);
        assertEquals(Vec3.ZERO, AspectOrbRules.attraction(new Vec3(8, 0, 0)));
        assertEquals(Vec3.ZERO, AspectOrbRules.attraction(new Vec3(8.01, 0, 0)));
        assertEquals(Vec3.ZERO, AspectOrbRules.attraction(Vec3.ZERO));
        assertEquals(Vec3.ZERO, AspectOrbRules.attraction(new Vec3(Double.NaN, 0, 0)));
    }
    @Test void oneOrbUnitMeansOneHundredCentivisAndPreservesOriginalPositiveValueRange() {
        assertEquals(100, AspectOrbRules.centivis(1)); assertEquals(2000, AspectOrbRules.centivis(20));
        assertEquals(3276700, AspectOrbRules.centivis(Short.MAX_VALUE));
        assertThrows(IllegalArgumentException.class, () -> AspectOrbRules.centivis(0));
        assertThrows(IllegalArgumentException.class, () -> AspectOrbRules.centivis(-1));
        assertThrows(IllegalArgumentException.class, () -> AspectOrbRules.centivis(32768));
    }
    @Test void lockStrengthScalesTheOriginalStabilizationChanceInsteadOfChangingItsWinningDraw() {
        assertEquals(10000, AspectOrbRules.stabilityBound(1)); assertEquals(5000, AspectOrbRules.stabilityBound(2));
        assertEquals(42, AspectOrbRules.STABILITY_ROLL); assertEquals(100, AspectOrbRules.EMISSION_INTERVAL); assertEquals(150, AspectOrbRules.MAX_AGE);
        assertThrows(IllegalArgumentException.class, () -> AspectOrbRules.stabilityBound(0));
    }
}
