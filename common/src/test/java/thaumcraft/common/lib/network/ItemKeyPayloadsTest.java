package thaumcraft.common.lib.network;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.netty.handler.codec.DecoderException;
import net.minecraft.world.InteractionHand;
import org.junit.jupiter.api.Test;

/**
 * The original carried every item action of the misc key in one byte
 * (oracle {@code thaumcraft/common/lib/network/misc/PacketItemKeyToServer.java:60-79}:
 * key 0 the golem bell, key 1 the wand toggle or the shovel orientation) plus
 * a client dimension and a client player id. The port splits those actions
 * into three channels that carry no world or player address at all, so the
 * server always resolves the held item itself.
 */
class ItemKeyPayloadsTest {
    @Test void theHoverIntentIsAnEmptyPacket() {
        assertEquals(0, PayloadWire.bytes(HoverTogglePayload.STREAM_CODEC, new HoverTogglePayload()).length);
        assertEquals(new HoverTogglePayload(), PayloadWire.decode(HoverTogglePayload.STREAM_CODEC));
    }

    @Test void theHoverIntentRefusesAnyTrailingByte() {
        assertThrows(DecoderException.class, () -> PayloadWire.decode(HoverTogglePayload.STREAM_CODEC, 0));
        assertThrows(DecoderException.class, () -> PayloadWire.decode(HoverTogglePayload.STREAM_CODEC, 1, 2, 3));
    }

    @Test void theShovelIntentIsAnEmptyPacket() {
        assertEquals(0, PayloadWire.bytes(ShovelOrientationPayload.STREAM_CODEC, new ShovelOrientationPayload()).length);
        assertEquals(new ShovelOrientationPayload(), PayloadWire.decode(ShovelOrientationPayload.STREAM_CODEC));
    }

    @Test void theShovelIntentRefusesAnyTrailingByte() {
        assertThrows(DecoderException.class, () -> PayloadWire.decode(ShovelOrientationPayload.STREAM_CODEC, 0));
    }

    @Test void theBellResetCarriesOnlyTheHandThatHoldsTheBell() {
        assertArrayEquals(new int[] {0}, PayloadWire.unsigned(PayloadWire.bytes(GolemBellResetPayload.STREAM_CODEC,
                new GolemBellResetPayload(InteractionHand.MAIN_HAND))));
        assertArrayEquals(new int[] {1}, PayloadWire.unsigned(PayloadWire.bytes(GolemBellResetPayload.STREAM_CODEC,
                new GolemBellResetPayload(InteractionHand.OFF_HAND))));
        assertEquals(new GolemBellResetPayload(InteractionHand.MAIN_HAND),
                PayloadWire.decode(GolemBellResetPayload.STREAM_CODEC, 0));
        assertEquals(new GolemBellResetPayload(InteractionHand.OFF_HAND),
                PayloadWire.decode(GolemBellResetPayload.STREAM_CODEC, 1));
    }

    @Test void theBellResetRefusesAnyOtherLengthOrHand() {
        assertThrows(DecoderException.class, () -> PayloadWire.decode(GolemBellResetPayload.STREAM_CODEC));
        assertThrows(DecoderException.class, () -> PayloadWire.decode(GolemBellResetPayload.STREAM_CODEC, 0, 0));
        assertThrows(DecoderException.class, () -> PayloadWire.decode(GolemBellResetPayload.STREAM_CODEC, 2));
        assertThrows(DecoderException.class, () -> PayloadWire.decode(GolemBellResetPayload.STREAM_CODEC, 255));
    }
}
