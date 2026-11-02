package thaumcraft.common.lib.network;

import static org.junit.jupiter.api.Assertions.*;
import io.netty.buffer.Unpooled;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class NodeDrainFxPayloadTest {
    private static final ResourceLocation DIMENSION = ResourceLocation.parse("thaumcraft:test");
    private static final UUID ACTOR = UUID.fromString("00000000-0000-0000-0000-000000000123");

    @Test void activeAndStopPulsesKeepFullCoordinatesAndExactActorAcrossTheWire() {
        for (boolean active : List.of(false, true)) {
            var payload = new NodeDrainFxPayload(DIMENSION, new BlockPos(-70_000, 8192, 90_000), 128, ACTOR, 0x56c000, active);
            var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
            try {
                NodeDrainFxPayload.STREAM_CODEC.encode(buffer, payload);
                assertEquals(payload, NodeDrainFxPayload.STREAM_CODEC.decode(buffer));
                assertFalse(buffer.isReadable());
            } finally { buffer.release(); }
        }
    }

    @Test void queuedAppearanceDoesNotFollowAMutablePositionAndRejectsInvalidIdentityColorOrBounds() {
        var pos = new BlockPos.MutableBlockPos(10, 70, 20);
        var payload = new NodeDrainFxPayload(DIMENSION, pos, 0, ACTOR, 0, true);
        pos.set(0, 0, 0);
        assertEquals(new BlockPos(10, 70, 20), payload.node());
        assertThrows(IllegalArgumentException.class, () -> new NodeDrainFxPayload(DIMENSION, pos, -1, ACTOR, 0, true));
        for (int color : List.of(-1, 0x1000000))
            assertThrows(IllegalArgumentException.class, () -> new NodeDrainFxPayload(DIMENSION, pos, 1, ACTOR, color, true));
        assertThrows(IllegalArgumentException.class, () -> new NodeDrainFxPayload(DIMENSION, new BlockPos(Integer.MIN_VALUE, 0, 0), 1, ACTOR, 0, true));
        assertThrows(NullPointerException.class, () -> new NodeDrainFxPayload(DIMENSION, pos, 1, null, 0, true));
    }
}
