package thaumcraft.common.lib.research;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The connector lines of the original research browser.
 *
 * <p>Oracle {@code thaumcraft/client/gui/GuiResearchBrowser.java:299-348} draws two kinds of
 * connectors between the cells of the selected category:</p>
 * <ul>
 *   <li>the <b>parent</b> connectors of {@code :303-326}: every research is joined with every
 *       parent of the same category;</li>
 *   <li>the <b>sibling</b> connectors of {@code :327-348}: every research is joined with every
 *       sibling of the same category whose own parents do not name it, so that the parent
 *       connector is not drawn twice.</li>
 * </ul>
 *
 * <p>The colour of a connector is the state of its two ends:</p>
 * <ul>
 *   <li>the research itself is complete: a flat dark grey, {@code 0.1, 0.1, 0.1} for a parent and
 *       {@code 0.1, 0.1, 0.2} for a sibling, without the wobble;</li>
 *   <li>the other end is complete: green, {@code 0.0, 1.0, 0.0}, with the wobble;</li>
 *   <li>otherwise: blue, {@code 0.0, 0.0, 1.0}, with the wobble.</li>
 * </ul>
 *
 * <p>A connector is not drawn at all when one of its ends is lost, hidden without its clue or
 * concealed without its prerequisites. The connector line itself is the three pixel wide
 * gradient polyline of {@code :732-787}.</p>
 */
public final class ResearchBrowserConnectors {
    /** Oracle GuiResearchBrowser:750 - the width of the connector line in pixels. */
    public static final float WIDTH = 3.0F;
    /** Oracle GuiResearchBrowser:765 - the alpha of a vertex of the connector line. */
    public static final float VERTEX_ALPHA = 0.6F;
    /** Oracle GuiResearchBrowser:768-769 - the amplitude of the wobble of the connector line. */
    public static final float WOBBLE = 5.0F;
    /** Oracle GuiResearchBrowser:768 - the period of the horizontal wobble. */
    public static final int WOBBLE_X_PERIOD = 7;
    /** Oracle GuiResearchBrowser:769 - the period of the vertical wobble. */
    public static final int WOBBLE_Y_PERIOD = 5;

    private ResearchBrowserConnectors() {}

    /** One connector of the original browser. */
    public record Connector(String from, String to, float red, float green, float blue, boolean wiggle) {}

    /**
     * Every connector of the original browser for the selected category.
     *
     * @param category the selected category of the original
     * @param completed the completed research of the player, including the clues with the
     *                  {@code @} prefix
     */
    public static List<Connector> connectors(String category, Set<String> completed) {
        List<ResearchDefinition> all = List.copyOf(ResearchCatalog.all());
        Map<String, ResearchDefinition> byKey = new LinkedHashMap<>();
        for (ResearchDefinition research : all) byKey.put(research.key(), research);
        List<Connector> result = new ArrayList<>();
        for (ResearchDefinition research : all) {
            if (!research.category().equals(category)) continue;
            for (String parentKey : research.parents()) {
                ResearchDefinition parent = byKey.get(parentKey);
                // Oracle GuiResearchBrowser:306 - a parent of another category and a
                // virtual parent never carry a connector.
                if (parent == null || !parent.category().equals(category) || parent.isVirtual()) continue;
                Connector connector = parentConnector(research, parent, completed, byKey);
                if (connector != null) result.add(connector);
            }
            for (String siblingKey : research.siblings()) {
                ResearchDefinition sibling = byKey.get(siblingKey);
                // Oracle GuiResearchBrowser:330 - a sibling of another category, a virtual
                // sibling and a sibling that names this research as its parent carry no
                // connector of their own.
                if (sibling == null || !sibling.category().equals(category) || sibling.isVirtual()) continue;
                if (sibling.parents().contains(research.key())) continue;
                Connector connector = siblingConnector(research, sibling, completed, byKey);
                if (connector != null) result.add(connector);
            }
        }
        return List.copyOf(result);
    }

