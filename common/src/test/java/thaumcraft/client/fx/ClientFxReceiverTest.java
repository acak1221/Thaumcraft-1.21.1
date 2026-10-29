package thaumcraft.client.fx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import thaumcraft.common.lib.network.ClientFxPayload;

/**
 * The client half of the server twins of {@code ThaumcraftFX} without a client: which slot of a {@link ClientFxPayload} becomes
 * which argument of the facade call it stands for. Every number below is different, so a payload slot that lands in the wrong
 * argument (gravity taken from {@code d} instead of {@code b}, the radius and the start swapped...) shows as a failure.
 *
 * <p>That the sink really is {@code ThaumcraftClientFX} with the client level is the four lines of the private
 * {@code FacadeSink}; what the facade then draws needs a client and is not tested here.</p>
 */
class ClientFxReceiverTest {
    private static final ResourceLocation DIMENSION = ResourceLocation.fromNamespaceAndPath("minecraft", "overworld");

    /** Records the calls as text, so one assertion states the whole call, name and arguments in order. */
    private static final class Recorder implements ClientFxReceiver.Sink {
        final List<String> calls = new ArrayList<>();

        @Override public void sparkle(double x, double y, double z, float size, int type, float gravity) {
            calls.add("sparkle(" + x + ", " + y + ", " + z + ", size " + size + ", type " + type + ", gravity " + gravity + ")");
        }

        @Override public void burst(double x, double y, double z, float size) {
            calls.add("burst(" + x + ", " + y + ", " + z + ", size " + size + ")");
        }

        @Override public void blockRunes(double x, double y, double z, float red, float green, float blue, int duration, float gravity) {
            calls.add("blockRunes(" + x + ", " + y + ", " + z + ", red " + red + ", green " + green + ", blue " + blue
                    + ", duration " + duration + ", gravity " + gravity + ")");
        }

        @Override public void smokeSpiral(double x, double y, double z, float radius, int start, int minY, int rgb) {
            calls.add("smokeSpiral(" + x + ", " + y + ", " + z + ", radius " + radius + ", start " + start + ", minY " + minY + ", rgb " + rgb + ")");
        }
    }

    private static List<String> heard(ClientFxPayload payload) {
        Recorder recorder = new Recorder();
        ClientFxReceiver.dispatch(payload, recorder);
        return recorder.calls;
    }

    @Test void aSparkleLandsInTheSparkleCallWithItsSizeTypeAndGravityInTheirOwnArguments() {
        // size 1.5 (a), gravity -0.25 (b), type 4 (i): the slots are not in the order of the call.
        assertEquals(List.of("sparkle(12.5, 64.25, -3.75, size 1.5, type 4, gravity -0.25)"),
                heard(ClientFxPayload.sparkle(DIMENSION, 12.5, 64.25, -3.75, 1.5F, 4, -0.25F)));
    }

    @Test void aBurstLandsInTheBurstCall() {
        assertEquals(List.of("burst(1.0, 2.0, 3.0, size 0.5)"), heard(ClientFxPayload.burst(DIMENSION, 1, 2, 3, 0.5F)));
    }

    @Test void runesLandInTheRuneCallWithTheColourTheDurationAndTheGravityInTheirOwnArguments() {
        // red 0.25 (a), green 0.5 (b), blue 0.75 (c), gravity -0.125 (d), duration 20 (i): five different slots.
        assertEquals(List.of("blockRunes(10.0, 70.5, -10.0, red 0.25, green 0.5, blue 0.75, duration 20, gravity -0.125)"),
                heard(ClientFxPayload.runes(DIMENSION, 10, 70.5, -10, 0.25F, 0.5F, 0.75F, 20, -0.125F)));
    }

    @Test void aSmokeSpiralLandsInTheSpiralCallWithTheRadiusTheStartTheFloorAndTheColourInTheirOwnArguments() {
        // radius 2.5 (a), start 90 (i), minY 60 (j), colour (k): the three ints are adjacent in the payload and in the call.
        assertEquals(List.of("smokeSpiral(1.5, 64.0, 2.5, radius 2.5, start 90, minY 60, rgb " + 0x09E2FF + ")"),
                heard(ClientFxPayload.smokeSpiral(DIMENSION, 1.5, 64, 2.5, 2.5F, 90, 60, 0xFF09E2FF)));
    }

    @Test void eachPayloadMakesExactlyOneCallAndTheSlotsOfOtherKindsAreNotRead() {
        // A sparkle carries gravity in b; a payload of another kind with the same numbers in other slots must not reach it.
        for (ClientFxPayload payload : List.of(
                ClientFxPayload.sparkle(DIMENSION, 1, 2, 3, 1.0F, 2, 0.0F),
                ClientFxPayload.burst(DIMENSION, 1, 2, 3, 1.0F),
                ClientFxPayload.runes(DIMENSION, 1, 2, 3, 0.1F, 0.2F, 0.3F, 5, 0.0F),
                ClientFxPayload.smokeSpiral(DIMENSION, 1, 2, 3, 4.0F, 5, 6, 7))) {
            assertEquals(1, heard(payload).size(), "one payload, one call: " + payload);
        }
        assertTrue(heard(ClientFxPayload.burst(DIMENSION, 1, 2, 3, 1.0F)).get(0).startsWith("burst("));
        assertTrue(heard(ClientFxPayload.smokeSpiral(DIMENSION, 1, 2, 3, 4.0F, 5, 6, 7)).get(0).startsWith("smokeSpiral("));
    }
}
