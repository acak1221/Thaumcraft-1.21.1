package thaumcraft.research;

import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import thaumcraft.common.config.ResearchSettings;
import thaumcraft.common.config.ThaumcraftSettings;
import static org.junit.jupiter.api.Assertions.*;

class ResearchSettingsTest {
    @Test void originalDefaultsAndAllThreeResearchModesHaveDifferentPurchasePolicies() {
        assertEquals(new ResearchSettings(false, true, true, true, false, 0, 100), ResearchSettings.read(new Properties()));
        for (int mode : new int[]{-2, -1, 0, 1, 2, 256}) {
            var settings = new ResearchSettings(false, true, true, true, false, mode, 100);
            assertEquals(mode == -1, settings.purchases(false));
            assertEquals(mode == -1 || mode == 0, settings.purchases(true));
        }
    }
    @Test void originalIntegerOptionsRemainSignedAndMalformedValuesUseOriginalFallbacks() {
        Properties values = new Properties();
        values.setProperty("research_difficulty", " -1 "); values.setProperty("aspect_total_cap", "-7");
        values.setProperty("wuss_mode", "TRUE"); values.setProperty("allow_mirrors", "false");
        assertEquals(new ResearchSettings(false, true, false, true, true, -1, -7), ResearchSettings.read(values));
        values.setProperty("research_difficulty", "1.0"); values.setProperty("aspect_total_cap", "2147483648"); values.setProperty("wuss_mode", "anything");
        assertEquals(8, ResearchSettings.read(values).difficulty()); assertEquals(100, ResearchSettings.read(values).aspectCap());
        assertFalse(ResearchSettings.read(values).purchases(false));
        assertFalse(ResearchSettings.read(values).purchases(true)); // Config:390 getInt(8) follows the original note path.
        assertFalse(ResearchSettings.read(values).wuss());
        values.setProperty("allow_warded_stone", "anything");
        assertFalse(ResearchSettings.read(values).wardedStone());
    }
    @Test void remoteServerSettingsStayOnTheClientThreadAndResetWithoutChangingIntegratedServerFacts() throws Exception {
        var before = ThaumcraftSettings.server();
        var authoritative = new ResearchSettings(true, false, false, false, true, 1, 37);
        var remote = new ResearchSettings(false, true, true, true, false, -1, 5);
        var problem = new AtomicReference<Throwable>();
        ThaumcraftSettings.configureServer(authoritative);
        try {
            Thread client = new Thread(() -> {
                try {
                    ThaumcraftSettings.initializeClient(); assertEquals(authoritative, ThaumcraftSettings.current());
                    ThaumcraftSettings.receive(remote); assertEquals(remote, ThaumcraftSettings.current());
                    assertEquals(authoritative, ThaumcraftSettings.server());
                    ThaumcraftSettings.resetClient(); assertEquals(authoritative, ThaumcraftSettings.current());
                    assertEquals(authoritative, ThaumcraftSettings.server());
                } catch (Throwable failure) { problem.set(failure); }
            }, "settings-client-test");
            client.start(); client.join(5000);
            assertFalse(client.isAlive()); if (problem.get() != null) throw new AssertionError(problem.get());
            assertEquals(authoritative, ThaumcraftSettings.current());
            assertThrows(IllegalStateException.class, () -> ThaumcraftSettings.receive(remote));
            assertEquals(authoritative, ThaumcraftSettings.server());
        } finally { ThaumcraftSettings.configureServer(before); }
    }
}