    /** Oracle GuiResearchBrowser:303-326 - the connector of a parent. */
    public static Connector parentConnector(ResearchDefinition research, ResearchDefinition parent,
                                            Set<String> completed, Map<String, ResearchDefinition> byKey) {
        if (completed.contains(research.key())) return new Connector(research.key(), parent.key(), 0.1F, 0.1F, 0.1F, false);
        if (hiddenWithoutClue(research, completed) || (research.isConcealed() && !canUnlock(research, completed, byKey))) return null;
        if (completed.contains(parent.key())) return new Connector(research.key(), parent.key(), 0.0F, 1.0F, 0.0F, true);
        if ((parent.isHidden() || research.isLost()) && !completed.contains("@" + parent.key())) return null;
        if (parent.isConcealed() && !canUnlock(parent, completed, byKey)) return null;
        return new Connector(research.key(), parent.key(), 0.0F, 0.0F, 1.0F, true);
    }

    /** Oracle GuiResearchBrowser:327-348 - the connector of a sibling. */
    public static Connector siblingConnector(ResearchDefinition research, ResearchDefinition sibling,
                                             Set<String> completed, Map<String, ResearchDefinition> byKey) {
        if (completed.contains(research.key())) return new Connector(research.key(), sibling.key(), 0.1F, 0.1F, 0.2F, false);
        if (hiddenWithoutClue(research, completed) || (research.isConcealed() && !canUnlock(research, completed, byKey))) return null;
        if (completed.contains(sibling.key())) return new Connector(research.key(), sibling.key(), 0.0F, 1.0F, 0.0F, true);
        if (sibling.isHidden() && !completed.contains("@" + sibling.key())) return null;
        if (sibling.isConcealed() && !canUnlock(sibling, completed, byKey)) return null;
        return new Connector(research.key(), sibling.key(), 0.0F, 0.0F, 1.0F, true);
    }

    /**
     * Oracle GuiResearchBrowser:313,345 - a lost research and a hidden research without its
     * clue carry no connector.
     */
    public static boolean hiddenWithoutClue(ResearchDefinition research, Set<String> completed) {
        return research.isLost() || research.isHidden() && !completed.contains("@" + research.key());
    }

    /**
     * Oracle GuiResearchBrowser:713-727 ({@code canUnlockResearch}): every parent and every
     * hidden parent of the research must be complete. A parent key that the catalogue does not
     * know never blocks the research.
     */
    public static boolean canUnlock(ResearchDefinition research, Set<String> completed,
                                    Map<String, ResearchDefinition> byKey) {
        for (String parent : research.parents()) {
            ResearchDefinition definition = byKey.get(parent);
            if (definition == null || completed.contains(definition.key())) continue;
            return false;
        }
        for (String parent : research.parentsHidden()) {
            ResearchDefinition definition = byKey.get(parent);
            if (definition == null || completed.contains(definition.key())) continue;
            return false;
        }
        return true;
    }

    /**
     * Oracle GuiResearchBrowser:742-747 - the number of the segments of the connector line:
     * the half of the distance between its two ends.
     */
    public static int segments(float dx, float dy) {
        return (int) (Math.sqrt(dx * dx + dy * dy) / 2.0F);
    }

    /** Oracle GuiResearchBrowser:768 - the horizontal offset of a vertex of the wobbling line. */
    public static float wobbleX(float count, int step, int segments) {
        return (float) Math.sin((count + step) / WOBBLE_X_PERIOD) * WOBBLE * (1.0F - phase(step, segments));
    }

    /** Oracle GuiResearchBrowser:769 - the vertical offset of a vertex of the wobbling line. */
    public static float wobbleY(float count, int step, int segments) {
        return (float) Math.sin((count + step) / WOBBLE_Y_PERIOD) * WOBBLE * (1.0F - phase(step, segments));
    }

    /** Oracle GuiResearchBrowser:767 - the phase of a vertex along the line. */
    public static float phase(int step, int segments) {
        return segments <= 0 ? 0.0F : (float) step / (float) segments;
    }

    /** Oracle GuiResearchBrowser:770-772 - the colour of a vertex fades to black along the line. */
    public static float fade(int step, int segments) {
        return 1.0F - phase(step, segments);
    }
}
