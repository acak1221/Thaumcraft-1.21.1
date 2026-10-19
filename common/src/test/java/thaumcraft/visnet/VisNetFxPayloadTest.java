package thaumcraft.visnet;

import io.netty.buffer.Unpooled;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import thaumcraft.common.lib.network.VisNetFxPayload;
import static org.junit.jupiter.api.Assertions.*;

class VisNetFxPayloadTest {
    private static final ResourceLocation DIMENSION = ResourceLocation.fromNamespaceAndPath("thaumcraft", "test_dimension");

    @Test void fullSignedCoordinatesAndDimensionRoundTripWithoutShortWrapping() {
        for (BlockPos start : List.of(new BlockPos(29_999_990, -64, -29_999_990), new BlockPos(-80_000, 300, 80_000))) {
            var value = new VisNetFxPayload(DIMENSION, start, start.offset(7, 1, 0), 0x56c000);
            var wire = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
            try {
                VisNetFxPayload.STREAM_CODEC.encode(wire, value);
                assertEquals(value, VisNetFxPayload.STREAM_CODEC.decode(wire));
                assertFalse(wire.isReadable());
                assertEquals(DIMENSION, value.dimension());
            } finally { wire.release(); }
        }
    }

    @Test void mutableInputPositionsCannotChangeAQueuedVisualFrame() {
        var origin = new BlockPos.MutableBlockPos(50_000, -40, 90_000);
        var target = new BlockPos.MutableBlockPos(50_007, -40, 90_000);
        var frame = new VisNetFxPayload(DIMENSION, origin, target, 0);
        origin.set(0, 0, 0); target.set(1, 1, 1);
        assertEquals(new BlockPos(50_000, -40, 90_000), frame.origin());
        assertEquals(new BlockPos(50_007, -40, 90_000), frame.target());
        assertEquals(0, frame.rgb());
    }

    @Test void malformedGeometryColorAndNullsAreRefusedWhole() {
        var pos = new BlockPos(0, 70, 0);
        for (BlockPos invalid : List.of(pos, pos.offset(65, 0, 0), new BlockPos(Integer.MIN_VALUE, 70, 0),
                new BlockPos(0, Integer.MAX_VALUE, 0), new BlockPos(30_000_001, 70, 0))) {
            assertThrows(IllegalArgumentException.class, () -> new VisNetFxPayload(DIMENSION, pos, invalid, 0xffffff));
        }
        for (int rgb : List.of(-1, 0x1000000, Integer.MAX_VALUE)) {
            assertThrows(IllegalArgumentException.class, () -> new VisNetFxPayload(DIMENSION, pos, pos.east(), rgb));
        }
        assertThrows(NullPointerException.class, () -> new VisNetFxPayload(null, pos, pos.east(), 0));
        assertThrows(NullPointerException.class, () -> new VisNetFxPayload(DIMENSION, null, pos.east(), 0));
        assertThrows(NullPointerException.class, () -> new VisNetFxPayload(DIMENSION, pos, null, 0));
    }

    @Test void decoderDoesNotConvertAnInvalidRgbOrTruncatedPositionToAUsablePulse() {
        for (boolean truncated : List.of(false, true)) {
            var wire = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
            try {
                wire.writeResourceLocation(DIMENSION);
                wire.writeInt(50_000); wire.writeInt(-64); wire.writeInt(90_000);
                wire.writeInt(50_007);
                if (truncated) {
                    assertThrows(IndexOutOfBoundsException.class, () -> VisNetFxPayload.STREAM_CODEC.decode(wire));
                } else {
                    wire.writeInt(-64); wire.writeInt(90_000); wire.writeInt(-1);
                    assertThrows(IllegalArgumentException.class, () -> VisNetFxPayload.STREAM_CODEC.decode(wire));
                }
            } finally { wire.release(); }
        }
    }
}
