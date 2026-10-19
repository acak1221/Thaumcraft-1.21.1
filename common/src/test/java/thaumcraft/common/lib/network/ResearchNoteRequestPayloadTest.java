package thaumcraft.common.lib.network;

import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.junit.jupiter.api.Test;
import thaumcraft.common.lib.research.ResearchCatalog;
import static org.junit.jupiter.api.Assertions.*;

class ResearchNoteRequestPayloadTest {
    @Test void eachCanonicalResearchKeyUsesOnlyItsBoundedKeyOnTheWire() {
        ResearchCatalog.loadBundled();
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        try {
            for (var research : ResearchCatalog.all()) {
                buffer.clear();
                var intent = new ResearchNoteRequestPayload(research.key());
                ResearchNoteRequestPayload.STREAM_CODEC.encode(buffer, intent);
                assertEquals(research.key(), buffer.readUtf(ResearchNoteRequestPayload.MAX_KEY_LENGTH));
                assertFalse(buffer.isReadable(), "No identity, slots, prices, ink damage or client-generated grid");
                buffer.readerIndex(0);
                assertEquals(intent, ResearchNoteRequestPayload.STREAM_CODEC.decode(buffer));
                assertFalse(buffer.isReadable());
            }
        } finally { buffer.release(); }
    }

    @Test void oversizedIntentIsRejectedAtConstructionAndDecode() {
        var tooLong = "x".repeat(ResearchNoteRequestPayload.MAX_KEY_LENGTH + 1);
        assertThrows(IllegalArgumentException.class, () -> new ResearchNoteRequestPayload(tooLong));
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        try {
            buffer.writeUtf(tooLong);
            assertThrows(DecoderException.class, () -> ResearchNoteRequestPayload.STREAM_CODEC.decode(buffer));
        } finally { buffer.release(); }
    }

    @Test void boundedUnknownAndClueKeysRemainUntrustedIntentForServerRejection() {
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        try {
            for (String key : new String[]{"", "nitor", "@NITOR", "NO_SUCH_RESEARCH", "../NITOR"}) {
                buffer.clear();
                ResearchNoteRequestPayload.STREAM_CODEC.encode(buffer, new ResearchNoteRequestPayload(key));
                assertEquals(key, ResearchNoteRequestPayload.STREAM_CODEC.decode(buffer).key());
            }
        } finally { buffer.release(); }
    }
}
