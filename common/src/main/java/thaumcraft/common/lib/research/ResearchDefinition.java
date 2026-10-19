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
    public boolean isVirtual() { return this.flags.contains("Virtual"); }
    public boolean isHidden() { return this.flags.contains("Hidden"); }
    public boolean isLost() { return this.flags.contains("Lost"); }
    public boolean isSecondary() { return this.flags.contains("Secondary"); }

    public boolean hasPrerequisites(Set<String> completed) {
        return completed.containsAll(this.parents) && completed.containsAll(this.parentsHidden);
    }

    public String nameKey() { return "tc.research_name." + this.key; }
    public String textKey() { return "tc.research_text." + this.key; }

    public record Page(String kind, String textKey, String recipeKey,
                       String requiredResearch, String javaExpression) {
        public boolean isVisible(Set<String> completed) {
            return this.requiredResearch == null || completed.contains(this.requiredResearch);
        }
    }
}
