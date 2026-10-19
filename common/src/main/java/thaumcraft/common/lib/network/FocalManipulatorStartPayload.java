package thaumcraft.common.lib.network;

import io.netty.handler.codec.DecoderException;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Bounded START intent. Selection itself is local; no stack, quote or XP is trusted. */
public record FocalManipulatorStartPayload(int containerId, UUID sessionId, long expectedRevision, int upgradeId)
        implements CustomPacketPayload {
    public static final int ENCODED_BYTES = 27;
    public static final Type<FocalManipulatorStartPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("thaumcraft", "focal_manipulator_start"));
    public static final StreamCodec<RegistryFriendlyByteBuf, FocalManipulatorStartPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeByte(payload.containerId);
                buffer.writeUUID(payload.sessionId);
                buffer.writeLong(payload.expectedRevision);
                buffer.writeShort(payload.upgradeId);
            }, buffer -> {
                if (buffer.readableBytes() != ENCODED_BYTES) throw new DecoderException("Invalid focal START length");
                try {
                    return new FocalManipulatorStartPayload(buffer.readUnsignedByte(), buffer.readUUID(),
                            buffer.readLong(), buffer.readUnsignedShort());
                } catch (IllegalArgumentException failure) {
                    throw new DecoderException("Invalid focal START intent", failure);
                }
            });

    public FocalManipulatorStartPayload {
        Objects.requireNonNull(sessionId, "sessionId");
        if (containerId < 0 || containerId > 255 || expectedRevision < 0 || upgradeId < 0 || upgradeId > Short.MAX_VALUE) {
            throw new IllegalArgumentException("Invalid focal menu generation or upgrade id");
        }
    }

    @Override public Type<FocalManipulatorStartPayload> type() { return TYPE; }
}
