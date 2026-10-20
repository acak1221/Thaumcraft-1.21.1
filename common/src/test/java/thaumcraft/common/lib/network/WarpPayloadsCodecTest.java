package thaumcraft.common.lib.network;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/** The two warp notifications the server sends a client: the message (amount and kind) and the visual event. */
class WarpPayloadsCodecTest {
    @Test void aWarpMessageIsAFourByteSignedAmountThenAKindByte() {
        assertArrayEquals(new int[] {0, 0, 0, 5, 1}, PayloadWire.unsigned(PayloadWire.bytes(WarpMessagePayload.STREAM_CODEC, new WarpMessagePayload(5, (byte) 1))));
        assertArrayEquals(new int[] {0xFF, 0xFF, 0xFF, 0xFD, 0xFF}, PayloadWire.unsigned(PayloadWire.bytes(WarpMessagePayload.STREAM_CODEC, new WarpMessagePayload(-3, (byte) -1))));
    }

    @Test void aWarpMessageKeepsEveryAmountAndKindThroughTheWire() {
        for (int delta : new int[] {Integer.MIN_VALUE, -1000, -1, 0, 1, 1000, Integer.MAX_VALUE}) {
            for (byte kind : new byte[] {Byte.MIN_VALUE, -1, 0, 1, 2, Byte.MAX_VALUE}) {
                WarpMessagePayload payload = new WarpMessagePayload(delta, kind);
                assertEquals(payload, PayloadWire.decode(WarpMessagePayload.STREAM_CODEC, PayloadWire.unsigned(PayloadWire.bytes(WarpMessagePayload.STREAM_CODEC, payload))));
            }
        }
    }

    @Test void aTruncatedWarpMessageIsNeverHalfDecoded() {
        assertThrows(IndexOutOfBoundsException.class, () -> PayloadWire.decode(WarpMessagePayload.STREAM_CODEC, 0, 0, 0, 5));
        assertThrows(IndexOutOfBoundsException.class, () -> PayloadWire.decode(WarpMessagePayload.STREAM_CODEC));
    }

    @Test void aWarpEventIsATwoByteShortOfTheThreeKnownEvents() {
        for (int event = 0; event <= 2; event++) {
            WarpEventPayload payload = new WarpEventPayload(event);
            byte[] wire = PayloadWire.bytes(WarpEventPayload.STREAM_CODEC, payload);
            assertArrayEquals(new int[] {0, event}, PayloadWire.unsigned(wire));
            assertEquals(payload, PayloadWire.decode(WarpEventPayload.STREAM_CODEC, PayloadWire.unsigned(wire)));
        }
    }

    @Test void anUnknownWarpEventCannotBeBuiltOrDecoded() {
        for (int event : new int[] {-1, 3, 4, 100, Integer.MAX_VALUE}) assertThrows(IllegalArgumentException.class, () -> new WarpEventPayload(event), "event " + event);
        assertThrows(IllegalArgumentException.class, () -> PayloadWire.decode(WarpEventPayload.STREAM_CODEC, 0, 3));
        assertThrows(IllegalArgumentException.class, () -> PayloadWire.decode(WarpEventPayload.STREAM_CODEC, 0xFF, 0xFF), "-1 as a short");
        assertThrows(IndexOutOfBoundsException.class, () -> PayloadWire.decode(WarpEventPayload.STREAM_CODEC, 0));
    }
}
