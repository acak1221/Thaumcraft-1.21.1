package thaumcraft.common.lib.research;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import thaumcraft.api.aspects.Aspect;

/**
 * TC4 PacketAspectCombinationToServer:97-123 and ScanManager:420-440.
 * No ink, paper, Researcher discount or gameplay RNG draw belongs to combination.
 * Both discovered inputs must be payable before publishing anything, closing
 * the original packet's client-bonus and same-aspect single-point exploits.
 */
public final class ResearchAspectCombination {
    private ResearchAspectCombination() {}

    /** A null output is a paid, unsuccessful experiment, not an admission refusal. */
    public record Plan(PlayerKnowledge knowledge, Map<String, Integer> bonusPools,
                       Aspect output, int award, boolean discovery) {
        public Plan {
            bonusPools = Collections.unmodifiableMap(new LinkedHashMap<>(bonusPools));
        }
    }

    /** Stages only private copies; null means neither input may be spent. */
    public static Plan plan(PlayerKnowledge actor, Map<String, Integer> bonusPools, String left, String right) {
        if (!canCombine(actor, bonusPools, left, right)) return null;
        Aspect first = Aspect.getAspect(left), second = Aspect.getAspect(right);
        PlayerKnowledge staged = actor.copy();
        Map<String, Integer> bonus = new LinkedHashMap<>(bonusPools);
        // Sequential staging is important for equal inputs: RP1+bonus1 works,
        // RP1 alone does not, and the second branch observes the first debit.
        if (!debit(staged, bonus, left) || !debit(staged, bonus, right)) return null;
        Aspect output = Aspect.getCombinationResult(first, second);
        boolean discovery = output != null && !staged.hasDiscoveredAspect(output.getTag());
        int award = 0;
        if (output != null) {
            String tag = output.getTag();
            award = discovery ? 3 : 1; // checkAndSyncAspectKnowledge(combo, 1), plus first-discovery 2.
            int queried = ResearchNoteEditing.queryPool(staged.getAspectPoolFor(tag));
            int cap = ScanManager.DEFAULT_ASPECT_TOTAL_CAP;
            if (queried >= cap) award = (int) Math.sqrt(award);
            if (award > 1 && (float) queried >= (float) cap * 1.25F) award = 1;
            staged.addAspectPool(tag, (short) award);
            // Original completeAspect receives getAspectPoolFor's signed short,
            // unlike ordinary input debits, which leave their raw int storage.
            staged.setAspectPool(tag, ResearchNoteEditing.queryPool(staged.getAspectPoolFor(tag)));
        }
        return new Plan(staged, bonus, output, award, discovery);
    }

    /** Lightweight GUI preflight: never copy a player's entire scan/research history per frame. */
    public static boolean canCombine(PlayerKnowledge actor, Map<String, Integer> bonusPools, String left, String right) {
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(bonusPools, "bonus pools");
        Aspect first = Aspect.getAspect(left), second = Aspect.getAspect(right);
        if (first == null || second == null || !first.getTag().equals(left) || !second.getTag().equals(right)
                || !actor.hasDiscoveredAspect(left) || !actor.hasDiscoveredAspect(right)) return false;
        return first == second ? available(actor, bonusPools, left) >= 2
                : available(actor, bonusPools, left) >= 1 && available(actor, bonusPools, right) >= 1;
    }

    private static long available(PlayerKnowledge actor, Map<String, Integer> bonus, String tag) {
        int raw = actor.getAspectPoolFor(tag), queried = ResearchNoteEditing.queryPool(raw);
        // A wrapped negative raw pool cannot actually pay the original RP-first
        // branch, even if its signed-short query happens to look positive.
        if (queried > 0 && raw <= 0) return 0;
        return Math.max(0, queried) + (long) bonus.getOrDefault(tag, 0);
    }

    private static boolean debit(PlayerKnowledge actor, Map<String, Integer> bonus, String tag) {
        if (ResearchNoteEditing.queryPool(actor.getAspectPoolFor(tag)) > 0) {
            return actor.addAspectPool(tag, -1);
        }
        int amount = bonus.getOrDefault(tag, 0);
        if (amount <= 0) return false;
        if (amount == 1) bonus.remove(tag);
        else bonus.put(tag, amount - 1);
        return true;
    }
}
