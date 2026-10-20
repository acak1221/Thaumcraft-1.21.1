package thaumcraft.common.lib.network;

import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.junit.jupiter.api.Test;
import thaumcraft.common.config.ResearchSettings;
import static org.junit.jupiter.api.Assertions.*;

class ResearchSettingsPayloadTest {
    @Test void allFlagsAndSignedIntegerBoundariesRoundTripWithoutTheLegacyDifficultyByteTruncation() {
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        try {
            for (int flags = 0; flags < 32; flags++) for (int value : new int[]{Integer.MIN_VALUE, -1, 0, 1, 256, Integer.MAX_VALUE}) {
                var expected = new ResearchSettingsPayload(new ResearchSettings((flags & 1) != 0, (flags & 2) != 0, (flags & 4) != 0,
                        (flags & 8) != 0, (flags & 16) != 0, value, ~value));
                buffer.clear(); ResearchSettingsPayload.STREAM_CODEC.encode(buffer, expected);
                assertTrue(buffer.readableBytes() <= 11); assertEquals(expected, ResearchSettingsPayload.STREAM_CODEC.decode(buffer)); assertFalse(buffer.isReadable());
            }
        } finally { buffer.release(); }
    }
    @Test void unknownFlagsAndTruncatedFieldsCannotBecomePartialClientSettings() {
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        try {
            buffer.writeByte(32); buffer.writeVarInt(0); buffer.writeVarInt(100);
            assertThrows(IllegalArgumentException.class, () -> ResearchSettingsPayload.STREAM_CODEC.decode(buffer));
            buffer.clear(); buffer.writeByte(0); buffer.writeVarInt(0);
            assertThrows(IndexOutOfBoundsException.class, () -> ResearchSettingsPayload.STREAM_CODEC.decode(buffer));
        } finally { buffer.release(); }
    }
}
