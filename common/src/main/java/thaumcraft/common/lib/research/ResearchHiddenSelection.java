package thaumcraft.common.lib.research;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Original findHiddenResearch category insertion order, per-category HashMap values, and WORLD-time-seeded private RNG. */
public final class ResearchHiddenSelection {
    private ResearchHiddenSelection() {}

    /**
     * The original seed: ResearchManager.findHiddenResearch:148 draws
     * {@code new Random(player.worldObj.getWorldTime()/10L/5L)}. The 1.7.10 JAR calls
     * World.func_72820_D(), which the official mcp_stable-12-1.7.10 mapping names
     * getWorldTime. The basis is the world time counter, not the time of day, so the
     * same world state always reveals the same hidden research.
     */
    public static long seed(long worldTime) { return worldTime / 10L / 5L; }

    /**
     * @param worldTime the world time counter (1.7.10 {@code World.getWorldTime()},
     *                  1.21.1 {@code Level.getGameTime()}), never the time of day.
     */
    public static String choose(PlayerKnowledge actor, long worldTime) {
        List<String> keys = eligible(actor);
        Random privateChoice = new Random(seed(worldTime));
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
