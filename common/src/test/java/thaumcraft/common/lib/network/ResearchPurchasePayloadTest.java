package thaumcraft.common.lib.network;

import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.junit.jupiter.api.Test;
import thaumcraft.common.lib.research.ResearchCatalog;
import static org.junit.jupiter.api.Assertions.*;

class ResearchPurchasePayloadTest {
    @Test void canonicalCaseSensitiveKeysRoundTripWithoutExtraIdentityOrPrices() {
        ResearchCatalog.loadBundled();
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        try {
            for (var definition : ResearchCatalog.all()) {
                buffer.clear();
                var intent = new ResearchPurchasePayload(definition.key());
                ResearchPurchasePayload.STREAM_CODEC.encode(buffer, intent);
                assertEquals(definition.key(), buffer.readUtf(ResearchPurchasePayload.MAX_KEY_LENGTH));
                assertFalse(buffer.isReadable(), "Only canonical key belongs on the wire");
                buffer.readerIndex(0);
                assertEquals(intent, ResearchPurchasePayload.STREAM_CODEC.decode(buffer));
                assertFalse(buffer.isReadable());
            }
        } finally { buffer.release(); }
    }

    @Test void oversizedWireAndLocalKeysAreRejectedBeforeLookup() {
        assertThrows(IllegalArgumentException.class,
                () -> new ResearchPurchasePayload("x".repeat(ResearchPurchasePayload.MAX_KEY_LENGTH + 1)));
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        try {
            buffer.writeUtf("x".repeat(ResearchPurchasePayload.MAX_KEY_LENGTH + 1));
            assertThrows(DecoderException.class, () -> ResearchPurchasePayload.STREAM_CODEC.decode(buffer));
        } finally { buffer.release(); }
    }

    @Test void boundedUnknownKeysRemainIntentForServerRejection() {
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        try {
            for (String key : new String[]{"", "@BANNERS", "enchfabric", "NO_SUCH_RESEARCH", "../../research"}) {
                buffer.clear();
                ResearchPurchasePayload.STREAM_CODEC.encode(buffer, new ResearchPurchasePayload(key));
                assertEquals(key, ResearchPurchasePayload.STREAM_CODEC.decode(buffer).key());
            }
        } finally { buffer.release(); }
    }
}
