package thaumcraft.items;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import thaumcraft.common.lib.utils.SpecialMining;

/** The roll of the original special mining ({@code Utils.findSpecialMiningResult}, Config:799-801) without a world. */
class SpecialMiningRuleTest {
    @Test void theChanceGrowsByAnEighthOfAQuarterPerFortuneLevel() {
        assertEquals(0.2F, SpecialMining.chance(0), 1e-6F);
        assertEquals(0.275F, SpecialMining.chance(1), 1e-6F);
        assertEquals(0.35F, SpecialMining.chance(2), 1e-6F);
        assertEquals(0.425F, SpecialMining.chance(3), 1e-6F);
    }

    @Test void theThresholdIsInclusiveAndScaledByTheEntrysOwnFactor() {
        assertTrue(SpecialMining.succeeds(0.2F, SpecialMining.chance(0), SpecialMining.IRON), "a roll on the iron threshold succeeds");
        assertFalse(SpecialMining.succeeds(0.2001F, SpecialMining.chance(0), SpecialMining.IRON), "a hair above does not");
        assertTrue(SpecialMining.succeeds(0.179F, SpecialMining.chance(0), SpecialMining.GOLD), "gold and cinnabar run on 0.9 of it: 0.18");
        assertFalse(SpecialMining.succeeds(0.181F, SpecialMining.chance(0), SpecialMining.GOLD));
        assertTrue(SpecialMining.succeeds(0.179F, SpecialMining.chance(0), SpecialMining.CINNABAR));
        assertFalse(SpecialMining.succeeds(0.181F, SpecialMining.chance(0), SpecialMining.CINNABAR));
        assertTrue(SpecialMining.succeeds(0.0F, 0.0F, SpecialMining.IRON), "a zero roll always succeeds, as r <= 0 does in the original");
        assertTrue(SpecialMining.succeeds(0.38F, SpecialMining.chance(3), SpecialMining.GOLD), "fortune 3 gold: 0.425 x 0.9");
        assertFalse(SpecialMining.succeeds(0.39F, SpecialMining.chance(3), SpecialMining.GOLD));
    }

    @Test void theFactorsAreTheOriginalOnes() {
        assertEquals(1.0F, SpecialMining.IRON);
        assertEquals(0.9F, SpecialMining.GOLD);
        assertEquals(0.9F, SpecialMining.CINNABAR);
        assertEquals(0.2F, SpecialMining.BASE_CHANCE);
        assertEquals(0.075F, SpecialMining.FORTUNE_STEP);
    }
}
