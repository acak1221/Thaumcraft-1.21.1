package thaumcraft.common.lib.network;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.netty.handler.codec.DecoderException;
import net.minecraft.world.InteractionHand;
import org.junit.jupiter.api.Test;
import thaumcraft.common.items.wands.FocusSelectionSources;

/** The focus-selection intent: a hand and a physical address, in the one canonical wire form per address and nothing else. */
class FocusSelectionPayloadTest {
    private static final InteractionHand MAIN = InteractionHand.MAIN_HAND;
    private static final InteractionHand OFF = InteractionHand.OFF_HAND;

    private static byte[] wire(FocusSelectionPayload payload) { return PayloadWire.bytes(FocusSelectionPayload.STREAM_CODEC, payload); }

    private static FocusSelectionPayload read(int... bytes) { return PayloadWire.decode(FocusSelectionPayload.STREAM_CODEC, bytes); }

    @Test void anAddressInTheInventoryRangeTakesTwoBytesHandThenSignedSlot() {
        assertArrayEquals(new int[] {0, 5}, PayloadWire.unsigned(wire(new FocusSelectionPayload(MAIN, 5))));
        assertArrayEquals(new int[] {1, 0}, PayloadWire.unsigned(wire(new FocusSelectionPayload(OFF, 0))));
        assertArrayEquals(new int[] {0, 35}, PayloadWire.unsigned(wire(new FocusSelectionPayload(MAIN, 35))));
        assertArrayEquals(new int[] {0, 0xFF}, PayloadWire.unsigned(wire(new FocusSelectionPayload(MAIN, -1))), "-1 means the held focus itself");
    }

    @Test void aPouchAddressTakesFourBytesWithTheMarkerThenAnUnsignedShort() {
        assertArrayEquals(new int[] {0, 127, 0x03, 0xE8}, PayloadWire.unsigned(wire(new FocusSelectionPayload(MAIN, 1000))));
        assertArrayEquals(new int[] {1, 127, 0x06, 0x6F}, PayloadWire.unsigned(wire(new FocusSelectionPayload(OFF, 1647))));
        assertArrayEquals(new int[] {0, 127, 0x07, 0xD0}, PayloadWire.unsigned(wire(new FocusSelectionPayload(MAIN, 2000))));
        assertArrayEquals(new int[] {0, 127, 0x08, 0x17}, PayloadWire.unsigned(wire(new FocusSelectionPayload(MAIN, 2071))));
    }

    @Test void anAddressInTheBandsOfTheExtraCellsOfALargerPouchTakesTheSameFourBytes() {
        // Cells 18.. of an add-on pouch (IFocusPouch): main slot 0 cell 18, main slot 35 cell 127, worn slot 0 cell 18, worn slot 3 cell 127.
        assertArrayEquals(new int[] {0, 127, 0x0B, 0xB8}, PayloadWire.unsigned(wire(new FocusSelectionPayload(MAIN, 3000))));
        assertArrayEquals(new int[] {1, 127, 0x1B, 0x2F}, PayloadWire.unsigned(wire(new FocusSelectionPayload(OFF, 6959))));
        assertArrayEquals(new int[] {0, 127, 0x1B, 0x30}, PayloadWire.unsigned(wire(new FocusSelectionPayload(MAIN, 6960))));
        assertArrayEquals(new int[] {1, 127, 0x1C, 0xE7}, PayloadWire.unsigned(wire(new FocusSelectionPayload(OFF, 7399))));
        for (int address : new int[] {3000, 3109, 3110, 6959, 6960, 7399}) {
            for (InteractionHand hand : InteractionHand.values()) {
                FocusSelectionPayload payload = new FocusSelectionPayload(hand, address);
                assertEquals(payload, read(PayloadWire.unsigned(wire(payload))), hand + " " + address);
            }
        }
    }

    @Test void theAddressesAroundTheExtraBandsAreRefusedOnTheWireAndInTheConstructor() {
        for (int[] gap : new int[][] {{0x0B, 0xB7}, {0x1C, 0xE8}, {0xFF, 0xFF}}) {
            assertThrows(DecoderException.class, () -> read(0, 127, gap[0], gap[1]), gap[0] + ":" + gap[1]);
        }
        for (int slot : new int[] {2999, 7400, 65535}) assertThrows(IllegalArgumentException.class, () -> new FocusSelectionPayload(MAIN, slot), "slot " + slot);
    }

    @Test void everyValidAddressRoundTripsWithBothHands() {
        for (int slot = -1; slot < 2100; slot++) {
            if (!FocusSelectionSources.valid(slot)) continue;
            for (InteractionHand hand : InteractionHand.values()) {
                FocusSelectionPayload payload = new FocusSelectionPayload(hand, slot);
                assertEquals(payload, read(PayloadWire.unsigned(wire(payload))), hand + " " + slot);
            }
        }
    }

    @Test void thereIsNoWayToBuildAnAddressOutsideTheThreeRanges() {
        for (int slot : new int[] {-2, 36, 127, 999, 1648, 1999, 2072, Integer.MIN_VALUE, Integer.MAX_VALUE}) {
            assertThrows(IllegalArgumentException.class, () -> new FocusSelectionPayload(MAIN, slot), "slot " + slot);
        }
        assertThrows(NullPointerException.class, () -> new FocusSelectionPayload(null, 0));
    }

    @Test void aMalformedPayloadIsRefusedWithADecoderException() {
        assertThrows(DecoderException.class, () -> read(), "empty");
        assertThrows(DecoderException.class, () -> read(0), "hand only");
        assertThrows(DecoderException.class, () -> read(0, 5, 0), "three bytes");
        assertThrows(DecoderException.class, () -> read(2, 5), "no such hand");
        assertThrows(DecoderException.class, () -> read(0, 36), "a one-byte address past the inventory");
        assertThrows(DecoderException.class, () -> read(0, 0xFE), "-2");
        assertThrows(DecoderException.class, () -> read(0, 5, 1, 1), "four bytes without the marker are trailing bytes");
        assertThrows(DecoderException.class, () -> read(0, 127), "the marker without its short");
    }

    @Test void aLongFormForAnAddressThatHasAShortFormIsNotCanonicalAndIsRefused() {
        assertThrows(DecoderException.class, () -> read(0, 127, 0, 5), "slot 5 written as a short");
        assertThrows(DecoderException.class, () -> read(0, 127, 0, 35), "slot 35 written as a short");
        assertThrows(DecoderException.class, () -> read(0, 127, 0, 36), "36 is no address at all");
    }

    @Test void theShortFormAddressesBetweenTheRangesAreRefused() {
        for (int[] gap : new int[][] {{0x00, 0x24}, {0x03, 0xE7}, {0x06, 0x70}, {0x07, 0xCF}, {0x08, 0x18}, {0xFF, 0xFF}}) {
            assertThrows(DecoderException.class, () -> read(0, 127, gap[0], gap[1]), gap[0] + ":" + gap[1]);
        }
    }
}
