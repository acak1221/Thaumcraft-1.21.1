package thaumcraft.common.lib.network;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.junit.jupiter.api.Test;
import thaumcraft.common.lib.research.ResearchNoteData;
import thaumcraft.common.lib.research.ResearchNoteEditing;

/**
 * The R08 table-edit intent on the wire. It replaces the original
 * {@code PacketAspectPlaceToServer} (oracle
 * {@code thaumcraft/common/lib/network/playerdata/PacketAspectPlaceToServer.java:57-95}),
 * which carried a client dimension, a client player id and a free block
 * position. The port carries a menu generation, a session token, the table
 * revision, the action and the hex cell, so the server can admit the edit
 * against the live table instead of a client address.
 */
class ResearchTableEditPayloadTest {
    private static final int RADIUS = ResearchNoteData.MAX_RADIUS;

    private static byte[] wire(ResearchTableEditPayload payload) {
        return PayloadWire.bytes(ResearchTableEditPayload.STREAM_CODEC, payload);
    }

    private static ResearchTableEditPayload read(int... bytes) {
        return PayloadWire.decode(ResearchTableEditPayload.STREAM_CODEC, bytes);
    }

    @Test void aPlaceCarriesTheMenuSessionRevisionActionCellAndAspect() {
        var payload = new ResearchTableEditPayload(7, 42L, 9L, ResearchNoteEditing.Action.PLACE, 1, -1, "aer");
        var decoded = read(PayloadWire.unsigned(wire(payload)));
        assertEquals(payload, decoded);
        assertEquals("aer", decoded.aspect());
    }

    @Test void anEraseCarriesNoAspectAtAll() {
        var payload = new ResearchTableEditPayload(0, 1L, 0L, ResearchNoteEditing.Action.ERASE, 0, 0, null);
        var decoded = read(PayloadWire.unsigned(wire(payload)));
        assertEquals(payload, decoded);
        assertNull(decoded.aspect());
    }

    @Test void theFrameIsLengthPrefixedAndStaysInsideItsByteBudget() {
        byte[] encoded = wire(new ResearchTableEditPayload(255, Long.MAX_VALUE, Long.MAX_VALUE,
                ResearchNoteEditing.Action.PLACE, RADIUS, 0, "perditio"));
        assertTrue(encoded.length <= ResearchTableEditPayload.MAX_ENCODED_BYTES,
                "frame budget " + ResearchTableEditPayload.MAX_ENCODED_BYTES + " exceeded by " + encoded.length);
        // The first byte is the varint frame length of everything after it.
        assertEquals(encoded.length - 1, encoded[0]);
    }

    @Test void everyAdmissibleCellRoundTrips() {
        for (int q = -RADIUS; q <= RADIUS; q++) {
            for (int r = -RADIUS; r <= RADIUS; r++) {
                if (Math.abs((long) q + r) > RADIUS) continue;
                var payload = new ResearchTableEditPayload(3, 5L, 6L, ResearchNoteEditing.Action.PLACE, q, r, "ignis");
                assertEquals(payload, read(PayloadWire.unsigned(wire(payload))), q + "/" + r);
            }
        }
    }

    @Test void aCellOutsideTheOriginalHexRadiusCannotBeBuilt() {
        for (int[] cell : new int[][] {{RADIUS + 1, 0}, {0, RADIUS + 1}, {-RADIUS - 1, 0}, {0, -RADIUS - 1}, {RADIUS, 1}, {1, RADIUS}}) {
            assertThrows(IllegalArgumentException.class, () -> new ResearchTableEditPayload(1, 1L, 0L,
                    ResearchNoteEditing.Action.PLACE, cell[0], cell[1], "aer"), cell[0] + "/" + cell[1]);
        }
    }

