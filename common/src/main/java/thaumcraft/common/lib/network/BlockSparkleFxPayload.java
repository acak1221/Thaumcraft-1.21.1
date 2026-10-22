package thaumcraft.common.lib.network;

import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * The original block sparkle packet.
 *
 * <p>Oracle {@code thaumcraft/common/lib/network/fx/PacketFXBlockSparkle.java:36-52}: the packet carries
 * the block position and the colour, and the client half calls
 * {@code Thaumcraft.proxy.blockSparkle(world, x, y, z, color, 7)} with seven particles.</p>
 *
 * <p>The colour is the raw 1.7.10 {@code int} of the packet, including the special value
 * {@code -9999} of the original, which is not a colour at all: the original replaces it with a random
 * grey (oracle {@code thaumcraft/client/ClientProxy.java:637-643}).</p>
 */
public record BlockSparkleFxPayload(ResourceLocation dimension, BlockPos target, int rgb) implements CustomPacketPayload {
    public static final Type<BlockSparkleFxPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("thaumcraft", "block_sparkle_fx"));
    /**
     * The original random grey colour: oracle {@code thaumcraft/client/ClientProxy.java:638} reads the
     * colour {@code -9999} and replaces the three channels with {@code 0.33 + random * 0.67}.
     */
    public static final int RANDOM_GREY = -9999;
    public static final StreamCodec<RegistryFriendlyByteBuf, BlockSparkleFxPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeResourceLocation(payload.dimension);
                buffer.writeBlockPos(payload.target);
                buffer.writeInt(payload.rgb);
            },
            buffer -> new BlockSparkleFxPayload(buffer.readResourceLocation(), buffer.readBlockPos(), buffer.readInt()));
    public BlockSparkleFxPayload {
        Objects.requireNonNull(dimension, "dimension");
        target = Objects.requireNonNull(target, "target").immutable();
    }
    @Override public Type<BlockSparkleFxPayload> type() { return TYPE; }
}
