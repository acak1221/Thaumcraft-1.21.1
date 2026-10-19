package thaumcraft.foci;

import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.junit.jupiter.api.Test;
import thaumcraft.common.lib.network.FocalManipulatorAckPayload;
import thaumcraft.common.lib.network.FocalManipulatorStartPayload;
import static org.junit.jupiter.api.Assertions.*;

class FocalManipulatorPayloadTest {
    private static final UUID SESSION = UUID.fromString("b1017993-e4e8-4c17-934c-304ed19917e1");

    private static RegistryFriendlyByteBuf wire() {
        return new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
    }

    @Test void startCarriesTheFullMenuGenerationAndCanonicalUpgradeId() {
        for (int id : List.of(0, 1, 2, 3, 20, (int) Short.MAX_VALUE)) {
            var value = new FocalManipulatorStartPayload(255, SESSION, Long.MAX_VALUE, id);
            var buffer = wire();
            try {
                FocalManipulatorStartPayload.STREAM_CODEC.encode(buffer, value);
                assertEquals(27, buffer.readableBytes());
                assertEquals(value, FocalManipulatorStartPayload.STREAM_CODEC.decode(buffer));
                assertFalse(buffer.isReadable());
            } finally { buffer.release(); }
        }
    }

    @Test void bothAcceptedAndRefusedAcknowledgmentsEchoTheExactPendingIntent() {
        for (boolean accepted : List.of(false, true)) {
            var value = new FocalManipulatorAckPayload(7, SESSION, 4_294_967_297L, 3, accepted, 4_294_967_299L);
            var buffer = wire();
            try {
                FocalManipulatorAckPayload.STREAM_CODEC.encode(buffer, value);
                assertEquals(36, buffer.readableBytes());
                assertEquals(value, FocalManipulatorAckPayload.STREAM_CODEC.decode(buffer));
                assertFalse(buffer.isReadable());
            } finally { buffer.release(); }
        }
    }

    @Test void constructorsRejectUnrepresentableOrNegativeIntentInsteadOfTruncating() {
        for (int menu : List.of(-1, 256, Integer.MAX_VALUE)) {
            assertThrows(IllegalArgumentException.class, () -> new FocalManipulatorStartPayload(menu, SESSION, 0, 1));
        }
        for (int id : List.of(-1, 32768, Integer.MAX_VALUE)) {
            assertThrows(IllegalArgumentException.class, () -> new FocalManipulatorStartPayload(1, SESSION, 0, id));
        }
        assertThrows(IllegalArgumentException.class, () -> new FocalManipulatorStartPayload(1, SESSION, -1, 1));
        assertThrows(NullPointerException.class, () -> new FocalManipulatorStartPayload(1, null, 0, 1));
        assertThrows(IllegalArgumentException.class, () -> new FocalManipulatorAckPayload(1, SESSION, 0, 1, false, -1));
        assertThrows(IllegalArgumentException.class, () -> new FocalManipulatorAckPayload(1, SESSION, -1, 1, false, 0));
    }

    @Test void decoderRefusesWholeShortOversizedAndTrailingFrames() {
        for (int length : List.of(0, 1, 26, 28, 1024)) {
            var buffer = wire();
            try {
                buffer.writeZero(length);
                assertThrows(DecoderException.class, () -> FocalManipulatorStartPayload.STREAM_CODEC.decode(buffer));
            } finally { buffer.release(); }
        }
        for (int length : List.of(0, 27, 35, 37, 1024)) {
            var buffer = wire();
            try {
                buffer.writeZero(length);
                assertThrows(DecoderException.class, () -> FocalManipulatorAckPayload.STREAM_CODEC.decode(buffer));
            } finally { buffer.release(); }
        }
    }

    @Test void malformedCanonicalLengthStartCannotBecomeAValidChoice() {
        for (boolean negativeRevision : List.of(false, true)) {
            var buffer = wire();
            try {
                buffer.writeByte(1); buffer.writeUUID(SESSION);
                buffer.writeLong(negativeRevision ? -1 : 0); buffer.writeShort(negativeRevision ? 1 : 0xffff);
                assertEquals(27, buffer.readableBytes());
                assertThrows(DecoderException.class, () -> FocalManipulatorStartPayload.STREAM_CODEC.decode(buffer));
            } finally { buffer.release(); }
        }
    }

    @Test void ackRejectsNoncanonicalBooleanAndNegativeServerGeneration() {
        for (boolean badFlag : List.of(false, true)) {
            var buffer = wire();
            try {
                buffer.writeByte(1); buffer.writeUUID(SESSION); buffer.writeLong(0); buffer.writeShort(1);
                buffer.writeByte(badFlag ? 2 : 0); buffer.writeLong(badFlag ? 0 : -1);
                assertEquals(36, buffer.readableBytes());
                assertThrows(DecoderException.class, () -> FocalManipulatorAckPayload.STREAM_CODEC.decode(buffer));
            } finally { buffer.release(); }
        }
    }
}
