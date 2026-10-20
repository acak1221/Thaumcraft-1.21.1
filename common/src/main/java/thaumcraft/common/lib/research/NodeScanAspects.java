package thaumcraft.common.lib.research;

import java.util.Objects;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.tiles.TileNode.NodeType;

/** Original generateNodeAspects: per-tag scaling, then type-specific MAX and ADD operations in their exact order. */
public final class NodeScanAspects {
    private NodeScanAspects() {}

    public static AspectList get(AspectList current, NodeType type) {
        Objects.requireNonNull(type, "type");
        AspectList tags = ScanManager.nodeScanAspects(current);
        switch (type) {
            case UNSTABLE -> tags.merge(Aspect.ENTROPY, 4);
            case HUNGRY -> tags.merge(Aspect.HUNGER, 4);
            case TAINTED -> tags.merge(Aspect.TAINT, 4);
            case PURE -> { tags.merge(Aspect.HEAL, 2); tags.add(Aspect.ORDER, 2); }
            case DARK -> { tags.merge(Aspect.DEATH, 2); tags.add(Aspect.DARKNESS, 2); }
            case NORMAL -> { }
        }
        return tags;
    }
}
