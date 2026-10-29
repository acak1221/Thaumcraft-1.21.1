package thaumcraft.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import thaumcraft.common.config.Config;
import thaumcraft.common.config.ResearchSettings;
import thaumcraft.common.config.ThaumcraftSettings;

/**
 * {@link ThaumcraftApi#isMirrorsAllowed()} and {@link ThaumcraftApi#isWardedStoneAllowed()} are the two server switches
 * of the original {@code Config}, read from the same settings object as the gameplay. Off the client thread they answer
 * this installation's own server value; on the client thread they answer the value the connected server sent, which the
 * last test here drives through the facade with a client thread of its own.
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

    /**
     * The distinguishing requirement of the two methods: on a client they answer the SYNCED value of the connected server,
     * not the local configuration. {@code ThaumcraftSettings} knows its client thread by a static field that a thread of this
     * test has to claim, so the field is cleared for the test and put back afterwards (another test class may have set it).
     */
    @Test void onTheClientThreadTheFacadeAnswersTheValueTheServerSent() throws Exception {
        Field clientThread = ThaumcraftSettings.class.getDeclaredField("clientThread");
        clientThread.setAccessible(true);
        Object previousClientThread = clientThread.get(null);
        Field remote = ThaumcraftSettings.class.getDeclaredField("remote");
        remote.setAccessible(true);
        Object previousRemote = remote.get(null);
        ResearchSettings before = ThaumcraftSettings.server();
        ResearchSettings local = before.withFeatures(true, true);
        ResearchSettings sent = before.withFeatures(false, false);
        AtomicReference<Throwable> problem = new AtomicReference<>();
        clientThread.set(null, null);
        try {
            ThaumcraftSettings.configureServer(local);
            Thread client = new Thread(() -> {
                try {
                    ThaumcraftSettings.initializeClient();
                    assertTrue(ThaumcraftApi.isMirrorsAllowed() && ThaumcraftApi.isWardedStoneAllowed(), "before the server sent anything: the local value");
                    ThaumcraftSettings.receive(sent);
                    assertFalse(ThaumcraftApi.isMirrorsAllowed(), "on the client: the mirrors value the server sent");
                    assertFalse(ThaumcraftApi.isWardedStoneAllowed(), "on the client: the warded stone value the server sent");
                    ThaumcraftSettings.receive(sent.withFeatures(true, false));
                    assertFalse(ThaumcraftApi.isMirrorsAllowed(), "each switch follows its own field (mirrors off, warded stone on)");
                    assertTrue(ThaumcraftApi.isWardedStoneAllowed());
                    ThaumcraftSettings.resetClient();
                    assertTrue(ThaumcraftApi.isMirrorsAllowed() && ThaumcraftApi.isWardedStoneAllowed(), "after a disconnect: the local value again");
                    ThaumcraftSettings.receive(sent);
                } catch (Throwable failure) {
                    problem.set(failure);
                }
            }, "settings-client-test-api");
            client.start();
            client.join(5000);
            assertFalse(client.isAlive());
            if (problem.get() != null) throw new AssertionError(problem.get());
            // Back on this thread, which is not the client thread: the server value, whatever the client was sent.
            assertTrue(ThaumcraftApi.isMirrorsAllowed() && ThaumcraftApi.isWardedStoneAllowed(), "off the client thread the server value stays");
        } finally {
            ThaumcraftSettings.configureServer(before);
            clientThread.set(null, previousClientThread);
            remote.set(null, previousRemote);
        }
    }
}
