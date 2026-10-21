package thaumcraft.common.lib.network;

import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/**
 * The original client visual channels of the block effects (oracle
 * {@code thaumcraft/common/lib/network/fx/PacketFXBlockArc.java},
 * {@code thaumcraft/common/lib/network/fx/PacketFXBlockZap.java} and
 * {@code thaumcraft/common/lib/network/fx/PacketFXBlockBubble.java}), as one S2C payload.
 *
 * <p>The three original packets are pure visuals: the arc lightning of the eldritch warden and the cultist
 * portal, the node bolt of the eldritch trap and the crucible bubbles of the elemental axe. The port keeps
 * them in one payload with an explicit kind, because the modern channel replaces three registered packets
 * with one registered payload type.</p>
 */
public record BlockFxPayload(ResourceLocation dimension, int kind, BlockPos target, int source, Vec3 origin,
                             Vec3 destination, int rgb) implements CustomPacketPayload {
    /** The original PacketFXBlockArc: the arc lightning of an entity to a block. */
    public static final int ARC = 0;
    /** The original PacketFXBlockZap: the node bolt between two points. */
    public static final int ZAP = 1;
    /** The original PacketFXBlockBubble: the crucible bubbles around a block. */
    public static final int BUBBLE = 2;

    public static final Type<BlockFxPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("thaumcraft", "block_fx"));
    public static final StreamCodec<RegistryFriendlyByteBuf, BlockFxPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeResourceLocation(payload.dimension);
                buffer.writeVarInt(payload.kind);
                buffer.writeBlockPos(payload.target);
                buffer.writeVarInt(payload.source);
                buffer.writeDouble(payload.origin.x); buffer.writeDouble(payload.origin.y); buffer.writeDouble(payload.origin.z);
                buffer.writeDouble(payload.destination.x); buffer.writeDouble(payload.destination.y); buffer.writeDouble(payload.destination.z);
                buffer.writeInt(payload.rgb);
            },
            buffer -> new BlockFxPayload(buffer.readResourceLocation(), buffer.readVarInt(), buffer.readBlockPos(),
                    buffer.readVarInt(), new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble()),
                    new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble()), buffer.readInt()));

    public BlockFxPayload {
        Objects.requireNonNull(dimension, "dimension");
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(origin, "origin");
        Objects.requireNonNull(destination, "destination");
        target = target.immutable();
        if (kind != ARC && kind != ZAP && kind != BUBBLE) throw new IllegalArgumentException("Unknown block effect kind");
        if (kind == ARC && source < 0) throw new IllegalArgumentException("The arc needs a source entity");
        if (kind == ZAP && (origin.distanceToSqr(destination) < 1.0E-6 || !finite(origin) || !finite(destination)))
            throw new IllegalArgumentException("The node bolt needs two different finite points");
        if (kind == BUBBLE && (rgb < 0 || rgb > 0xffffff)) throw new IllegalArgumentException("The bubbles need a valid colour");
    }

    /** The arc lightning of the original PacketFXBlockArc. */
    public static BlockFxPayload arc(ResourceLocation dimension, BlockPos target, int source) {
        return new BlockFxPayload(dimension, ARC, target, source, Vec3.ZERO, Vec3.ZERO, 0);
    }

    /** The node bolt of the original PacketFXBlockZap. */
    public static BlockFxPayload zap(ResourceLocation dimension, Vec3 origin, Vec3 destination) {
        return new BlockFxPayload(dimension, ZAP, BlockPos.containing(origin), -1, origin, destination, 0);
    }

    /** The crucible bubbles of the original PacketFXBlockBubble. */
    public static BlockFxPayload bubble(ResourceLocation dimension, BlockPos target, int rgb) {
        return new BlockFxPayload(dimension, BUBBLE, target, -1, Vec3.ZERO, Vec3.ZERO, rgb);
    }

    @Override public Type<BlockFxPayload> type() { return TYPE; }

    private static boolean finite(Vec3 point) {
        return Double.isFinite(point.x) && Double.isFinite(point.y) && Double.isFinite(point.z);
    }
}
