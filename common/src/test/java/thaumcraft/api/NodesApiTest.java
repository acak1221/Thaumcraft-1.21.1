package thaumcraft.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.nodes.INode;
import thaumcraft.api.nodes.NodeBase;
import thaumcraft.api.nodes.NodeModifier;
import thaumcraft.api.nodes.NodeType;

/** The node contract of the public API. */
class NodesApiTest {
    @BeforeAll static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test void aNodeKeepsTheOriginalContainerRules() {
        INode node = new NodeBase();
        node.setAspects(new AspectList().add(Aspect.AIR, 10));
        assertEquals(10, node.getAspectsBase().getAmount(Aspect.AIR));
        assertEquals(10, node.getAspects().getAmount(Aspect.AIR));
        assertEquals(10, node.getNodeVisBase(Aspect.AIR));
        assertEquals(10, node.containerContains(Aspect.AIR));
    }

    @Test void aNodeNeverHoldsMoreThanItsBase() {
        INode node = new NodeBase();
        node.setAspects(new AspectList().add(Aspect.AIR, 10));
        assertEquals(5, node.addToContainer(Aspect.AIR, 5));
        assertEquals(10, node.getAspects().getAmount(Aspect.AIR));
        assertTrue(node.takeFromContainer(Aspect.AIR, 4));
        assertEquals(6, node.getAspects().getAmount(Aspect.AIR));
        assertFalse(node.takeFromContainer(Aspect.AIR, 100));
        assertEquals(6, node.getAspects().getAmount(Aspect.AIR));
    }

    @Test void theBaseOnlyGrowsOrShrinksByTheGivenAmount() {
        INode node = new NodeBase();
        node.setAspects(new AspectList().add(Aspect.AIR, 10));
        node.setNodeVisBase(Aspect.AIR, (short) 20);
        assertEquals(20, node.getNodeVisBase(Aspect.AIR));
        node.setNodeVisBase(Aspect.AIR, (short) 5);
        assertEquals(5, node.getNodeVisBase(Aspect.AIR));
    }

    @Test void theNodeTypeAndModifierAreTheOriginalOnes() {
        INode node = new NodeBase();
        assertEquals(NodeType.NORMAL, node.getNodeType());
        assertNull(node.getNodeModifier());
        node.setNodeType(NodeType.UNSTABLE);
        node.setNodeModifier(NodeModifier.BRIGHT);
        assertEquals(NodeType.UNSTABLE, node.getNodeType());
        assertEquals(NodeModifier.BRIGHT, node.getNodeModifier());
        assertNotNull(node.getId());
        assertEquals(6, NodeType.values().length);
        assertEquals(3, NodeModifier.values().length);
    }
}
