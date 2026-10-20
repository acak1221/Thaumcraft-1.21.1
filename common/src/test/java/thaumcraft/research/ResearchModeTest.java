package thaumcraft.common.lib.research;

import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumcraft.common.config.ResearchSettings;
import thaumcraft.common.config.ThaumcraftSettings;
import static org.junit.jupiter.api.Assertions.*;

class ResearchModeTest {
    @BeforeAll static void load() { ResearchCatalog.loadBundled(); }
    private static PlayerKnowledge funded(String key) {
        var knowledge = new PlayerKnowledge();
        var research = ResearchCatalog.get(key);
        for (String parent : research.parents()) knowledge.addResearch(parent);
        for (String parent : research.parentsHidden()) knowledge.addResearch(parent);
        knowledge.addResearch("@" + key);
        research.aspectCost().forEach((aspect, count) -> knowledge.setAspectPool(aspect, 100));
        return knowledge;
    }
    @Test void easyPurchasesPrimaryAndHardIssuesSecondaryNotesThroughTheActualPolicies() {
        var before = ThaumcraftSettings.server();
        try {
            for (int mode : new int[]{-1, 0, 1}) for (String key : List.of("FOCUSFIRE", "ENCHFABRIC")) {
                var settings = new ResearchSettings(false, true, true, true, false, mode, 100);
                ThaumcraftSettings.configureServer(settings);
                var actor = funded(key);
                var research = ResearchCatalog.get(key);
                boolean purchased = settings.purchases(research.isSecondary());
                var preview = ResearchPurchase.preview(actor, key);
                assertEquals(purchased, preview.success(), mode + ":" + key);
                assertEquals(purchased ? ResearchNoteIssuance.Failure.SECONDARY_REQUIRES_PURCHASE : ResearchNoteIssuance.Failure.NONE,
                        ResearchNoteIssuance.validate(actor, key));
                if (purchased) {
                    assertTrue(ResearchPurchase.purchase(actor, key).success());
                    assertTrue(actor.isResearchComplete(key));
                    research.aspectCost().forEach((aspect, count) -> assertEquals(100 - count, actor.getAspectPoolFor(aspect)));
                }
            }
        } finally { ThaumcraftSettings.configureServer(before); }
    }
    @Test void wussSuppressesResearchWarpWhileStillCompletingTheActualResearch() {
        var before = ThaumcraftSettings.server();
        try {
            ThaumcraftSettings.configureServer(new ResearchSettings(false, true, true, true, true, -1, 100));
            var actor = funded("FOCUSHELLBAT");
            int permanent = actor.getWarpPerm(), sticky = actor.getWarpSticky();
            assertTrue(ResearchPurchase.purchase(actor, "FOCUSHELLBAT").success());
            assertTrue(actor.isResearchComplete("FOCUSHELLBAT"));
            assertEquals(permanent, actor.getWarpPerm()); assertEquals(sticky, actor.getWarpSticky());
        } finally { ThaumcraftSettings.configureServer(before); }
    }
}
