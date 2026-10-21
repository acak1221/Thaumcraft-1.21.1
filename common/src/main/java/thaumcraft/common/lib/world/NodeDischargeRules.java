package thaumcraft.common.lib.world;

/**
 * The discharge of an ordinary node: a stronger nearby node takes a part of a weaker one.
 *
 * <p>Original {@code thaumcraft/common/tiles/TileNode.java}:
 * {@code handleDischarge():559-560} runs only when the node stands in the invisible block
 * {@code blockAiry} and the node is not locked by a node stabilizer;
 * {@code :564-566} refuses a FADING node; {@code :577-579} accepts only an unlocked invisible
 * aura node as the target. The silverwood knot and the totem host of the original therefore never
 * discharge and are never drained.</p>
 *
 * <p>The class holds no registry and no world state, so the rules can be asserted without a
 * bootstrap.</p>
 */
public final class NodeDischargeRules {
    private NodeDischargeRules() {}

    /**
     * Original {@code TileNode:559-560,564-566}: the conditions of the discharging node.
     *
     * @param auraNodeHost the node stands in the invisible aura node block
     * @param lock         the stabilizer lock of the node
     * @param fading       the node carries the FADING modifier
     */
    public static boolean mayDischarge(boolean auraNodeHost, int lock, boolean fading) {
        return auraNodeHost && lock != NodeStabilizerRules.BASIC_LOCK && !fading;
    }

    /**
     * Original {@code TileNode:577-579}: the target of a discharge.
     *
     * <p>The target is an invisible aura node without a stabilizer lock.</p>
     */
    public static boolean mayDischargeInto(boolean auraNodeHost, int otherLock) {
        return auraNodeHost && otherLock == NodeStabilizerRules.NO_LOCK;
    }
}
