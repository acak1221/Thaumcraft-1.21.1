package thaumcraft.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import thaumcraft.common.config.Config;
import thaumcraft.common.config.ResearchSettings;
import thaumcraft.common.config.ThaumcraftSettings;

/**
 * {@link ThaumcraftApi#isMirrorsAllowed()} and {@link ThaumcraftApi#isWardedStoneAllowed()} are the two server switches
 * of the original {@code Config}, read from the same settings object as the gameplay. The client-thread snapshot that
 * {@code ThaumcraftSettings.current()} answers on a client is covered by {@code ResearchSettingsTest}; this class runs on
 * a thread that is not the client thread, so it sees this installation's own server value.
 */
class ServerSwitchesApiTest {
    @Test void bothSwitchesAreOnByDefaultAsInTheOriginal() {
        assertTrue(ResearchSettings.DEFAULTS.mirrors(), "original Config.allowMirrors default");
        assertTrue(ResearchSettings.DEFAULTS.wardedStone(), "original Config.wardedStone default");
    }

    @Test void theSwitchesFollowTheServerSettingsInEveryCombination() {
        ResearchSettings before = ThaumcraftSettings.server();
        try {
            for (boolean mirrors : new boolean[] {true, false}) {
                for (boolean warded : new boolean[] {true, false}) {
                    ThaumcraftSettings.configureServer(before.withFeatures(warded, mirrors));
                    assertEquals(mirrors, ThaumcraftApi.isMirrorsAllowed(), "mirrors=" + mirrors + " warded=" + warded);
                    assertEquals(warded, ThaumcraftApi.isWardedStoneAllowed(), "mirrors=" + mirrors + " warded=" + warded);
                    // The public fields an add-on of the original read are the same facts.
                    assertEquals(mirrors, Config.allowMirrors);
                    assertEquals(warded, Config.wardedStone);
                }
            }
        } finally {
            ThaumcraftSettings.configureServer(before);
        }
        assertEquals(before.mirrors(), ThaumcraftApi.isMirrorsAllowed(), "the settings were restored");
        assertEquals(before.wardedStone(), ThaumcraftApi.isWardedStoneAllowed());
    }
}
