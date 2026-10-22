package thaumcraft.common.lib.network;

import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * The original essence flow packet of the public essentia source API.
 *
 * <p>Oracle {@code thaumcraft/common/lib/network/fx/PacketFXEssentiaSource.java:23-52}: the packet carries
 * the position of the CONSUMER, the three byte deltas to the source and the colour of the aspect. The
 * original wrote the deltas as {@code (byte)(consumer - source)} (oracle
 * {@code thaumcraft/common/lib/events/EssentiaHandler.java:54}) and the client rebuilt the source with
 * {@code tx = message.x - message.dx} (oracle {@code PacketFXEssentiaSource.java:65-67}).</p>
 *
 * <p>The byte deltas and the rebuild are kept exactly as the original had them, so a source that is more
 * than 127 blocks away is wrapped by the original byte as well. The native payload adds the dimension of
 * the world, because the 1.7.10 packet had no dimension of its own and relied on the connection.</p>
 */
public record EssentiaFlowFxPayload(ResourceLocation dimension, BlockPos consumer, byte dx, byte dy, byte dz, int rgb)
        implements CustomPacketPayload {
    public static final Type<EssentiaFlowFxPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("thaumcraft", "essentia_flow_fx"));
    public static final StreamCodec<RegistryFriendlyByteBuf, EssentiaFlowFxPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeResourceLocation(payload.dimension);
                buffer.writeBlockPos(payload.consumer);
                buffer.writeByte(payload.dx);
                buffer.writeByte(payload.dy);
                buffer.writeByte(payload.dz);
                buffer.writeInt(payload.rgb);
            },
            buffer -> new EssentiaFlowFxPayload(buffer.readResourceLocation(), buffer.readBlockPos(),
                    buffer.readByte(), buffer.readByte(), buffer.readByte(), buffer.readInt()));

    public EssentiaFlowFxPayload {
        Objects.requireNonNull(dimension, "dimension");
        consumer = Objects.requireNonNull(consumer, "consumer").immutable();
    }

    /**
     * The source of the flow, rebuilt the way the original client rebuilt it.
     *
     * <p>Oracle {@code PacketFXEssentiaSource.java:65-67}: {@code tx = x - dx}, {@code ty = y - dy},
     * {@code tz = z - dz}.</p>
     */
    public BlockPos source() {
        return new BlockPos(consumer.getX() - dx, consumer.getY() - dy, consumer.getZ() - dz);
    }

    @Override public Type<EssentiaFlowFxPayload> type() { return TYPE; }
}
