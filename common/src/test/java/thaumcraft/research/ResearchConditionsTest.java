package thaumcraft.research;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import thaumcraft.common.lib.research.ResearchConditions;
import static org.junit.jupiter.api.Assertions.*;

class ResearchConditionsTest {
    @Test void conjunctionRequiresEveryOriginalFact() {
        String expression = "if (Config.foundSilverOre && Config.foundSilverIngot)";
        assertFalse(ResearchConditions.evaluate(List.of(expression), Set.of("foundSilverOre")::contains));
        assertFalse(ResearchConditions.evaluate(List.of(expression), Set.of("foundSilverIngot")::contains));
        assertTrue(ResearchConditions.evaluate(List.of(expression), Set.of("foundSilverOre", "foundSilverIngot")::contains));
    }
    @Test void closedExpressionsDoNotEvaluateJavaOrAcceptAnEmptyConjunction() {
        for (String text : List.of("if (Config.wardedStone || true)", "Config.wardedStone", "if ()",
                "if (Config.wardedStone &&)", "if (Config.wardedStone())")) {
            assertFalse(ResearchConditions.evaluate(List.of(text), ignored -> true), text);
        }
    }
    @Test void originalFeatureDefaultsAndNativeCopperAreAvailable() {
        assertTrue(ResearchConditions.available(List.of("if (Config.wardedStone)")));
        assertTrue(ResearchConditions.available(List.of("if (Config.allowMirrors)")));
        assertTrue(ResearchConditions.available(List.of("if (Config.foundCopperOre && Config.foundCopperIngot)")));
        assertFalse(ResearchConditions.available(List.of("if (Config.unknownFlag)")));
        assertTrue(ResearchConditions.available(List.of()));
    }
}
