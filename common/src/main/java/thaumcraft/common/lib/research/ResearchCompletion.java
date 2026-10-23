package thaumcraft.common.lib.research;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Function;

/** Shared original completion path for purchases and learned notes, not normal book use. */
final class ResearchCompletion {
    private ResearchCompletion() {}

    /** Mutates the caller's staged state; callers commit only after this pass succeeds. */
    static Set<String> apply(PlayerKnowledge staged, ResearchDefinition main,
                             Function<String, ResearchDefinition> resolver) {
        var unlocked = new LinkedHashSet<String>();
        complete(staged, main, unlocked);
        // Original PacketPlayerCompleteToServer:92 and ItemResearchNotes:95:
        // direct ordered siblings, their own prerequisites, no recursion/costs.
        for (String key : main.siblings()) {
            var sibling = resolver.apply(key);
            if (sibling != null && sibling.isEnabled()
                    && sibling.hasPrerequisites(staged.getResearchComplete())) complete(staged, sibling, unlocked);
        }
        return Collections.unmodifiableSet(unlocked);
    }

    private static void complete(PlayerKnowledge state, ResearchDefinition research, Set<String> unlocked) {
        if (!state.addResearch(research.key())) return;
        unlocked.add(research.key());
        int warp = research.currentWarp();
        if (warp > 0 && !thaumcraft.common.lib.WarpEvents.isWussMode()) {
            // ResearchManager:510 / Thaumcraft:302; original wuss default false.
            state.addWarpPerm(warp - warp / 2);
            state.addWarpSticky(warp / 2);
            state.setWarpCounter(state.getTotalWarp());
        }
    }
}
