package thaumcraft.api;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import java.util.UUID;
import net.minecraft.SharedConstants;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.lib.research.PlayerKnowledge;
import thaumcraft.common.lib.research.ResearchAspectCombination;

/** Real add-on registrations and the research table's combination planner. */
class TailApiAspectReviewTest {
    @BeforeAll static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test void zeroComponentPrimalDoesNotCrashCombination() {
        assertShortPrimalCannotCrashCombination(new Aspect[0]);
    }

    @Test void oneComponentPrimalDoesNotCrashCombination() {
        assertShortPrimalCannotCrashCombination(new Aspect[]{Aspect.AIR});
    }

    private static void assertShortPrimalCannotCrashCombination(Aspect[] components) {
        synchronized (Aspect.aspects) {
            Aspect primal = register(components);
            try {
                assertTrue(primal.isPrimal(), "the documented original rule admits non-pair arrays as primals");
                PlayerKnowledge actor = new PlayerKnowledge();
                actor.setAspectPool("aer", 2);
                var before = actor.save();
                var plan = assertDoesNotThrow(() -> ResearchAspectCombination.plan(actor, Map.of(), "aer", "aer"),
                        "an add-on primal must not crash a paid experiment that has no matching compound");
                assertNotNull(plan);
                assertNull(plan.output());
                assertEquals(0, plan.award());
                assertFalse(plan.discovery());
                assertEquals(0, plan.knowledge().getAspectPoolFor("aer"), "both experiment inputs remain payable");
                assertEquals(before, actor.save(), "planning must retain the live player's knowledge");
                assertSame(Aspect.LIGHT, Aspect.getCombinationResult(Aspect.AIR, Aspect.FIRE),
                        "the existing compound and its registration precedence remain unchanged");
            } finally {
                Aspect.aspects.remove(primal.getTag(), primal);
            }
        }
    }

    @Test void longComponentArraysKeepOriginalFirstPairCombination() {
        synchronized (Aspect.aspects) {
            Aspect primal = register(new Aspect[]{Aspect.AIR, Aspect.AIR, Aspect.FIRE});
            try {
                assertTrue(primal.isPrimal());
                assertSame(primal, Aspect.getCombinationResult(Aspect.AIR, Aspect.AIR),
                        "the inherited first-pair quirk of arrays longer than two is outside the short-array crash fix");
            } finally {
                Aspect.aspects.remove(primal.getTag(), primal);
            }
        }
    }

    private static Aspect register(Aspect[] components) {
        String tag = "tail_api_review_" + UUID.randomUUID().toString().replace("-", "");
        return new Aspect(tag, 0x123456, components,
                ResourceLocation.fromNamespaceAndPath("thaumcraft_review", "textures/aspects/primal.png"), 1);
    }
}
