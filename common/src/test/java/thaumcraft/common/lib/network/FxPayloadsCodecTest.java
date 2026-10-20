package thaumcraft.common.lib.network;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

/** The small effect payloads: wisp lightning, the guardian's sonic beam and the growth lamp sparkle. */
class FxPayloadsCodecTest {
    @Test void aWispZapIsTwoSignedEntityIds() {
        WispZapPayload payload = new WispZapPayload(7, -2);
        byte[] wire = PayloadWire.bytes(WispZapPayload.STREAM_CODEC, payload);
        assertArrayEquals(new int[] {0, 0, 0, 7, 0xFF, 0xFF, 0xFF, 0xFE}, PayloadWire.unsigned(wire));
        assertEquals(payload, PayloadWire.decode(WispZapPayload.STREAM_CODEC, PayloadWire.unsigned(wire)));
        for (int id : new int[] {Integer.MIN_VALUE, -1, 0, Integer.MAX_VALUE}) {
            WispZapPayload edge = new WispZapPayload(id, ~id);
            assertEquals(edge, PayloadWire.decode(WispZapPayload.STREAM_CODEC, PayloadWire.unsigned(PayloadWire.bytes(WispZapPayload.STREAM_CODEC, edge))));
        }
        assertThrows(IndexOutOfBoundsException.class, () -> PayloadWire.decode(WispZapPayload.STREAM_CODEC, 0, 0, 0, 7, 0, 0, 0));
    }

    @Test void aGuardianSonicIsOneSignedEntityId() {
        for (int id : new int[] {Integer.MIN_VALUE, -1, 0, 1, 1_000_000, Integer.MAX_VALUE}) {
            GuardianSonicPayload payload = new GuardianSonicPayload(id);
            byte[] wire = PayloadWire.bytes(GuardianSonicPayload.STREAM_CODEC, payload);
            assertEquals(4, wire.length);
            assertEquals(payload, PayloadWire.decode(GuardianSonicPayload.STREAM_CODEC, PayloadWire.unsigned(wire)));
        }
        assertThrows(IndexOutOfBoundsException.class, () -> PayloadWire.decode(GuardianSonicPayload.STREAM_CODEC, 1, 2, 3));
    }

    @Test void aGrowthLampSparkleCarriesTheDimensionAndTheTargetBlock() {
        GrowthLampFxPayload payload = new GrowthLampFxPayload(ResourceLocation.parse("minecraft:the_nether"), new BlockPos(-30, 64, 1_000));
        byte[] wire = PayloadWire.bytes(GrowthLampFxPayload.STREAM_CODEC, payload);
        assertEquals(payload, PayloadWire.decode(GrowthLampFxPayload.STREAM_CODEC, PayloadWire.unsigned(wire)));
        // a resource location is a length-prefixed string, then the block position is one packed long
        assertEquals(1 + "minecraft:the_nether".length() + 8, wire.length);
    }

    @Test void aGrowthLampSparkleKeepsAnImmutableCopyOfItsPosition() {
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos(1, 2, 3);
        GrowthLampFxPayload payload = new GrowthLampFxPayload(ResourceLocation.parse("minecraft:overworld"), mutable);
        mutable.set(9, 9, 9);
        assertEquals(new BlockPos(1, 2, 3), payload.target(), "later changes of the caller's position do not move the sparkle");
        assertNotSame(mutable, payload.target());
    }

    @Test void aGrowthLampSparkleRequiresBothParts() {
        assertThrows(NullPointerException.class, () -> new GrowthLampFxPayload(null, BlockPos.ZERO));
        assertThrows(NullPointerException.class, () -> new GrowthLampFxPayload(ResourceLocation.parse("minecraft:overworld"), null));
    }
}
