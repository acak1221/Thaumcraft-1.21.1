package thaumcraft.common.lib.research;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * TC4 browser visibility and normal-book side effects, without client classes.
 * Conditional catalogue rows stay unavailable until a native config/compat
 * resolver exists; their original Java expressions are provenance, not config.
 */
public final class ResearchBrowserData {
    // Original ConfigResearch.initCategories registration order.
    private static final List<String> CATEGORY_ORDER = List.of(
            "BASICS", "THAUMATURGY", "ALCHEMY", "ARTIFICE", "GOLEMANCY", "ELDRITCH");

    private ResearchBrowserData() {}

    public static boolean isVisible(ResearchDefinition research, Set<String> completed) {
        if (research == null || research.isVirtual() || !research.isEnabled()) return false;
        if (!isCategoryVisible(research.category(), completed)) return false;
        // Original GuiResearchBrowser icon loop: completion and @ clue each
        // bypass Hidden/Lost/Concealed checks; Virtual still never draws.
        if (completed.contains(research.key()) || completed.contains("@" + research.key())) return true;
        return !research.isHidden() && !research.isLost()
                && (!research.flags().contains("Concealed") || research.hasPrerequisites(completed));
    }

    public static List<ResearchDefinition> entries(String category, Set<String> completed) {
        return ResearchCatalog.all().stream()
                .filter(research -> research.category().equals(category))
                .filter(research -> isVisible(research, completed))
                .toList();
    }

    /**
     * Every research of the category, visible or not.
     *
     * <p>Original {@code thaumcraft/client/gui/GuiResearchBrowser.java:132-134} fills its research
     * list with every entry of the selected category, and the connector loop of {@code :299-348}
     * walks that list: the line of a hidden research is drawn as well, even when its cell is
     * never drawn.</p>
     */
    public static List<ResearchDefinition> categoryEntries(String category) {
        return ResearchCatalog.all().stream()
                .filter(research -> research.category().equals(category))
                .toList();
    }

    /**
     * The tabs of the book: the six original categories, then the categories add-ons registered, in
     * registration order (original GuiResearchBrowser walks {@code ResearchCategories.researchCategories}).
     */
    public static List<String> categories(Set<String> completed) {
        List<String> ordered = new java.util.ArrayList<>(CATEGORY_ORDER);
        for (String key : thaumcraft.api.research.ResearchCategories.categoryKeys()) {
            if (key != null && !ordered.contains(key)) ordered.add(key);
        }
        return ordered.stream().filter(category -> isCategoryVisible(category, completed)).toList();
    }

    /** True for one of the six categories of the original. */
    public static boolean isOriginalCategory(String category) { return CATEGORY_ORDER.contains(category); }

    /** Text-only compatibility view; native recipe availability is client-side. */
    public static List<ResearchDefinition.Page> readablePages(ResearchDefinition research, Set<String> completed) {
        if (research == null || !isVisible(research, completed) || !completed.contains(research.key())) return List.of();
        return research.pages().stream()
                .filter(page -> "text".equals(page.kind()) && page.textKey() != null)
                .filter(page -> page.isVisible(completed))
                .toList();
    }

    /**
     * Original page order and completion/concealed-text gates. A recipe page is
     * included only when the caller resolves an actual, renderable native recipe;
     * neither the extracted key nor its Java expression is executable recipe data.
     */
    public static List<ResearchDefinition.Page> displayablePages(ResearchDefinition research, Set<String> completed,
                                                                Predicate<ResearchDefinition.Page> recipeAvailable) {
        if (research == null || !isVisible(research, completed) || !completed.contains(research.key())) return List.of();
        return research.pages().stream()
                .filter(page -> page.isVisible(completed))
                .filter(page -> "text".equals(page.kind()) && page.textKey() != null
                        || ("recipe_reference".equals(page.kind()) && page.recipeKey() != null
                        || "legacy_expression".equals(page.kind()) && page.javaExpression() != null
                        || "api".equals(page.kind()) && page.source() != null) && recipeAvailable.test(page))
                .toList();
    }

    /**
     * Original ItemThaumonomicon normal-use branch: one live pass, no sibling
     * prerequisite check and no fixed-point closure. The extracted catalogue is
     * key-ordered rather than the original per-category HashMap iteration. In
     * the pinned corpus no sibling target has siblings, so that order cannot
     * affect the result (covered by the corpus test).
     */
    public static boolean applyBookSiblingUnlocks(PlayerKnowledge knowledge) {
        return applyBookSiblingUnlocks(knowledge, List.copyOf(ResearchCatalog.all()));
    }

    static boolean applyBookSiblingUnlocks(PlayerKnowledge knowledge, List<ResearchDefinition> orderedResearch) {
        Map<String, ResearchDefinition> byKey = new LinkedHashMap<>();
        for (ResearchDefinition research : orderedResearch) byKey.put(research.key(), research);
        boolean changed = false;
        for (ResearchDefinition research : orderedResearch) {
            if (!research.isEnabled() || !knowledge.isResearchComplete(research.key())) continue;
            for (String sibling : research.siblings()) {
                ResearchDefinition definition = byKey.get(sibling);
                if (definition != null && !definition.isEnabled()) continue;
                if (!knowledge.addResearch(sibling)) continue;
                changed = true;
                // Original completion awards warp only when wuss mode is disabled.
                int warp = definition == null ? 0 : definition.currentWarp();
                if (warp > 0 && !thaumcraft.common.lib.WarpEvents.isWussMode()) {
                    knowledge.addWarpPerm(warp - warp / 2);
                    knowledge.addWarpSticky(warp / 2);
                    knowledge.setWarpCounter(knowledge.getTotalWarp());
                }
            }
        }
        return changed;
    }

    private static boolean isCategoryVisible(String category, Set<String> completed) {
        return !"ELDRITCH".equals(category) || completed.contains("ELDRITCHMINOR");
    }
}
