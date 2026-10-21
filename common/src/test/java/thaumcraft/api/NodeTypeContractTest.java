package thaumcraft.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import thaumcraft.api.nodes.NodeModifier;
import thaumcraft.api.nodes.NodeType;
import thaumcraft.common.tiles.TileNode;

/**
 * The published node contract of the original.
 *
 * <p>Oracle {@code thaumcraft/api/nodes/INode.java:12-29} and
 * {@code thaumcraft/api/nodes/NodeType.java} / {@code NodeModifier.java} (CFR 0.152
 * decompilation of {@code Thaumcraft-1.7.10-4.2.3.5.jar}). The original node itself implements
 * the contract (oracle {@code thaumcraft/common/tiles/TileNode.java:75-77}) and writes its type
 * and its modifier through it (oracle {@code TileNode:245-252}).</p>
 *
 * <p>Forced 1.21.1 change: the native node keeps its own nested enums, so the published enums
 * are translated by name. The two enums carry the same constants, in the same order.</p>
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

    @Test void everyPublishedTypeAndModifierHasItsNativeCounterpart() {
        for (NodeType type : NodeType.values()) {
            assertEquals(type.name(), TileNode.nativeType(type).name(),
                    "the native type of a published type carries the same name");
        }
        for (NodeModifier modifier : NodeModifier.values()) {
            assertEquals(modifier.name(), TileNode.nativeModifier(modifier).name(),
                    "the native modifier of a published modifier carries the same name");
        }
        assertNull(TileNode.nativeType(null), "no published type means no native type");
        assertNull(TileNode.nativeModifier(null), "no published modifier means no native modifier");
    }
}
