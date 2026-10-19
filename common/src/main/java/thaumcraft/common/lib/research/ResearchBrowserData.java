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
        if (research == null || research.isVirtual() || !research.conditions().isEmpty()) return false;
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

    public static List<String> categories(Set<String> completed) {
        return CATEGORY_ORDER.stream().filter(category -> isCategoryVisible(category, completed)).toList();
    }

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
                        || "legacy_expression".equals(page.kind()) && page.javaExpression() != null) && recipeAvailable.test(page))
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
            if (!research.conditions().isEmpty() || !knowledge.isResearchComplete(research.key())) continue;
            for (String sibling : research.siblings()) {
                ResearchDefinition definition = byKey.get(sibling);
                if (definition != null && !definition.conditions().isEmpty()) continue;
                if (!knowledge.addResearch(sibling)) continue;
                changed = true;
                // Original ResearchManager.completeResearch, default wuss=false.
                // A native wuss config is still a separate port unit.
                int warp = definition == null ? 0 : definition.warp();
                if (warp > 0) {
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
