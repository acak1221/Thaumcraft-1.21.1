package thaumcraft.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.nodes.INode;
import thaumcraft.api.nodes.NodeAdapter;
import thaumcraft.api.nodes.NodeModifier;
import thaumcraft.api.nodes.NodeType;
import thaumcraft.common.tiles.TileNode;

/**
 * The published node contract of the native aura node.
 *
 * <p>Original {@code thaumcraft/api/nodes/INode.java:12-29} and
 * {@code thaumcraft/common/tiles/TileNode.java:75-77, 240-271} (CFR 0.152 decompilation of
 * {@code Thaumcraft-1.7.10-4.2.3.5.jar}). The original node itself implements the published
 * contract, writes its type and its modifier through it and changes only the base of an
 * aspect when the base of that aspect is set.</p>
 *
 * <p>Forced 1.21.1 change: the native node keeps its own nested type and modifier enums, so
 * the published view of the node translates them by name. The test never constructs a block: it
 * only uses the already registered air block of the vanilla bootstrap.</p>
 */
class NodeAdapterContractTest {
    @BeforeAll static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    /** A detached native node; the original creative fixture gives every primal twenty five. */
    private static TileNode nativeNode() {
        return new TileNode(null, new BlockPos(0, 64, 0), Blocks.AIR.defaultBlockState());
    }

    @Test void thePublishedViewAnswersTheOriginalContract() {
        TileNode node = nativeNode();
        INode view = NodeAdapter.of(node);
        assertNotNull(view, "the published view of a native node is reachable");
        assertEquals(node.getScanId(), view.getId(), "the identity of the node is the scan id");
        assertEquals(NodeType.NORMAL, view.getNodeType());
        assertNull(view.getNodeModifier(), "the original node has no modifier until one is written");
        view.setNodeType(NodeType.UNSTABLE);
        assertEquals(NodeType.UNSTABLE, view.getNodeType(), "the type is written through the published contract");
        assertEquals(TileNode.NodeType.UNSTABLE, node.getNodeType(), "the native node carries the written type");
        view.setNodeModifier(NodeModifier.BRIGHT);
        assertEquals(NodeModifier.BRIGHT, view.getNodeModifier());
        view.setNodeModifier(null);
        assertNull(view.getNodeModifier(), "a null modifier clears it, like the original");
    }

    @Test void theBaseChangesWithoutTouchingTheCurrentEssence() {
        TileNode node = nativeNode();
        INode view = NodeAdapter.of(node);
        int current = view.getAspects().getAmount(Aspect.AIR);
        assertEquals(25, current, "the creative fixture of the native node holds twenty five of every primal");
        view.setNodeVisBase(Aspect.AIR, (short) 40);
        assertEquals(40, view.getNodeVisBase(Aspect.AIR), "the base of the aspect is the written amount");
        assertEquals(current, view.getAspects().getAmount(Aspect.AIR),
                "the current essence of the node survives the change of the base");
        view.setNodeVisBase(Aspect.AIR, (short) 5);
        assertEquals(5, view.getNodeVisBase(Aspect.AIR), "the base shrinks to the written amount");
        assertEquals(current, view.getAspects().getAmount(Aspect.AIR),
                "the current essence of the node still survives the shrink of the base");
    }

    @Test void theCurrentEssenceNeverExceedsTheBase() {
        INode view = NodeAdapter.of(nativeNode());
        view.setNodeVisBase(Aspect.AIR, (short) 30);
        assertEquals(0, view.addToContainer(Aspect.AIR, 5), "the essence that fits the base is taken");
        assertEquals(30, view.getAspects().getAmount(Aspect.AIR));
        assertEquals(5, view.addToContainer(Aspect.AIR, 5), "the essence that does not fit the base is refused");
        assertEquals(30, view.getAspects().getAmount(Aspect.AIR), "the current essence never exceeds the base");
        assertTrue(view.takeFromContainer(Aspect.AIR, 4), "the current essence can be taken");
        assertEquals(26, view.getAspects().getAmount(Aspect.AIR));
    }
}
