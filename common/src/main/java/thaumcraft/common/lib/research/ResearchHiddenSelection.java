package thaumcraft.common.lib.research;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Original findHiddenResearch category insertion order, per-category HashMap values, and DAY-time-seeded private RNG. */
public final class ResearchHiddenSelection {
    private ResearchHiddenSelection() {}

    public static String choose(PlayerKnowledge actor, long dayTime) {
        List<String> keys = eligible(actor);
        Random privateChoice = new Random(dayTime / 10L / 5L);
        return keys.isEmpty() ? null : keys.get(privateChoice.nextInt(keys.size()));
    }
    public static List<String> eligible(PlayerKnowledge actor) {
        if (actor == null) throw new NullPointerException("actor");
        List<String> keys = new ArrayList<>();
        // The helper inserts ALL enabled research before filtering, including nonhidden/aspectless entries.
        for (ResearchDefinition research : ResearchRegistrationOrder.enabled()) {
            if (!research.isHidden() || research.aspectCost().isEmpty() || actor.isResearchComplete(research.key())
                    || !research.hasPrerequisites(actor.getResearchComplete())
                    || research.itemTriggerExpressions().isEmpty() && research.entityTriggers().isEmpty() && research.aspectTriggers().isEmpty()) continue;
            keys.add(research.key());
        }
        return List.copyOf(keys);
    }
}
