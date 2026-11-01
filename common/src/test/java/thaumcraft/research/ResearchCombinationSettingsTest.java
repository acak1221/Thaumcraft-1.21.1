package thaumcraft.research;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import thaumcraft.common.config.ResearchSettings;
import thaumcraft.common.config.ThaumcraftSettings;
import thaumcraft.common.lib.research.PlayerKnowledge;
import thaumcraft.common.lib.research.ResearchAspectCombination;

class ResearchCombinationSettingsTest {
    @Test void firstDiscoveryUsesTheConfiguredSignedCapWithoutChangingTheInputPrice() {
        var before = ThaumcraftSettings.server();
        try {
            for (int cap : new int[]{-7, 0, 100, 250}) {
                var properties = new Properties(); properties.setProperty("aspect_total_cap", Integer.toString(cap));
                ThaumcraftSettings.configureServer(ResearchSettings.read(properties));
                var actor = new PlayerKnowledge(); actor.setAspectPool("aer", 2); actor.setAspectPool("ignis", 2);
                var plan = ResearchAspectCombination.plan(actor, Map.of(), "aer", "ignis");
                assertNotNull(plan);
                assertEquals(cap <= 0 ? 1 : 3, plan.award(), "configured cap " + cap);
                assertEquals(plan.award(), plan.knowledge().getAspectPoolFor("lux"));
                assertEquals(1, plan.knowledge().getAspectPoolFor("aer"));
                assertEquals(1, plan.knowledge().getAspectPoolFor("ignis"));
                assertEquals(2, actor.getAspectPoolFor("aer"), "planning does not spend the actor's live points");
            }
        } finally { ThaumcraftSettings.configureServer(before); }
    }
}
