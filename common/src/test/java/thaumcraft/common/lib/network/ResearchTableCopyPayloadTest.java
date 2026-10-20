package thaumcraft.common.lib.network;

import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ResearchTableCopyPayloadTest {
    @Test void menuSessionAndRevisionRoundTripAtTheirBoundaries() {
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        try {
            for (var intent : new ResearchTableCopyPayload[]{new ResearchTableCopyPayload(0, 1, 0), new ResearchTableCopyPayload(255, Long.MAX_VALUE, Long.MAX_VALUE)}) {
                buffer.clear(); ResearchTableCopyPayload.STREAM_CODEC.encode(buffer, intent);
                assertTrue(buffer.readableBytes() <= ResearchTableCopyPayload.MAX_ENCODED_BYTES);
                assertEquals(intent, ResearchTableCopyPayload.STREAM_CODEC.decode(buffer)); assertFalse(buffer.isReadable());
            }
        } finally { buffer.release(); }
    }
    @Test void truncatedTrailingAndOversizedFramesCannotBecomeCopyIntents() {
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        try {
            ResearchTableCopyPayload.STREAM_CODEC.encode(buffer, new ResearchTableCopyPayload(1, 2, 3));
            buffer.writerIndex(buffer.writerIndex() - 1);
            assertThrows(DecoderException.class, () -> ResearchTableCopyPayload.STREAM_CODEC.decode(buffer));
            buffer.clear(); ResearchTableCopyPayload.STREAM_CODEC.encode(buffer, new ResearchTableCopyPayload(1, 2, 3)); buffer.writeByte(0);
            assertThrows(DecoderException.class, () -> ResearchTableCopyPayload.STREAM_CODEC.decode(buffer));
            buffer.clear(); buffer.writeVarInt(1000);
            assertThrows(DecoderException.class, () -> ResearchTableCopyPayload.STREAM_CODEC.decode(buffer));
        } finally { buffer.release(); }
    }
    @Test void invalidSessionContainerAndRevisionAreRefusedBeforeDispatch() {
        assertThrows(IllegalArgumentException.class, () -> new ResearchTableCopyPayload(-1, 1, 0));
        assertThrows(IllegalArgumentException.class, () -> new ResearchTableCopyPayload(256, 1, 0));
        assertThrows(IllegalArgumentException.class, () -> new ResearchTableCopyPayload(1, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new ResearchTableCopyPayload(1, 1, -1));
    }
}
