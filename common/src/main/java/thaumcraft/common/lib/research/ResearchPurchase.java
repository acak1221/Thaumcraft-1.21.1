package thaumcraft.common.lib.research;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import thaumcraft.api.aspects.Aspect;

/**
 * TC4 Secondary research purchases at the original defaults: normal difficulty
 * (0), wuss mode false. Native research configuration and primary notes are
 * separate pending units. The caller owns server-thread/network validation.
 *
 * The original GUI checked affordability but PacketPlayerCompleteToServer did
 * not. Rechecking all admission rules and costs before any debit intentionally
 * fixes that authority defect; PlayerKnowledge's overdraw quirk is unchanged.
 */
public final class ResearchPurchase {
    public enum Failure {
        NONE, UNKNOWN_RESEARCH, UNAVAILABLE, ALREADY_COMPLETE,
        MISSING_PREREQUISITES, MISSING_CLUE, PRIMARY_REQUIRES_NOTE, INSUFFICIENT_ASPECTS
    }

    /** Costs quote the canonical price; unlocked lists only committed grants. */
    public record Result(Failure failure, Map<String, Integer> costs, Set<String> unlocked) {
        public Result {
            Objects.requireNonNull(failure, "failure");
            costs = Collections.unmodifiableMap(new LinkedHashMap<>(costs));
            unlocked = Collections.unmodifiableSet(new LinkedHashSet<>(unlocked));
        }

        public boolean success() { return failure == Failure.NONE; }
    }

    private ResearchPurchase() {}

    /** A non-mutating quote against the current knowledge, including failures. */
    public static Result preview(PlayerKnowledge knowledge, String key) {
        return preview(knowledge, key, ResearchCatalog::get);
    }

    /** Revalidates current state; successful grants replace it in one commit. */
    public static Result purchase(PlayerKnowledge knowledge, String key) {
        return purchase(knowledge, key, ResearchCatalog::get);
    }

    static Result preview(PlayerKnowledge knowledge, String key, Function<String, ResearchDefinition> resolver) {
        return validate(Objects.requireNonNull(knowledge, "knowledge"), resolve(key, resolver));
    }

    static Result purchase(PlayerKnowledge knowledge, String key, Function<String, ResearchDefinition> resolver) {
        Objects.requireNonNull(knowledge, "knowledge");
        ResearchDefinition research = resolve(key, resolver);
        Result quote = validate(knowledge, research);
        if (!quote.success()) return quote;

        PlayerKnowledge next = knowledge.copy();
        for (var cost : quote.costs().entrySet()) {
            // Full preflight guarantees this cannot exercise addAspectPool's
            // original successful-overdraw/no-debit behavior.
            next.addAspectPool(cost.getKey(), -cost.getValue());
        }
        Set<String> unlocked = ResearchCompletion.apply(next, research, resolver);
        Result result = new Result(Failure.NONE, quote.costs(), unlocked);
        knowledge.copyFrom(next);
        return result;
    }

    private static ResearchDefinition resolve(String key, Function<String, ResearchDefinition> resolver) {
        Objects.requireNonNull(resolver, "resolver");
        if (key == null || key.isBlank() || key.length() > 256 || key.startsWith("@")) return null;
        ResearchDefinition research = resolver.apply(key);
        return research != null && key.equals(research.key()) ? research : null;
    }

    private static Result validate(PlayerKnowledge knowledge, ResearchDefinition research) {
        if (research == null) return rejected(Failure.UNKNOWN_RESEARCH, Map.of());
        Map<String, Integer> costs = research.aspectCost();
        if (research.isVirtual() || research.isAutoUnlock() || !research.isEnabled()
                || ("ELDRITCH".equals(research.category()) && !knowledge.isResearchComplete("ELDRITCHMINOR"))
                || costs.isEmpty() || costs.entrySet().stream().anyMatch(cost ->
                        Aspect.getAspect(cost.getKey()) == null || cost.getValue() == null || cost.getValue() <= 0)) {
            return rejected(Failure.UNAVAILABLE, costs);
        }
        if (knowledge.isResearchComplete(research.key())) return rejected(Failure.ALREADY_COMPLETE, costs);
        // GuiResearchBrowser:644: default difficulty=0 purchases Secondary only.
        if (!research.isSecondary()) return rejected(Failure.PRIMARY_REQUIRES_NOTE, costs);
        if (!research.hasPrerequisites(knowledge.getResearchComplete())) {
            return rejected(Failure.MISSING_PREREQUISITES, costs);
        }
        if ((research.isHidden() || research.isLost()) && !knowledge.isResearchComplete("@" + research.key())) {
            return rejected(Failure.MISSING_CLUE, costs);
        }
        for (var cost : costs.entrySet()) {
            if (knowledge.getAspectPoolFor(cost.getKey()) < cost.getValue()) {
                return rejected(Failure.INSUFFICIENT_ASPECTS, costs);
            }
        }
        return new Result(Failure.NONE, costs, Set.of());
    }

    private static Result rejected(Failure failure, Map<String, Integer> costs) {
        return new Result(failure, costs, Set.of());
    }

}
