package thaumcraft.common.lib.research;

import java.util.ArrayDeque;
import java.util.List;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.lib.WarpEvents;

/** Original CommandThaumcraft's privileged state changes; callers authorize and sync the target. */
public final class ResearchAdministration {
    private ResearchAdministration() {}

    /** An already complete key stops recursion, even when its parents are missing. */
    public static boolean grant(PlayerKnowledge state, String key) {
        ResearchDefinition first = ResearchCatalog.get(key);
        if (first == null || !first.isEnabled()) return false;
        var pending = new ArrayDeque<String>();
        pending.push(key);
        while (!pending.isEmpty()) {
            var definition = ResearchCatalog.get(pending.pop());
            if (definition == null || !definition.isEnabled() || !complete(state, definition)) continue;
            pushReverse(pending, definition.siblings());
            pushReverse(pending, definition.parentsHidden());
            pushReverse(pending, definition.parents());
        }
        return true;
    }

    public static void grantAll(PlayerKnowledge state) {
        for (var definition : ResearchRegistrationOrder.enabled()) complete(state, definition);
    }

    /** Reset research and clues only: discovered aspects, scans and existing warp survive. */
    public static void resetResearch(PlayerKnowledge state) {
        for (String key : state.getResearchComplete()) state.removeResearch(key);
        for (var definition : ResearchRegistrationOrder.enabled()) {
            if (definition.isAutoUnlock()) complete(state, definition);
        }
    }

    /** The old command parses a positive int, then narrows it to a signed short. */
    public static void giveAspect(PlayerKnowledge state, Aspect aspect, int amount) {
        if (amount < 1) throw new IllegalArgumentException("Aspect command amount must be positive");
        if (aspect == null || Aspect.getAspect(aspect.getTag()) != aspect) throw new IllegalArgumentException("Unknown aspect");
        state.addAspectPool(aspect.getTag(), (short) amount);
    }

    /** Command set/add calls PlayerKnowledge directly and deliberately does not reset the event counter. */
    public static void warp(PlayerKnowledge state, boolean add, int amount, String type) {
        if (add ? amount < -100 || amount > 100 : amount < 0) throw new IllegalArgumentException("Warp amount out of range");
        if ("PERM".equalsIgnoreCase(type)) {
            if (add) state.addWarpPerm(amount); else state.setWarpPerm(amount);
        } else if ("TEMP".equalsIgnoreCase(type)) {
            if (add) state.addWarpTemp(amount); else state.setWarpTemp(amount);
        } else {
            if (add) state.addWarpSticky(amount); else state.setWarpSticky(amount);
        }
    }

    private static boolean complete(PlayerKnowledge state, ResearchDefinition definition) {
        if (!state.addResearch(definition.key())) return false;
        int warp = definition.currentWarp();
        if (warp > 0 && !WarpEvents.isWussMode()) {
            state.addWarpPerm(warp - warp / 2);
            state.addWarpSticky(warp / 2);
            state.setWarpCounter(state.getTotalWarp());
        }
        return true;
    }

    private static void pushReverse(ArrayDeque<String> stack, List<String> keys) {
        for (int i = keys.size() - 1; i >= 0; i--) stack.push(keys.get(i));
    }
}
