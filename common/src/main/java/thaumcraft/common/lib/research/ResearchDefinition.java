package thaumcraft.common.lib.research;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Collections;
import java.util.LinkedHashMap;

/** Original TC4 research data. Legacy expressions describe provenance only. */
public record ResearchDefinition(
        String key, String category, int column, int row, int complexity, int warp,
        Map<String, Integer> aspectCost, Set<String> flags,
        List<String> parents, List<String> parentsHidden, List<String> siblings,
        List<String> aspectTriggers, List<String> entityTriggers,
        List<String> itemTriggerExpressions, String iconExpression,
        List<Page> pages, List<String> conditions) {

    public ResearchDefinition {
        aspectCost = Collections.unmodifiableMap(new LinkedHashMap<>(aspectCost));
        flags = Set.copyOf(flags);
        parents = List.copyOf(parents);
        parentsHidden = List.copyOf(parentsHidden);
        siblings = List.copyOf(siblings);
        aspectTriggers = List.copyOf(aspectTriggers);
        entityTriggers = List.copyOf(entityTriggers);
        itemTriggerExpressions = List.copyOf(itemTriggerExpressions);
        pages = List.copyOf(pages);
        conditions = List.copyOf(conditions);
    }

    public boolean isAutoUnlock() { return this.flags.contains("AutoUnlock"); }
    /** The immutable warp field is provenance; gameplay and warnings use the live public registry. */
    public int currentWarp() { return thaumcraft.api.ThaumcraftApi.getWarpForResearch(this); }
    public boolean isVirtual() { return this.flags.contains("Virtual"); }
    public boolean isHidden() { return this.flags.contains("Hidden"); }
    public boolean isLost() { return this.flags.contains("Lost"); }
    public boolean isConcealed() { return this.flags.contains("Concealed"); }
    public boolean isSecondary() { return this.flags.contains("Secondary"); }
    public boolean isEnabled() { return ResearchConditions.available(conditions); }

    public boolean hasPrerequisites(Set<String> completed) {
        return completed.containsAll(this.parents) && completed.containsAll(this.parentsHidden);
    }

    public String nameKey() { return "tc.research_name." + this.key; }
    public String textKey() { return "tc.research_text." + this.key; }

    /**
     * One page of a research entry.
     *
     * <p>{@code source} is the {@code thaumcraft.api.research.ResearchPage} an add-on registered, for a page of
     * kind {@code "api"}: its recipe objects, aspects or image are drawn from that page. It is null for every
     * page of the original corpus.</p>
     */
    public record Page(String kind, String textKey, String recipeKey,
                       String requiredResearch, String javaExpression, Object source) {
        public Page(String kind, String textKey, String recipeKey, String requiredResearch, String javaExpression) {
            this(kind, textKey, recipeKey, requiredResearch, javaExpression, null);
        }

        public boolean isVisible(Set<String> completed) {
            return this.requiredResearch == null || completed.contains(this.requiredResearch);
        }
    }
}
