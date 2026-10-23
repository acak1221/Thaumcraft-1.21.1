package thaumcraft.api.nodes;

import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.tiles.TileNode;

/**
 * The public view of the native aura node.
 *
 * <p>The original TC4 node itself implements the published contract (oracle
 * {@code thaumcraft/common/tiles/TileNode.java:75-77}), so an add-on casts the block entity of a
 * node to {@link INode}. The native node does the same: it implements {@link INode} with the
 * published {@link NodeType} and {@link NodeModifier} enums, exactly as the original published
 * them. This adapter stays as the documented bridge for an add-on that holds a reference to the
 * native node and wants the published view of it; both views share the same state, the same
 * write path and the same identity.</p>
 *
 * <p>Forced 1.21.1 change: none of the members changed; the native node carries the published
 * enums, so no translation by name is needed any more.</p>
 */
public class NodeAdapter implements INode {
    private final TileNode node;

    public NodeAdapter(TileNode node) {
        if (node == null) throw new IllegalArgumentException("A node view needs a native node");
        this.node = node;
    }

    /**
     * The published view of a native node.
     *
     * @param entity the block entity of a node, or null
     * @return the published view of the node, or null when the entity is not an aura node
     */
    public static INode of(net.minecraft.world.level.block.entity.BlockEntity entity) {
        return entity instanceof TileNode node ? new NodeAdapter(node) : null;
    }

    /** The native node behind this view. */
    public TileNode node() { return node; }

    @Override public String getId() { return node.getId(); }

    @Override public AspectList getAspects() { return node.getAspects(); }

    @Override public void setAspects(AspectList values) { node.setAspects(values); }

    @Override public AspectList getAspectsBase() { return node.getAspectsBase(); }

    @Override public NodeType getNodeType() { return node.getNodeType(); }

    @Override public void setNodeType(NodeType nodeType) {
        node.setNodeType(nodeType);
    }

    @Override public void setNodeModifier(NodeModifier nodeModifier) {
        node.setNodeModifier(nodeModifier);
    }

    @Override public NodeModifier getNodeModifier() {
        return node.getNodeModifier();
    }

    @Override public int getNodeVisBase(Aspect aspect) { return node.getNodeVisBase(aspect); }

    /** The original rule: the base only grows, or it shrinks by the given amount. */
    @Override public void setNodeVisBase(Aspect aspect, short nodeVisBase) {
        // Original TileNode:265-271: only the base changes, the current essence of the node
        // stays exactly as it is.
        node.setNodeVisBase(aspect, nodeVisBase);
    }

    @Override public boolean doesContainerAccept(Aspect aspect) { return node.doesContainerAccept(aspect); }

    @Override public int addToContainer(Aspect aspect, int amount) { return node.addToContainer(aspect, amount); }

    @Override public boolean takeFromContainer(Aspect aspect, int amount) { return node.takeFromContainer(aspect, amount); }

    /** Delegates legacy removal to the native node, which returns false without mutation even for null.
     * @deprecated Use takeFromContainer(Aspect, int); repeated single-aspect removals are not atomic. */
    @Override @Deprecated public boolean takeFromContainer(AspectList values) { return node.takeFromContainer(values); }

    @Override public boolean doesContainerContainAmount(Aspect aspect, int amount) {
        return node.doesContainerContainAmount(aspect, amount);
    }

    /** Delegates the legacy query to the native node, which returns false even for null.
     * @deprecated Use doesContainerContainAmount(Aspect, int) for explicit per-aspect checks; native nodes may reject that query too. */
    @Override @Deprecated public boolean doesContainerContain(AspectList values) { return node.doesContainerContain(values); }

    @Override public int containerContains(Aspect aspect) { return node.containerContains(aspect); }
}
