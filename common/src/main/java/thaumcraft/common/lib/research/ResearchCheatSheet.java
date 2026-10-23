package thaumcraft.common.lib.research;

import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.config.ResearchSettings;

/** Original ItemThaumonomicon metadata42 branch and ResearchManager.completeResearch/completeAspect.
 * All research is admitted without prerequisites. Already known RP and research warp are never topped up. */
public final class ResearchCheatSheet {
    private ResearchCheatSheet() {}
    public static boolean grant(PlayerKnowledge knowledge, ResearchSettings settings) {
        if (!settings.cheatSheet()) return false;
        PlayerKnowledge staged = knowledge.copy(); boolean changed = false;
        for (ResearchDefinition research : ResearchRegistrationOrder.enabled()) {
            if (!staged.addResearch(research.key())) continue;
            changed = true;
            int warp = research.currentWarp();
            // Original completeResearch has NO sibling pass and awards permanent ceil(warp/2), sticky floor(warp/2).
            if (warp > 0 && !settings.wuss()) {
                staged.addWarpPerm(warp - warp / 2); staged.addWarpSticky(warp / 2);
                staged.setWarpCounter(staged.getTotalWarp());
            }
        }
        for (Aspect aspect : Aspect.aspects.values()) {
            if (staged.hasDiscoveredAspect(aspect.getTag())) continue;
            staged.setAspectPool(aspect.getTag(), 50); changed = true;
        }
        if (changed) knowledge.copyFrom(staged);
        return changed;
    }
}
