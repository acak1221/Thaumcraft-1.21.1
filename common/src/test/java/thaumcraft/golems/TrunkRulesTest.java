package thaumcraft.golems;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import thaumcraft.common.items.golems.GolemUpgrade;
import thaumcraft.common.items.golems.TrunkRules;

/** Pure rules of the Traveling Trunk (original EntityTravelingTrunk and neighbours). */
class TrunkRulesTest {
    @Test void upgradeIdsAreTheGolemUpgradeIds() {
        assertEquals(GolemUpgrade.AIR.id, TrunkRules.AIR);
        assertEquals(GolemUpgrade.EARTH.id, TrunkRules.EARTH);
        assertEquals(GolemUpgrade.FIRE.id, TrunkRules.FIRE);
        assertEquals(GolemUpgrade.WATER.id, TrunkRules.WATER);
        assertEquals(GolemUpgrade.ORDER.id, TrunkRules.ORDER);
        assertEquals(GolemUpgrade.ENTROPY.id, TrunkRules.ENTROPY);
        assertEquals(GolemUpgrade.EMPTY, TrunkRules.NO_UPGRADE);
        for (int upgrade = -1; upgrade <= 5; upgrade++) assertTrue(TrunkRules.validUpgrade(upgrade), "upgrade " + upgrade);
        assertFalse(TrunkRules.validUpgrade(-2));
        assertFalse(TrunkRules.validUpgrade(6));
    }

    @Test void onlyEarthGivesTheFourthRow() {
        for (int upgrade = -1; upgrade <= 5; upgrade++) {
            assertEquals(upgrade == 1 ? 4 : 3, TrunkRules.rows(upgrade), "rows with " + upgrade);
            assertEquals(TrunkRules.rows(upgrade) * 9, TrunkRules.slots(upgrade));
        }
        assertEquals(27, TrunkRules.slots(-1));
        assertEquals(36, TrunkRules.slots(1));
        assertEquals(36, TrunkRules.MAX_SLOTS);
    }

    @Test void anUpgradeIsFittedOnlyOnceAndOnlyAKnownOne() {
        for (int offered = 0; offered <= 5; offered++) assertTrue(TrunkRules.canFit(-1, offered), "offered " + offered);
        for (int current = 0; current <= 5; current++) assertFalse(TrunkRules.canFit(current, 0), "already " + current);
        assertFalse(TrunkRules.canFit(-1, -1));
        assertFalse(TrunkRules.canFit(-1, 6));
    }

    @Test void waterMakesItInvulnerableHealingEveryTickAndPrivate() {
        for (int upgrade = -1; upgrade <= 5; upgrade++) {
            assertEquals(upgrade == 3, TrunkRules.invulnerable(upgrade));
            assertEquals(upgrade == 3, TrunkRules.healsNow(upgrade, 7), "tick 7 with " + upgrade);
            assertTrue(TrunkRules.healsNow(upgrade, 50) && TrunkRules.healsNow(upgrade, 0) && TrunkRules.healsNow(upgrade, 100));
            assertTrue(TrunkRules.mayOpen(upgrade, true));
            assertEquals(upgrade != 3, TrunkRules.mayOpen(upgrade, false), "a stranger with " + upgrade);
        }
    }

    @Test void airHopsFasterEntropyPullsFireDefends() {
        assertEquals(0.04F, TrunkRules.airAcceleration(0));
        for (int upgrade : new int[] {-1, 1, 2, 3, 4, 5}) assertEquals(0.03F, TrunkRules.airAcceleration(upgrade));
        assertTrue(TrunkRules.fast(0) && !TrunkRules.fast(1));
        assertTrue(TrunkRules.pullsItems(5) && !TrunkRules.pullsItems(4));
        assertTrue(TrunkRules.defendsOwner(2) && !TrunkRules.defendsOwner(3));
    }

    @Test void thresholdsAndTimersMatchTheOriginal() {
        assertEquals(75F, TrunkRules.MAX_HEALTH);
        assertEquals(4F, TrunkRules.ATTACK_DAMAGE);
        assertEquals(20F, TrunkRules.TELEPORT_DISTANCE);
        assertEquals(5F, TrunkRules.FOLLOW_DISTANCE);
        assertEquals(600, TrunkRules.ANGER_TICKS);
        // (nextInt(10) + 5) / 3 for rolls 0..9: 1,2,2,2,3,3,3,4,4,4 — integer division, as in the original.
        int[] expected = {1, 2, 2, 2, 3, 3, 3, 4, 4, 4};
        for (int roll = 0; roll < 10; roll++) assertEquals(expected[roll], TrunkRules.hopDelay(roll), "roll " + roll);
        for (int roll = 0; roll < 5; roll++) assertEquals(10 + roll, TrunkRules.attackCooldown(roll));
    }
}
