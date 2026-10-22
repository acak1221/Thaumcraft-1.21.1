package thaumcraft.research;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import thaumcraft.common.lib.research.ResearchBrowserConnectors;
import thaumcraft.common.lib.research.ResearchDefinition;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The connector lines of the original research browser of TC4 4.2.3.5.
 *
 * <p>Oracle {@code thaumcraft/client/gui/GuiResearchBrowser.java:299-348} draws the parent and the
 * sibling connectors, {@code :732-787} draws the three pixel wide gradient polyline itself.</p>
 */
class ResearchBrowserConnectorTest {
    private static ResearchDefinition research(String key, List<String> parents, List<String> siblings, String... flags) {
        return new ResearchDefinition(key, "BASICS", 0, 0, 1, 0, Map.of(), Set.of(flags), parents, List.of(),
                siblings, List.of(), List.of(), List.of(), null, List.of(), List.of());
    }

    private static Map<String, ResearchDefinition> byKey(ResearchDefinition... entries) {
        Map<String, ResearchDefinition> map = new LinkedHashMap<>();
        for (ResearchDefinition entry : entries) map.put(entry.key(), entry);
        return map;
    }

    @Test void theOriginalJoinsACompleteResearchWithADarkGreyFlatLine() {
        var child = research("CHILD", List.of("PARENT"), List.of());
        var parent = research("PARENT", List.of(), List.of());
        var connector = ResearchBrowserConnectors.parentConnector(child, parent, Set.of("CHILD"),
                byKey(child, parent));
        assertNotNull(connector);
        assertEquals(0.1F, connector.red());
        assertEquals(0.1F, connector.green());
        assertEquals(0.1F, connector.blue());
        assertFalse(connector.wiggle());
    }

    @Test void theOriginalJoinsACompleteParentWithAGreenWobblingLine() {
        var child = research("CHILD", List.of("PARENT"), List.of());
        var parent = research("PARENT", List.of(), List.of());
        var connector = ResearchBrowserConnectors.parentConnector(child, parent, Set.of("PARENT"),
                byKey(child, parent));
        assertNotNull(connector);
        assertEquals(0.0F, connector.red());
        assertEquals(1.0F, connector.green());
        assertEquals(0.0F, connector.blue());
        assertTrue(connector.wiggle());
    }

    @Test void theOriginalJoinsAnUnknownParentWithABlueWobblingLine() {
        var child = research("CHILD", List.of("PARENT"), List.of());
        var parent = research("PARENT", List.of(), List.of());
        var connector = ResearchBrowserConnectors.parentConnector(child, parent, Set.of(),
                byKey(child, parent));
        assertNotNull(connector);
        assertEquals(0.0F, connector.red());
        assertEquals(0.0F, connector.green());
        assertEquals(1.0F, connector.blue());
        assertTrue(connector.wiggle());
    }

    @Test void theOriginalDrawsNoLineForAHiddenResearchWithoutItsClue() {
        var child = research("CHILD", List.of("PARENT"), List.of(), "Hidden");
        var parent = research("PARENT", List.of(), List.of());
        assertNull(ResearchBrowserConnectors.parentConnector(child, parent, Set.of(), byKey(child, parent)));
        // The clue of the hidden research opens the line again.
        assertNotNull(ResearchBrowserConnectors.parentConnector(child, parent, Set.of("@CHILD"),
                byKey(child, parent)));
    }

    @Test void theOriginalDrawsNoLineForALostResearch() {
        var child = research("CHILD", List.of("PARENT"), List.of(), "Lost");
        var parent = research("PARENT", List.of(), List.of());
        assertNull(ResearchBrowserConnectors.parentConnector(child, parent, Set.of(), byKey(child, parent)));
    }

    @Test void theOriginalDrawsNoLineForAConcealedResearchWithoutItsPrerequisites() {
        var child = research("CHILD", List.of("PARENT"), List.of(), "Concealed");
        var parent = research("PARENT", List.of(), List.of());
        assertNull(ResearchBrowserConnectors.parentConnector(child, parent, Set.of(), byKey(child, parent)));
        // The complete prerequisite opens the line again.
        assertNotNull(ResearchBrowserConnectors.parentConnector(child, parent, Set.of("PARENT"),
                byKey(child, parent)));
    }

    @Test void theOriginalDrawsNoLineToAHiddenParentWithoutItsClue() {
        var child = research("CHILD", List.of("PARENT"), List.of());
        var parent = research("PARENT", List.of(), List.of(), "Hidden");
        assertNull(ResearchBrowserConnectors.parentConnector(child, parent, Set.of(), byKey(child, parent)));
        assertNotNull(ResearchBrowserConnectors.parentConnector(child, parent, Set.of("@PARENT"),
                byKey(child, parent)));
    }

    @Test void theOriginalJoinsASiblingWithADarkVioletFlatLineWhenTheResearchIsComplete() {
        var child = research("CHILD", List.of(), List.of("SIBLING"));
        var sibling = research("SIBLING", List.of(), List.of());
        var connector = ResearchBrowserConnectors.siblingConnector(child, sibling, Set.of("CHILD"),
                byKey(child, sibling));
        assertNotNull(connector);
        assertEquals(0.1F, connector.red());
        assertEquals(0.1F, connector.green());
        assertEquals(0.2F, connector.blue());
        assertFalse(connector.wiggle());
    }

    @Test void theOriginalJoinsASiblingWithABlueWobblingLine() {
        var child = research("CHILD", List.of(), List.of("SIBLING"));
        var sibling = research("SIBLING", List.of(), List.of());
        var connector = ResearchBrowserConnectors.siblingConnector(child, sibling, Set.of(),
                byKey(child, sibling));
        assertNotNull(connector);
        assertEquals(0.0F, connector.red());
        assertEquals(0.0F, connector.green());
        assertEquals(1.0F, connector.blue());
        assertTrue(connector.wiggle());
    }

    @Test void theOriginalLineHasThreePixelsAndTheVertexAlphaOfPointSix() {
        assertEquals(3.0F, ResearchBrowserConnectors.WIDTH);
        assertEquals(0.6F, ResearchBrowserConnectors.VERTEX_ALPHA);
        assertEquals(5.0F, ResearchBrowserConnectors.WOBBLE);
        assertEquals(7, ResearchBrowserConnectors.WOBBLE_X_PERIOD);
        assertEquals(5, ResearchBrowserConnectors.WOBBLE_Y_PERIOD);
    }

    @Test void theOriginalLineHasHalfTheDistanceOfItsSegments() {
        assertEquals(12, ResearchBrowserConnectors.segments(24, 0));
        assertEquals(6, ResearchBrowserConnectors.segments(12, 0));
        assertEquals(0, ResearchBrowserConnectors.segments(1, 0));
    }

    @Test void theOriginalWobbleFadesAlongTheLine() {
        int segments = 10;
        assertEquals(0.0F, ResearchBrowserConnectors.phase(0, segments));
        assertEquals(1.0F, ResearchBrowserConnectors.phase(segments, segments));
        assertEquals(1.0F, ResearchBrowserConnectors.fade(0, segments));
        assertEquals(0.0F, ResearchBrowserConnectors.fade(segments, segments));
        // The wobble of the first vertex is the full amplitude, the last one is zero.
        assertEquals(0.0F, ResearchBrowserConnectors.wobbleX(0, 0, segments) - ResearchBrowserConnectors.wobbleX(0, 0, segments));
        assertEquals(0.0F, ResearchBrowserConnectors.wobbleY(0, segments, segments));
    }
}
