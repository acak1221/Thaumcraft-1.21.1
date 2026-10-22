package thaumcraft.blocks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.nodes.NodeModifier;
import thaumcraft.api.nodes.NodeType;
import thaumcraft.common.blocks.ItemJarNode;

/**
 * The published data contract of the item form of the node jar.
 *
 * <p>Oracle {@code thaumcraft/common/blocks/ItemJarNode.java:165-213}: the item publishes
 * {@code setAspects}, {@code setNodeAttributes}, {@code getNodeType}, {@code getNodeModifier} and
 * {@code getNodeId} for the aspects, the node type, the node modifier and the identity of the stored
 * node. The original writes the node type, the node modifier and the identity into the NBT of the item
 * and reads them back from it.</p>
 */
class ItemJarNodeContractTest {
    @Test void theNodeTypeTravelsWithTheItem() {
        CompoundTag tag = new CompoundTag();
        ItemJarNode.writeNodeAttributes(tag, NodeType.DARK, null, "node-1");
        assertEquals(NodeType.DARK, ItemJarNode.nodeTypeOf(tag));
        assertEquals("node-1", tag.getString("nodeId"));
    }

    @Test void theNodeModifierTravelsWithTheItem() {
        CompoundTag tag = new CompoundTag();
        ItemJarNode.writeNodeAttributes(tag, NodeType.NORMAL, NodeModifier.PALE, "");
        assertEquals(NodeModifier.PALE, ItemJarNode.nodeModifierOf(tag));
        // The original writes the modifier only when it is not null; the native
        // writes the minus one of the original tile, so the placed jar has no
        // modifier at all.
        ItemJarNode.writeNodeAttributes(tag, NodeType.NORMAL, null, "");
        assertNull(ItemJarNode.nodeModifierOf(tag));
        assertEquals(-1, tag.getInt("modifier"));
    }

    @Test void anItemWithoutDataCarriesNoNode() {
        // Oracle ItemJarNode:194-199 and :201-213 - an item without NBT has no node
        // type and no node modifier, and the original answers "0" for its identity.
        CompoundTag tag = new CompoundTag();
        assertNull(ItemJarNode.nodeTypeOf(tag));
        assertNull(ItemJarNode.nodeModifierOf(tag));
    }

    @Test void theAspectsOfTheItemUseTheKeyOfTheOriginal() {
        // Oracle ItemJarNode:165-181 - the aspects of the item live in the "Aspects"
        // list of the original, the same key the tile of the original uses.
        AspectList aspects = new AspectList().add(Aspect.AIR, 40).add(Aspect.FIRE, 40);
        CompoundTag tag = new CompoundTag();
        aspects.writeToNBT(tag);
        assertEquals(2, tag.getList("Aspects", net.minecraft.nbt.Tag.TAG_COMPOUND).size());
        AspectList read = new AspectList();
        read.readFromNBT(tag);
        assertEquals(40, read.getAmount(Aspect.AIR));
        assertEquals(40, read.getAmount(Aspect.FIRE));
    }
}