    @Test void theMenuGenerationSessionAndRevisionAreBounded() {
        assertThrows(IllegalArgumentException.class, () -> new ResearchTableEditPayload(-1, 1L, 0L, ResearchNoteEditing.Action.ERASE, 0, 0, null));
        assertThrows(IllegalArgumentException.class, () -> new ResearchTableEditPayload(256, 1L, 0L, ResearchNoteEditing.Action.ERASE, 0, 0, null));
        assertThrows(IllegalArgumentException.class, () -> new ResearchTableEditPayload(1, 0L, 0L, ResearchNoteEditing.Action.ERASE, 0, 0, null));
        assertThrows(IllegalArgumentException.class, () -> new ResearchTableEditPayload(1, 1L, -1L, ResearchNoteEditing.Action.ERASE, 0, 0, null));
    }

    @Test void onlyACanonicalAspectTagMayBePlaced() {
        assertThrows(IllegalArgumentException.class, () -> new ResearchTableEditPayload(1, 1L, 0L, ResearchNoteEditing.Action.PLACE, 0, 0, null));
        assertThrows(IllegalArgumentException.class, () -> new ResearchTableEditPayload(1, 1L, 0L, ResearchNoteEditing.Action.PLACE, 0, 0, ""));
        assertThrows(IllegalArgumentException.class, () -> new ResearchTableEditPayload(1, 1L, 0L, ResearchNoteEditing.Action.PLACE, 0, 0, "not-an-aspect"));
        assertThrows(IllegalArgumentException.class, () -> new ResearchTableEditPayload(1, 1L, 0L, ResearchNoteEditing.Action.PLACE, 0, 0, "AER"));
        // An erase may not smuggle an aspect past the action check.
        assertThrows(IllegalArgumentException.class, () -> new ResearchTableEditPayload(1, 1L, 0L, ResearchNoteEditing.Action.ERASE, 0, 0, "aer"));
    }

    @Test void aTruncatedOversizedOrTrailingFrameIsRefused() {
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        try {
            ResearchTableEditPayload.STREAM_CODEC.encode(buffer,
                    new ResearchTableEditPayload(1, 2L, 3L, ResearchNoteEditing.Action.PLACE, 0, 0, "aer"));
            buffer.writerIndex(buffer.writerIndex() - 1);
            assertThrows(DecoderException.class, () -> ResearchTableEditPayload.STREAM_CODEC.decode(buffer));
            buffer.clear();
            ResearchTableEditPayload.STREAM_CODEC.encode(buffer,
                    new ResearchTableEditPayload(1, 2L, 3L, ResearchNoteEditing.Action.ERASE, 0, 0, null));
            buffer.writeByte(0);
            assertThrows(DecoderException.class, () -> ResearchTableEditPayload.STREAM_CODEC.decode(buffer));
            buffer.clear();
            buffer.writeVarInt(ResearchTableEditPayload.MAX_ENCODED_BYTES + 1);
            assertThrows(DecoderException.class, () -> ResearchTableEditPayload.STREAM_CODEC.decode(buffer));
            buffer.clear();
            buffer.writeVarInt(0);
            assertThrows(DecoderException.class, () -> ResearchTableEditPayload.STREAM_CODEC.decode(buffer));
        } finally {
            buffer.release();
        }
    }

    @Test void anUnknownActionByteIsRefused() {
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        var body = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        try {
            body.writeVarInt(1);
            body.writeLong(2L);
            body.writeLong(3L);
            body.writeByte(2);
            body.writeByte(0);
            body.writeByte(0);
            buffer.writeVarInt(body.readableBytes());
            buffer.writeBytes(body);
            assertThrows(DecoderException.class, () -> ResearchTableEditPayload.STREAM_CODEC.decode(buffer));
        } finally {
            body.release();
            buffer.release();
        }
    }

    @Test void theEncodedFrameIsDeterministic() {
        var payload = new ResearchTableEditPayload(2, 3L, 4L, ResearchNoteEditing.Action.PLACE, 1, 1, "aqua");
        assertArrayEquals(wire(payload), wire(payload));
    }
}
