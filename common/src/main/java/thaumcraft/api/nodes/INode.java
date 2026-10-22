package thaumcraft.api.nodes;

import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IAspectContainer;

/**
 * The original TC4 node contract.
 *
 * <p>Forced 1.21.1 change: none of the members changed; the essence amount of the original was a
 * {@code short} and stays one.</p>
 *
 * <p><b>The identity of the node.</b> Oracle {@code thaumcraft/api/nodes/INode.java:12-29} publishes
 * {@link #getId()}, and the original node derived that identity from its own position
 * ({@code dim:x:y:z}, oracle {@code thaumcraft/common/tiles/TileNode.java:98-115}). The native node
 * keeps a <b>persisted random UUID</b> instead, and that UUID is the identity the scan of the node
 * uses (the phenomena key {@code NODE<uuid>}). The format cannot be changed without invalidating
 * the accepted client evidence of the port: the client test
 * {@code fabric/src/clientTest/java/thaumcraft/fabric/clienttest/FocalManipulatorClientSequence.java:509}
 * parses the identity with {@code UUID.fromString}, the accepted saved worlds carry the UUIDs in
 * their node NBT and the accepted scan records carry the phenomena string
 * {@code @NODE1b1965a9-747f-4d43-82ff-91e1f0ab443b}
 * ({@code fabric/src/clientTest/java/thaumcraft/fabric/clienttest/BoreClientSequence.java:870-871}).
 * Every other member of the contract carries the original's semantics exactly.</p>
 *
 * <p><b>The aspect lists are copies.</b> The original returned its live {@code AspectList} from
 * {@link #getAspects()} and {@link #getAspectsBase()}; the native returns a copy, so an add-on can
 * never mutate the state of a node through the published view. The values are the original's.</p>
 */
public interface INode extends IAspectContainer {
    /**
     * The identity of the node.
     *
     * <p>Original {@code INode:14}: the identity the original derived from the position of the
     * node ({@code dim:x:y:z}, oracle {@code TileNode:98-115}). The native node returns its
     * persisted UUID instead; see the javadoc of this interface for why that format is kept.</p>
     */
    String getId();

    /**
     * The base of the node: the capacity of every aspect of the node.
     *
     * <p>Original {@code INode:16} and {@code TileNode:190,265-271}: the base is the amount the
     * node may hold of an aspect, and writing it changes <b>only</b> the base, never the current
     * essence of the node.</p>
     */
    AspectList getAspectsBase();

    NodeType getNodeType();

    void setNodeType(NodeType nodeType);

    void setNodeModifier(NodeModifier nodeModifier);

    NodeModifier getNodeModifier();

    int getNodeVisBase(Aspect aspect);

    void setNodeVisBase(Aspect aspect, short nodeVisBase);
}
