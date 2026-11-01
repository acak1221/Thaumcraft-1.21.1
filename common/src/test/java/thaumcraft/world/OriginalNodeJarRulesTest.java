package thaumcraft.world;

import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.Test;
import thaumcraft.api.nodes.NodeModifier;
import thaumcraft.common.items.wands.WandData;
import thaumcraft.common.lib.NodeJarFormation;

import static org.junit.jupiter.api.Assertions.*;

class OriginalNodeJarRulesTest {
    @Test void originalRecipeCostsSeventyWholeVisOfEveryPrimal() {
        assertEquals(new HashSet<>(WandData.PRIMALS), NodeJarFormation.FORM_VIS.keySet());
        assertEquals(List.of(70), NodeJarFormation.FORM_VIS.values().stream().distinct().toList());
    }

    @Test void originalThreeQuarterDegradationChangesExactlyOneModifierStep() {
        assertNull(NodeJarFormation.degradedModifier(NodeModifier.BRIGHT, .749999F));
        assertEquals(NodeModifier.PALE, NodeJarFormation.degradedModifier(null, .749999F));
        assertEquals(NodeModifier.FADING, NodeJarFormation.degradedModifier(NodeModifier.PALE, .749999F));
        assertEquals(NodeModifier.FADING, NodeJarFormation.degradedModifier(NodeModifier.FADING, .749999F));
        for (float preservedRoll : new float[]{.75F, .999999F}) {
            assertNull(NodeJarFormation.degradedModifier(null, preservedRoll));
            for (NodeModifier modifier : NodeModifier.values()) assertEquals(modifier, NodeJarFormation.degradedModifier(modifier, preservedRoll));
        }
    }
}
