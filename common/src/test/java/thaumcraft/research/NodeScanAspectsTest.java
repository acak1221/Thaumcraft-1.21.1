package thaumcraft.common.lib.research;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.nodes.NodeType;

class NodeScanAspectsTest {
    @Test void specialTypeMaxBonusesDoNotAddToAnAlreadyScaledAspect() {
        var current = new AspectList().add(Aspect.ENTROPY, 90).add(Aspect.HUNGER, 50).add(Aspect.TAINT, 1);
        assertEquals(9, NodeScanAspects.get(current, NodeType.UNSTABLE).getAmount(Aspect.ENTROPY));
        assertEquals(5, NodeScanAspects.get(current, NodeType.HUNGRY).getAmount(Aspect.HUNGER));
        assertEquals(4, NodeScanAspects.get(current, NodeType.TAINTED).getAmount(Aspect.TAINT));
        assertEquals(90, current.getAmount(Aspect.ENTROPY)); assertEquals(1, current.getAmount(Aspect.TAINT));
    }

    @Test void pureAndDarkUseMaxThenAdditionIncludingEmptyNodes() {
        var current = new AspectList().add(Aspect.HEAL, 70).add(Aspect.ORDER, 50).add(Aspect.DEATH, 60).add(Aspect.DARKNESS, 80);
        var pure = NodeScanAspects.get(current, NodeType.PURE);
        assertEquals(7, pure.getAmount(Aspect.HEAL)); assertEquals(7, pure.getAmount(Aspect.ORDER));
        var dark = NodeScanAspects.get(current, NodeType.DARK);
        assertEquals(6, dark.getAmount(Aspect.DEATH)); assertEquals(10, dark.getAmount(Aspect.DARKNESS));
        var emptyPure = NodeScanAspects.get(new AspectList(), NodeType.PURE);
        assertEquals(2, emptyPure.getAmount(Aspect.HEAL)); assertEquals(2, emptyPure.getAmount(Aspect.ORDER));
        assertEquals(0, NodeScanAspects.get(new AspectList(), NodeType.NORMAL).size());
    }
}
