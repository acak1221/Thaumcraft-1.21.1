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
import thaumcraft.common.tiles.TileResearchTable;

/**
 * The shared research-table state on the wire: the menu generation, the
 * session token, the table revision and the table's own bonus pools. It is
 * the 1.21.1 replacement for the original's unframed table updates
 * (oracle {@code thaumcraft/common/tiles/TileResearchTable.java:118-121},
 * {@code func_147471_g} plus {@code func_70296_d}).
 */
class ResearchTableStatePayloadTest {
    private static byte[] wire(ResearchTableStatePayload payload) {
        return PayloadWire.bytes(ResearchTableStatePayload.STREAM_CODEC, payload);
    }

    private static ResearchTableStatePayload read(int... bytes) {
        return PayloadWire.decode(ResearchTableStatePayload.STREAM_CODEC, bytes);
    }

    @Test void theGenerationRevisionAndBonusPoolsRoundTrip() {
        Map<String, Integer> bonus = new LinkedHashMap<>();
        bonus.put("aer", 2);
        bonus.put("ignis", 1);
        var payload = new ResearchTableStatePayload(4, 12L, 5L, bonus);
        var decoded = read(PayloadWire.unsigned(wire(payload)));
        assertEquals(payload, decoded);
        assertEquals(bonus, decoded.bonusPools());
    }

    @Test void anEmptyBonusTableIsAValidState() {
        var payload = new ResearchTableStatePayload(0, 1L, 0L, Map.of());
        assertEquals(payload, read(PayloadWire.unsigned(wire(payload))));
    }

    @Test void theBonusPoolsAreCanonicalisedAndBounded() {
        assertThrows(IllegalArgumentException.class, () -> new ResearchTableStatePayload(0, 1L, 0L, Map.of("not-an-aspect", 1)));
        assertThrows(IllegalArgumentException.class, () -> new ResearchTableStatePayload(0, 1L, 0L, Map.of("aer", 0)));
        assertThrows(IllegalArgumentException.class, () -> new ResearchTableStatePayload(0, 1L, 0L, Map.of("aer", -1)));
        assertThrows(IllegalArgumentException.class, () -> new ResearchTableStatePayload(-1, 1L, 0L, Map.of()));
        assertThrows(IllegalArgumentException.class, () -> new ResearchTableStatePayload(256, 1L, 0L, Map.of()));
        assertThrows(IllegalArgumentException.class, () -> new ResearchTableStatePayload(0, 0L, 0L, Map.of()));
        assertThrows(IllegalArgumentException.class, () -> new ResearchTableStatePayload(0, 1L, -1L, Map.of()));
    }

    @Test void theFrameIsLengthPrefixedAndStaysInsideItsByteBudget() {
        Map<String, Integer> bonus = new LinkedHashMap<>();
        bonus.put("perditio", 3);
        byte[] encoded = wire(new ResearchTableStatePayload(255, Long.MAX_VALUE, Long.MAX_VALUE, bonus));
        assertTrue(encoded.length <= ResearchTableStatePayload.MAX_ENCODED_BYTES);
        assertEquals(encoded.length - 1, encoded[0]);
    }

    @Test void aTruncatedOversizedOrTrailingFrameIsRefused() {
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        try {
            ResearchTableStatePayload.STREAM_CODEC.encode(buffer, new ResearchTableStatePayload(1, 2L, 3L, Map.of("aer", 1)));
            buffer.writerIndex(buffer.writerIndex() - 1);
            assertThrows(DecoderException.class, () -> ResearchTableStatePayload.STREAM_CODEC.decode(buffer));
            buffer.clear();
            ResearchTableStatePayload.STREAM_CODEC.encode(buffer, new ResearchTableStatePayload(1, 2L, 3L, Map.of("aer", 1)));
            buffer.writeByte(0);
            assertThrows(DecoderException.class, () -> ResearchTableStatePayload.STREAM_CODEC.decode(buffer));
            buffer.clear();
            buffer.writeVarInt(ResearchTableStatePayload.MAX_ENCODED_BYTES + 1);
            assertThrows(DecoderException.class, () -> ResearchTableStatePayload.STREAM_CODEC.decode(buffer));
            buffer.clear();
            buffer.writeVarInt(0);
            assertThrows(DecoderException.class, () -> ResearchTableStatePayload.STREAM_CODEC.decode(buffer));
        } finally {
            buffer.release();
        }
    }

    @Test void aDuplicateBonusAspectIsRefused() {
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        var body = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        try {
            body.writeVarInt(1);
            body.writeLong(2L);
            body.writeLong(3L);
            body.writeVarInt(2);
            body.writeUtf("aer", 128);
            body.writeVarInt(1);
            body.writeUtf("aer", 128);
            body.writeVarInt(1);
            buffer.writeVarInt(body.readableBytes());
            buffer.writeBytes(body);
            assertThrows(DecoderException.class, () -> ResearchTableStatePayload.STREAM_CODEC.decode(buffer));
        } finally {
            body.release();
            buffer.release();
        }
    }

    @Test void aBonusCountBeyondTheTableLimitIsRefused() {
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        var body = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        try {
            body.writeVarInt(1);
            body.writeLong(2L);
            body.writeLong(3L);
            body.writeVarInt(TileResearchTable.MAX_BONUS_ASPECTS + 1);
            buffer.writeVarInt(body.readableBytes());
            buffer.writeBytes(body);
            assertThrows(DecoderException.class, () -> ResearchTableStatePayload.STREAM_CODEC.decode(buffer));
        } finally {
            body.release();
            buffer.release();
        }
    }
}
