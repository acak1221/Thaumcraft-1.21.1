package thaumcraft.api.nodes;

import java.util.UUID;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;

/**
 * A complete, self-contained {@link INode} implementation for an add-on's own node.
 *
 * <p>The native aura node is the game's own node; an add-on that contributes a node type implements this
 * class instead of the native one. The container rules are the original ones: the current essence never
 * exceeds the base, and the base only grows or shrinks through {@link #setNodeVisBase}.</p>
 */
public class NodeBase implements INode {
    private final String id = UUID.randomUUID().toString();
    private AspectList aspects = new AspectList();
    private AspectList aspectsBase = new AspectList();
    private NodeType nodeType = NodeType.NORMAL;
    private NodeModifier nodeModifier;

    @Override public String getId() { return id; }

    @Override public AspectList getAspects() { return aspects.copy(); }

    @Override public void setAspects(AspectList values) {
        aspects = new AspectList();
        aspectsBase = new AspectList();
        if (values != null) {
            for (Aspect aspect : values.getAspects()) {
                int amount = Math.max(0, values.getAmount(aspect));
                aspects.add(aspect, amount);
                aspectsBase.add(aspect, amount);
            }
        }
    }

    @Override public AspectList getAspectsBase() { return aspectsBase.copy(); }

    @Override public NodeType getNodeType() { return nodeType; }

    @Override public void setNodeType(NodeType nodeType) { this.nodeType = nodeType; }

    @Override public void setNodeModifier(NodeModifier nodeModifier) { this.nodeModifier = nodeModifier; }

    @Override public NodeModifier getNodeModifier() { return nodeModifier; }

    @Override public int getNodeVisBase(Aspect aspect) { return aspectsBase.getAmount(aspect); }

    /** The original rule: the base only grows, or it shrinks by the given amount. */
    @Override public void setNodeVisBase(Aspect aspect, short nodeVisBase) {
        if (aspect == null) return;
        if (aspectsBase.getAmount(aspect) < nodeVisBase) {
            aspectsBase.merge(aspect, nodeVisBase);
        } else {
            aspectsBase.reduce(aspect, aspectsBase.getAmount(aspect) - nodeVisBase);
        }
    }

    /** Always true, even for null: this is a capability answer, not validation that an insertion will succeed. */
    @Override public boolean doesContainerAccept(Aspect aspect) { return true; }

    /** Null aspects or nonpositive amounts return the full amount unchanged; other insertions return the unaccepted remainder. */
    @Override public int addToContainer(Aspect aspect, int amount) {
        if (aspect == null || amount <= 0) return amount;
        int left = Math.max(0, amount + aspects.getAmount(aspect) - aspectsBase.getAmount(aspect));
        aspects.add(aspect, amount - left);
        return left;
    }

    /** Returns false for null aspects, nonpositive amounts or insufficient contents without removing anything. */
    @Override public boolean takeFromContainer(Aspect aspect, int amount) {
        if (aspect == null || amount <= 0) return false;
        return aspects.reduce(aspect, amount);
    }

    @Override @Deprecated public boolean takeFromContainer(AspectList values) { return false; }

    @Override public boolean doesContainerContainAmount(Aspect aspect, int amount) {
        return aspect != null && aspects.getAmount(aspect) >= amount;
    }

    @Override @Deprecated public boolean doesContainerContain(AspectList values) { return false; }

    @Override public int containerContains(Aspect aspect) { return aspect == null ? 0 : aspects.getAmount(aspect); }
}
