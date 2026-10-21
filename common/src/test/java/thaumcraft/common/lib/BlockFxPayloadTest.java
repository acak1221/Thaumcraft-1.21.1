package thaumcraft.common.lib;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import thaumcraft.common.lib.network.BlockFxNetworking;
import thaumcraft.common.lib.network.BlockFxPayload;

/**
 * The original block visual channels (oracle
 * {@code thaumcraft/common/lib/network/fx/PacketFXBlockArc.java},
 * {@code thaumcraft/common/lib/network/fx/PacketFXBlockZap.java} and
 * {@code thaumcraft/common/lib/network/fx/PacketFXBlockBubble.java}) as one S2C payload.
 */
class BlockFxPayloadTest {
    private static final ResourceLocation DIMENSION = ResourceLocation.fromNamespaceAndPath("minecraft", "overworld");

    @Test void theArcNeedsItsSourceEntityAndItsTargetBlock() {
        var payload = BlockFxPayload.arc(DIMENSION, new BlockPos(1, 2, 3), 42);
        assertEquals(BlockFxPayload.ARC, payload.kind());
        assertEquals(42, payload.source());
        assertEquals(new BlockPos(1, 2, 3), payload.target());
        assertThrows(IllegalArgumentException.class, () -> BlockFxPayload.arc(DIMENSION, new BlockPos(0, 0, 0), -1));
    }

    @Test void theNodeBoltNeedsTwoDifferentFinitePoints() {
        var payload = BlockFxPayload.zap(DIMENSION, Vec3.ZERO, new Vec3(1, 2, 3));
        assertEquals(BlockFxPayload.ZAP, payload.kind());
        assertThrows(IllegalArgumentException.class, () -> BlockFxPayload.zap(DIMENSION, Vec3.ZERO, Vec3.ZERO));
        assertThrows(IllegalArgumentException.class,
                () -> BlockFxPayload.zap(DIMENSION, new Vec3(Double.NaN, 0, 0), new Vec3(1, 2, 3)));
        assertThrows(IllegalArgumentException.class,
                () -> BlockFxPayload.zap(DIMENSION, Vec3.ZERO, new Vec3(Double.POSITIVE_INFINITY, 2, 3)));
    }

    @Test void theCrucibleBubblesNeedARealColour() {
        var payload = BlockFxPayload.bubble(DIMENSION, new BlockPos(4, 5, 6), 0x5454ff);
        assertEquals(BlockFxPayload.BUBBLE, payload.kind());
        assertEquals(0x5454ff, payload.rgb());
        assertThrows(IllegalArgumentException.class, () -> BlockFxPayload.bubble(DIMENSION, new BlockPos(0, 0, 0), -1));
        assertThrows(IllegalArgumentException.class, () -> BlockFxPayload.bubble(DIMENSION, new BlockPos(0, 0, 0), 0x1000000));
    }

    @Test void anUnknownKindIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new BlockFxPayload(DIMENSION, 7,
                BlockPos.ZERO, 1, Vec3.ZERO, Vec3.ZERO, 0));
    }

    @Test void theOriginalWroteToTheThirtyTwoBlocksAroundTheEffect() {
        // The original TargetPoint of the three block effect packets.
        assertEquals(32.0D, BlockFxNetworking.RANGE);
    }
}
