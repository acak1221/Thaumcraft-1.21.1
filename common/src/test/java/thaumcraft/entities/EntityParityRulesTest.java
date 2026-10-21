package thaumcraft.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import thaumcraft.common.lib.cult.CultistRules;
import thaumcraft.common.lib.entities.OriginalLightRules;

/**
 * The entity parity rules of the original cultists, the original mobs and their light gates.
 *
 * <p>Every value comes from the 4.2.3.5 oracle; the line references are the oracle files.</p>
 */
class EntityParityRulesTest {

    // ------------------------------------------------------------------
    // The call for help of a hurt cultist
    // ------------------------------------------------------------------

    @Test void theCallForHelpReachesTheFollowRangeOfTheCaller() {
        // Original AICultistHurtByTarget:35-40 - the radius is the original
        // EntityAITarget.getTargetDistance(), which is the follow range attribute.
        assertEquals(32.0D, CultistRules.callForHelpRange(32.0D));
        assertEquals(40.0D, CultistRules.callForHelpRange(40.0D));
    }

    @Test void aMobWithoutAFollowRangeCallsToSixteenBlocks() {
        // Original EntityAITarget.getTargetDistance returns sixteen without the attribute.
        assertEquals(16.0D, CultistRules.callForHelpRange(null));
        assertEquals(16.0D, CultistRules.DEFAULT_TARGET_DISTANCE);
    }

    @Test void theCultistFollowsToThirtyTwoBlocks() {
        assertEquals(32.0D, CultistRules.CULTIST_FOLLOW_RANGE);
        assertEquals(40.0D, CultistRules.BOSS_FOLLOW_RANGE);
    }

    // ------------------------------------------------------------------
    // The light level gate of the original mobs
    // ------------------------------------------------------------------

    @Test void theMobGateReadsTheBlockLightAndNotTheMaximumRawBrightness() {
        // Original EntityMob.isValidLightLevel - the block light of the cell against the
        // nextInt(8) roll. A cell that is dark under the open sky still spawns the mob.
        assertTrue(OriginalLightRules.validMobLightLevel(15, 0, 31, 7));
        assertFalse(OriginalLightRules.validMobLightLevel(15, 9, 31, 7));
        assertFalse(OriginalLightRules.validMobLightLevel(15, 0, 10, 7));
    }

    @Test void theMobGateFailsOnTheSkyRollBeforeItDrawsTheBlockRoll() {
        // The saved sky light of fifteen is above a nextInt(32) roll only half of the time.
        assertTrue(OriginalLightRules.validMobLightLevel(15, 0, 15, 0));
        assertFalse(OriginalLightRules.validMobLightLevel(15, 0, 14, 7));
    }

    @Test void theWispGateReadsTheDarkenedSkyLight() {
        // Original EntityWisp.isValidLightLevel - the wisp reads getLightValue(Sky), the
        // darkened sky light, and then the block light.
        assertTrue(OriginalLightRules.validWispLightLevel(4, 0, 31, 7));
        assertFalse(OriginalLightRules.validWispLightLevel(4, 9, 31, 7));
        assertFalse(OriginalLightRules.validWispLightLevel(4, 0, 3, 7));
    }

    @Test void theLightRollBoundsAreTheOriginalOnes() {
        assertEquals(32, OriginalLightRules.SKY_LIGHT_ROLL_BOUND);
        assertEquals(8, OriginalLightRules.BLOCK_LIGHT_ROLL_BOUND);
    }
}
