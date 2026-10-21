package thaumcraft.common.lib.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.junit.jupiter.api.Test;

/**
 * The R09 combination intent on the wire. It replaces the original
 * {@code PacketAspectCombinationToServer} (oracle
 * {@code thaumcraft/common/lib/network/playerdata/PacketAspectCombinationToServer.java:56-124}),
 * whose payload carried a client dimension, a client player id and a free
 * block position of the research table. The port carries the menu generation,
 * the session token, the table revision and the two aspect tags, so the
 * server resolves the table and both debits itself.
 */
class ResearchTableCombinePayloadTest {
    private static byte[] wire(ResearchTableCombinePayload payload) {
        return PayloadWire.bytes(ResearchTableCombinePayload.STREAM_CODEC, payload);
    }

    private static ResearchTableCombinePayload read(int... bytes) {
        return PayloadWire.decode(ResearchTableCombinePayload.STREAM_CODEC, bytes);
    }

    @Test void bothAspectsAndTheAdmissionWitnessRoundTrip() {
        var payload = new ResearchTableCombinePayload(9, 77L, 3L, "aer", "ignis");
        assertEquals(payload, read(PayloadWire.unsigned(wire(payload))));
    }

    @Test void theSameAspectTwiceIsStillOneIntent() {
        var payload = new ResearchTableCombinePayload(1, 1L, 0L, "aqua", "aqua");
        assertEquals(payload, read(PayloadWire.unsigned(wire(payload))));
    }

    @Test void theFrameIsLengthPrefixedAndStaysInsideItsByteBudget() {
        byte[] encoded = wire(new ResearchTableCombinePayload(255, Long.MAX_VALUE, Long.MAX_VALUE, "perditio", "ordo"));
        assertTrue(encoded.length <= ResearchTableCombinePayload.MAX_ENCODED_BYTES,
                "frame budget " + ResearchTableCombinePayload.MAX_ENCODED_BYTES + " exceeded by " + encoded.length);
        assertEquals(encoded.length - 1, encoded[0]);
    }

    @Test void theMenuGenerationSessionAndRevisionAreBounded() {
        assertThrows(IllegalArgumentException.class, () -> new ResearchTableCombinePayload(-1, 1L, 0L, "aer", "ignis"));
        assertThrows(IllegalArgumentException.class, () -> new ResearchTableCombinePayload(256, 1L, 0L, "aer", "ignis"));
        assertThrows(IllegalArgumentException.class, () -> new ResearchTableCombinePayload(1, 0L, 0L, "aer", "ignis"));
        assertThrows(IllegalArgumentException.class, () -> new ResearchTableCombinePayload(1, 1L, -1L, "aer", "ignis"));
    }

    @Test void onlyCanonicalAspectTagsMayBeCombined() {
        assertThrows(IllegalArgumentException.class, () -> new ResearchTableCombinePayload(1, 1L, 0L, null, "ignis"));
        assertThrows(IllegalArgumentException.class, () -> new ResearchTableCombinePayload(1, 1L, 0L, "aer", null));
        assertThrows(IllegalArgumentException.class, () -> new ResearchTableCombinePayload(1, 1L, 0L, "", "ignis"));
        assertThrows(IllegalArgumentException.class, () -> new ResearchTableCombinePayload(1, 1L, 0L, "not-an-aspect", "ignis"));
        assertThrows(IllegalArgumentException.class, () -> new ResearchTableCombinePayload(1, 1L, 0L, "AER", "ignis"));
    }

    @Test void aTruncatedOversizedOrTrailingFrameIsRefused() {
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        try {
            ResearchTableCombinePayload.STREAM_CODEC.encode(buffer, new ResearchTableCombinePayload(1, 2L, 3L, "aer", "ignis"));
            buffer.writerIndex(buffer.writerIndex() - 1);
            assertThrows(DecoderException.class, () -> ResearchTableCombinePayload.STREAM_CODEC.decode(buffer));
            buffer.clear();
            ResearchTableCombinePayload.STREAM_CODEC.encode(buffer, new ResearchTableCombinePayload(1, 2L, 3L, "aer", "ignis"));
            buffer.writeByte(0);
            assertThrows(DecoderException.class, () -> ResearchTableCombinePayload.STREAM_CODEC.decode(buffer));
            buffer.clear();
            buffer.writeVarInt(ResearchTableCombinePayload.MAX_ENCODED_BYTES + 1);
            assertThrows(DecoderException.class, () -> ResearchTableCombinePayload.STREAM_CODEC.decode(buffer));
            buffer.clear();
            buffer.writeVarInt(0);
            assertThrows(DecoderException.class, () -> ResearchTableCombinePayload.STREAM_CODEC.decode(buffer));
        } finally {
            buffer.release();
        }
    }
}
