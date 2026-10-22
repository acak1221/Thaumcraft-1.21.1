package thaumcraft.api;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import thaumcraft.api.nodes.NodeModifier;
import thaumcraft.api.nodes.NodeType;

/**
 * The published node contract of the original.
 *
 * <p>Oracle {@code thaumcraft/api/nodes/INode.java:12-29} and
 * {@code thaumcraft/api/nodes/NodeType.java} / {@code NodeModifier.java} (CFR 0.152
 * decompilation of {@code Thaumcraft-1.7.10-4.2.3.5.jar}). The original node itself implements
 * the contract (oracle {@code thaumcraft/common/tiles/TileNode.java:75-77}) and writes its type
 * and its modifier through it (oracle {@code TileNode:245-252}).</p>
 *
 * <p>Forced 1.21.1 change: none. The published enums are the canonical ones; the native node
 * carries them, so there is no second set of node type enums any more. The two enums carry the
 * same constants, in the same order.</p>
 */
class NodeTypeContractTest {
    @Test void thePublishedEnumsCarryTheOriginalConstants() {
        assertEquals(6, NodeType.values().length);
        assertEquals(NodeType.NORMAL, NodeType.values()[0]);
        assertEquals(NodeType.UNSTABLE, NodeType.values()[1]);
        assertEquals(NodeType.DARK, NodeType.values()[2]);
        assertEquals(NodeType.TAINTED, NodeType.values()[3]);
        assertEquals(NodeType.HUNGRY, NodeType.values()[4]);
        assertEquals(NodeType.PURE, NodeType.values()[5]);
        assertEquals(3, NodeModifier.values().length);
        assertEquals(NodeModifier.BRIGHT, NodeModifier.values()[0]);
        assertEquals(NodeModifier.PALE, NodeModifier.values()[1]);
        assertEquals(NodeModifier.FADING, NodeModifier.values()[2]);
    }
}
