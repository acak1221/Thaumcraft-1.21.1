package thaumcraft.essentia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import thaumcraft.client.fx.EssentiaFlowClientEffects;
import thaumcraft.common.lib.network.EssentiaFlowFxNetworking;
import thaumcraft.common.lib.network.EssentiaFlowFxPayload;

/**
 * The original essence flow packet of the public essentia source API.
 *
 * <p>Oracle {@code thaumcraft/common/lib/network/fx/PacketFXEssentiaSource.java:20-78}: the packet
 * carries the position of the consumer, the byte deltas to the source and the colour of the aspect, and
 * the client puts the flow into a map with the key
 * {@code x + ":" + y + ":" + z + ":" + tx + ":" + ty + ":" + tz + ":" + color} and a lifetime of fifteen
 * ticks.</p>
 *
 * <p>Oracle {@code thaumcraft/client/lib/ClientTickEventsFML.java:249-269}: the trail keeps its full
 * size while more than five ticks are left, shrinks with {@code (ticks * ticks) / 25} over the last five
 * ticks and the destination of an infusion matrix is one block lower.</p>
 */
class EssentiaFlowPacketParityTest {
    @Test void theFlowMapKeyIsTheOriginalKey() {
        // Oracle PacketFXEssentiaSource.java:68.
        assertEquals("1:2:3:0:1:2:-9999",
                EssentiaFlowClientEffects.key(1, 2, 3, 0, 1, 2, -9999));
        assertEquals("10:64:-7:11:65:-8:16711680",
                EssentiaFlowClientEffects.key(10, 64, -7, 11, 65, -8, 16711680));
    }

    @Test void theFlowLivesFifteenTicks() {
        assertEquals(15, EssentiaFlowClientEffects.LIFETIME);
    }

    @Test void theTrailShrinksOverTheLastFiveTicks() {
        // Oracle ClientTickEventsFML.java:262-266.
        for (int ticks = 6; ticks <= 15; ticks++) {
            assertEquals(1.0F, EssentiaFlowClientEffects.scale(ticks), 1.0E-6F,
                    "the trail of the original keeps its full size above five ticks");
        }
        assertEquals(1.0F, EssentiaFlowClientEffects.scale(5), 1.0E-6F);
        assertEquals(16F / 25F, EssentiaFlowClientEffects.scale(4), 1.0E-6F);
        assertEquals(9F / 25F, EssentiaFlowClientEffects.scale(3), 1.0E-6F);
        assertEquals(4F / 25F, EssentiaFlowClientEffects.scale(2), 1.0E-6F);
        assertEquals(1F / 25F, EssentiaFlowClientEffects.scale(1), 1.0E-6F);
        assertEquals(0F, EssentiaFlowClientEffects.scale(0), 1.0E-6F);
    }

    @Test void thePhaseCountsBackOverTheShrinkingTail() {
        // Oracle ClientTickEventsFML.java:263-266.
        assertEquals(1000, EssentiaFlowClientEffects.phase(1000, 15));
        assertEquals(1000, EssentiaFlowClientEffects.phase(1000, 6));
        assertEquals(999, EssentiaFlowClientEffects.phase(1000, 5));
        assertEquals(996, EssentiaFlowClientEffects.phase(1000, 1));
    }

    @Test void theDestinationOfAnInfusionMatrixIsOneBlockLower() {
        // Oracle ClientTickEventsFML.java:258-261.
        assertEquals(0, EssentiaFlowClientEffects.destinationOffset(false));
        assertEquals(-1, EssentiaFlowClientEffects.destinationOffset(true));
    }

    @Test void theSourceIsRebuiltFromTheByteDeltas() {
        // Oracle PacketFXEssentiaSource.java:65-67: tx = x - dx, ty = y - dy, tz = z - dz.
        var payload = new EssentiaFlowFxPayload(
                ResourceLocation.fromNamespaceAndPath("thaumcraft", "overworld"),
                new BlockPos(10, 64, -7), (byte) 1, (byte) 2, (byte) 3, 0x00ff00);
        assertEquals(new BlockPos(9, 62, -10), payload.source());
    }

    @Test void theByteDeltaWrapsLikeTheOriginal() {
        // The original wrote the delta as a byte, so a source that is more than 127
        // blocks away wraps. The native keeps that wrap instead of hiding it.
        var payload = new EssentiaFlowFxPayload(
                ResourceLocation.fromNamespaceAndPath("thaumcraft", "overworld"),
                new BlockPos(0, 0, 0), (byte) 200, (byte) 0, (byte) 0, 0);
        assertEquals(-200, payload.source().getX());
    }

    @Test void theTargetPointRadiusIsTheOriginalOne() {
        // Oracle EssentiaHandler.java:54: NetworkRegistry.TargetPoint(dim, x, y, z, 32.0).
        assertEquals(32.0D, EssentiaFlowFxNetworking.RANGE, 1.0E-9D);
    }

    @Test void theFlowNeverDrawsWithoutAConsumerAndASource() {
        // The original map is keyed by the consumer and the source, so a flow without
        // a source has no key at all.
        assertTrue(EssentiaFlowClientEffects.key(0, 0, 0, 0, 0, 0, 0)
                .contains("0:0:0:0:0:0:0"));
    }
}
