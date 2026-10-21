package thaumcraft.wands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import thaumcraft.common.lib.world.NodeStabilizerRules;

/**
 * The stabilizer lock of an ordinary node and everything the lock changes.
 *
 * <p>Original {@code thaumcraft/common/tiles/TileNode.java}:
 * {@code checkLock():879-897} gives the lock one under the node stabilizer (the original
 * {@code blockStoneDevice} metadata nine) and the lock two under the advanced node stabilizer (the
 * original {@code blockStoneDevice} metadata ten); {@code handleRecharge():644-650} multiplies the
 * regeneration interval by two for the lock one and by twenty for the lock two;
 * {@code handleNodeStability():775,781} divides the two repair rolls by the lock.</p>
 */
class NodeStabilizerRulesTest {
    @Test void theNodeStabilizerLocksTheNodeOnceAndTheAdvancedOneTwice() {
        assertEquals(NodeStabilizerRules.NO_LOCK, NodeStabilizerRules.lock(false, false));
        assertEquals(NodeStabilizerRules.BASIC_LOCK, NodeStabilizerRules.lock(true, false));
        assertEquals(NodeStabilizerRules.ADVANCED_LOCK, NodeStabilizerRules.lock(false, true));
        // The advanced stabilizer is its own block in the native tree, so it wins over the basic one.
        assertEquals(NodeStabilizerRules.ADVANCED_LOCK, NodeStabilizerRules.lock(true, true));
        assertEquals(1, NodeStabilizerRules.BASIC_LOCK);
        assertEquals(2, NodeStabilizerRules.ADVANCED_LOCK);
    }

    @Test void theLockMultipliesTheRegenerationIntervalByTwoAndByTwenty() {
        // Original TileNode:628-651 - the interval of the modifier, then the lock multiplier.
        assertEquals(600, NodeStabilizerRules.BASE_INTERVAL);
        assertEquals(400, NodeStabilizerRules.BRIGHT_INTERVAL);
        assertEquals(900, NodeStabilizerRules.PALE_INTERVAL);
        assertEquals(0, NodeStabilizerRules.FADING_INTERVAL);
        for (int interval : new int[]{0, 400, 600, 900}) {
            assertEquals(interval, NodeStabilizerRules.regenerationInterval(interval, NodeStabilizerRules.NO_LOCK));
            assertEquals(interval * 2, NodeStabilizerRules.regenerationInterval(interval, NodeStabilizerRules.BASIC_LOCK));
            assertEquals(interval * 20, NodeStabilizerRules.regenerationInterval(interval, NodeStabilizerRules.ADVANCED_LOCK));
        }
        // The advanced stabilizer is twenty times slower than the ordinary one, not two.
        assertEquals(12000, NodeStabilizerRules.regenerationInterval(NodeStabilizerRules.BASE_INTERVAL, NodeStabilizerRules.ADVANCED_LOCK));
        assertEquals(1200, NodeStabilizerRules.regenerationInterval(NodeStabilizerRules.BASE_INTERVAL, NodeStabilizerRules.BASIC_LOCK));
    }

    @Test void theRepairRollsDivideByTheLock() {
        // Original TileNode:775 - nextInt(10000 / lock) == 42.
        assertEquals(10000, NodeStabilizerRules.stabilityBound(NodeStabilizerRules.BASIC_LOCK));
        assertEquals(5000, NodeStabilizerRules.stabilityBound(NodeStabilizerRules.ADVANCED_LOCK));
        assertEquals(42, NodeStabilizerRules.STABILITY_ROLL);
        // Original TileNode:781 - nextInt(12500 / lock) == 69.
        assertEquals(12500, NodeStabilizerRules.fadingRepairBound(NodeStabilizerRules.BASIC_LOCK));
        assertEquals(6250, NodeStabilizerRules.fadingRepairBound(NodeStabilizerRules.ADVANCED_LOCK));
        assertEquals(69, NodeStabilizerRules.FADING_REPAIR_ROLL);
        // An unlocked node never runs the repair rolls of the original.
        assertEquals(0, NodeStabilizerRules.stabilityBound(NodeStabilizerRules.NO_LOCK));
        assertEquals(0, NodeStabilizerRules.fadingRepairBound(NodeStabilizerRules.NO_LOCK));
    }

    @Test void theAdvancedStabilizerIsHalfAsLikelyToRepairAnUnstableNode() {
        // The original divides the bound by the lock, so the advanced stabilizer repairs twice as often.
        assertTrue(NodeStabilizerRules.stabilityBound(NodeStabilizerRules.ADVANCED_LOCK)
                < NodeStabilizerRules.stabilityBound(NodeStabilizerRules.BASIC_LOCK));
        assertTrue(NodeStabilizerRules.fadingRepairBound(NodeStabilizerRules.ADVANCED_LOCK)
                < NodeStabilizerRules.fadingRepairBound(NodeStabilizerRules.BASIC_LOCK));
        assertFalse(NodeStabilizerRules.regenerationInterval(NodeStabilizerRules.BASE_INTERVAL, NodeStabilizerRules.ADVANCED_LOCK)
                == NodeStabilizerRules.regenerationInterval(NodeStabilizerRules.BASE_INTERVAL, NodeStabilizerRules.BASIC_LOCK));
    }
}
