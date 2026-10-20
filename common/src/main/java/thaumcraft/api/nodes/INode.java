package thaumcraft.api.nodes;

import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IAspectContainer;

/**
 * The original TC4 node contract.
 *
 * <p>Forced 1.21.1 change: none of the members changed; the essence amount of the original was a
 * {@code short} and stays one.</p>
 */
public interface INode extends IAspectContainer {
    String getId();

    AspectList getAspectsBase();

    NodeType getNodeType();

    void setNodeType(NodeType nodeType);

    void setNodeModifier(NodeModifier nodeModifier);

    NodeModifier getNodeModifier();

    int getNodeVisBase(Aspect aspect);

    void setNodeVisBase(Aspect aspect, short nodeVisBase);
}
