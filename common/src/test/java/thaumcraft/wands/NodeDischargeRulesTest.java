package thaumcraft.wands;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import thaumcraft.common.lib.world.NodeDischargeRules;
import thaumcraft.common.lib.world.NodeStabilizerRules;

/**
 * The discharge of an ordinary node.
 *
 * <p>Original {@code thaumcraft/common/tiles/TileNode.java}: {@code handleDischarge():559-560} runs
 * only in the invisible block {@code blockAiry}, {@code :564-566} refuses a FADING node and
 * {@code :577-579} accepts only an unlocked invisible aura node as the target. The silverwood knot
 * and the totem host of the original never discharge and are never drained.</p>
 */
class NodeDischargeRulesTest {
    @Test void onlyTheInvisibleAuraNodeDischarges() {
        assertTrue(NodeDischargeRules.mayDischarge(true, NodeStabilizerRules.NO_LOCK, false));
        // The silverwood knot and the totem host are node hosts, but not the invisible aura node.
        assertFalse(NodeDischargeRules.mayDischarge(false, NodeStabilizerRules.NO_LOCK, false));
    }

    @Test void aStabilizedOrFadingNodeNeverDischarges() {
        // Original TileNode:559-560 - the lock one refuses the discharge.
        assertFalse(NodeDischargeRules.mayDischarge(true, NodeStabilizerRules.BASIC_LOCK, false));
        // Original TileNode:564-566 - the FADING modifier refuses the discharge.
        assertFalse(NodeDischargeRules.mayDischarge(true, NodeStabilizerRules.NO_LOCK, true));
        // The advanced lock (two) is NOT the lock one, so it still discharges.
        assertTrue(NodeDischargeRules.mayDischarge(true, NodeStabilizerRules.ADVANCED_LOCK, false));
    }

    @Test void onlyAnUnlockedInvisibleAuraNodeIsDrained() {
        assertTrue(NodeDischargeRules.mayDischargeInto(true, NodeStabilizerRules.NO_LOCK));
        assertFalse(NodeDischargeRules.mayDischargeInto(false, NodeStabilizerRules.NO_LOCK));
        assertFalse(NodeDischargeRules.mayDischargeInto(true, NodeStabilizerRules.BASIC_LOCK));
        assertFalse(NodeDischargeRules.mayDischargeInto(true, NodeStabilizerRules.ADVANCED_LOCK));
    }
}
