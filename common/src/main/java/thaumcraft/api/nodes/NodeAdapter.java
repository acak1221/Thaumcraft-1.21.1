package thaumcraft.api.nodes;

import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.tiles.TileNode;

/**
 * The public view of the native aura node.
 *
 * <p>Forced 1.21.1 change: the native node keeps its own {@code NodeType} and {@code NodeModifier} enums and
 * is final, so the type and the modifier cannot be written through this view; the native node fixes them
 * when it is created. The base essence can be written, because the native node accepts it.</p>
 */
public class NodeAdapter implements INode {
    private final TileNode node;

    public NodeAdapter(TileNode node) {
        if (node == null) throw new IllegalArgumentException("A node view needs a native node");
        this.node = node;
    }

    /** The native node behind this view. */
    public TileNode node() { return node; }

    @Override public String getId() { return node.getScanId(); }

    @Override public AspectList getAspects() { return node.getAspects(); }

    @Override public void setAspects(AspectList values) { node.setAspects(values); }

    @Override public AspectList getAspectsBase() { return node.getAspectsBase(); }

    @Override public NodeType getNodeType() { return NodeType.valueOf(node.getNodeType().name()); }

    @Override public void setNodeType(NodeType nodeType) {
        throw new UnsupportedOperationException("The native aura node fixes its type when it is created");
    }

    @Override public void setNodeModifier(NodeModifier nodeModifier) {
        throw new UnsupportedOperationException("The native aura node fixes its modifier when it is created");
    }

    @Override public NodeModifier getNodeModifier() {
        return node.getNodeModifier() == null ? null : NodeModifier.valueOf(node.getNodeModifier().name());
    }

    @Override public int getNodeVisBase(Aspect aspect) { return node.getNodeVisBase(aspect); }

    /** The original rule: the base only grows, or it shrinks by the given amount. */
    @Override public void setNodeVisBase(Aspect aspect, short nodeVisBase) {
        if (aspect == null) return;
        AspectList base = node.getAspectsBase();
        if (base.getAmount(aspect) < nodeVisBase) {
            base.merge(aspect, nodeVisBase);
        } else {
            base.reduce(aspect, base.getAmount(aspect) - nodeVisBase);
        }
        AspectList current = node.getAspects();
        node.setAspects(base);
        for (Aspect held : current.getAspects()) {
            int wanted = current.getAmount(held);
            int now = node.getAspects().getAmount(held);
            if (now > wanted) node.takeFromContainer(held, now - wanted);
        }
    }

    @Override public boolean doesContainerAccept(Aspect aspect) { return node.doesContainerAccept(aspect); }

    @Override public int addToContainer(Aspect aspect, int amount) { return node.addToContainer(aspect, amount); }

    @Override public boolean takeFromContainer(Aspect aspect, int amount) { return node.takeFromContainer(aspect, amount); }

    @Override @Deprecated public boolean takeFromContainer(AspectList values) { return node.takeFromContainer(values); }

    @Override public boolean doesContainerContainAmount(Aspect aspect, int amount) {
        return node.doesContainerContainAmount(aspect, amount);
    }

    @Override @Deprecated public boolean doesContainerContain(AspectList values) { return node.doesContainerContain(values); }

    @Override public int containerContains(Aspect aspect) { return node.containerContains(aspect); }
}
