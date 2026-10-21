package thaumcraft.cult;

import org.junit.jupiter.api.Test;
import thaumcraft.common.lib.cult.CultistRules;
import static org.junit.jupiter.api.Assertions.*;

/**
 * The original crimson portal of the cult.
 *
 * <p>The numbers come from the oracle of the original TC4 4.2.3.5,
 * {@code EntityCultistPortal:35-274}: the portal is a stationary gate that arms itself with
 * four banners, seeds loot crates around itself and sends wave after wave of cultists after
 * the player until the praetor answers at the stage twelve.
 */
class CultistPortalRulesTest {

    @Test void thePortalHasFiveHundredHealthAndThirtyExperience() {
        assertEquals(500.0D, CultistRules.PORTAL_HEALTH);
        assertEquals(30, CultistRules.PORTAL_EXPERIENCE);
        assertEquals(1.5F, CultistRules.PORTAL_WIDTH);
        assertEquals(3.0F, CultistRules.PORTAL_HEIGHT);
        assertEquals(1.0D, CultistRules.PORTAL_KNOCKBACK_RESISTANCE);
    }

    @Test void thePortalHurtsAPlayerThatStandsCloseEnough() {
        // Oracle EntityCultistPortal:239 - the original asks for the SQUARED distance
        // (func_70068_e) and compares it with three.
        assertEquals(3.0D, CultistRules.PORTAL_COLLISION_DISTANCE_SQ);
    }

    @Test void thePortalBurnsItselfOnlyPastTheStageOfThePraetor() {
        // Oracle EntityCultistPortal:204-206 - the portal burns itself for five to nine points
        // of the void, but only after the stage of the praetor.
        assertEquals(0, CultistRules.portalSelfDamage(12, 4));
        assertEquals(0, CultistRules.portalSelfDamage(11, 4));
        assertEquals(5, CultistRules.portalSelfDamage(13, 0));
        assertEquals(9, CultistRules.portalSelfDamage(13, 4));
    }

    @Test void thePortalHealsOnePointPerTickUntilTheStageOfThePraetor() {
        // Oracle EntityCultistPortal:181-183.
        assertEquals(1.0F, CultistRules.portalHeal(0));
        assertEquals(1.0F, CultistRules.portalHeal(11));
        assertEquals(0.0F, CultistRules.portalHeal(12));
    }

    @Test void thePraetorAnswersAtTheStageTwelve() {
        assertEquals(12, CultistRules.PORTAL_BOSS_STAGE);
        assertTrue(CultistRules.portalSpawnsBoss(12));
        assertFalse(CultistRules.portalSpawnsBoss(11));
        assertTrue(CultistRules.portalSpawnsMinions(11));
        assertFalse(CultistRules.portalSpawnsMinions(12));
    }

    @Test void theWaveOfTheCultistsIsTwentyTicksPerLivingCultist() {
        // Oracle EntityCultistPortal:190-193.
        assertEquals(0, CultistRules.portalTiming(0));
        assertEquals(20, CultistRules.portalTiming(1));
        assertEquals(60, CultistRules.portalTiming(3));
    }
}
