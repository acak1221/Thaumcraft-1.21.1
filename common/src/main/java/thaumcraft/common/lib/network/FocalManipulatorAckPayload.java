package thaumcraft.common.lib.network;

import io.netty.handler.codec.DecoderException;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Echo the precise pending intent, including refusals; native slots/BE sync own all material state. */
public record FocalManipulatorAckPayload(int containerId, UUID sessionId, long expectedRevision, int upgradeId,
                                         boolean accepted, long serverRevision) implements CustomPacketPayload {
    public static final int ENCODED_BYTES = FocalManipulatorStartPayload.ENCODED_BYTES + 9;
    public static final Type<FocalManipulatorAckPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("thaumcraft", "focal_manipulator_ack"));
    public static final StreamCodec<RegistryFriendlyByteBuf, FocalManipulatorAckPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeByte(payload.containerId);
                buffer.writeUUID(payload.sessionId);
                buffer.writeLong(payload.expectedRevision);
                buffer.writeShort(payload.upgradeId);
                buffer.writeBoolean(payload.accepted);
                buffer.writeLong(payload.serverRevision);
            }, buffer -> {
                if (buffer.readableBytes() != ENCODED_BYTES) throw new DecoderException("Invalid focal ACK length");
                try {
                    int menu = buffer.readUnsignedByte();
                    UUID session = buffer.readUUID();
                    long expected = buffer.readLong();
                    int upgrade = buffer.readUnsignedShort();
                    int accepted = buffer.readUnsignedByte();
                    if (accepted > 1) throw new DecoderException("Noncanonical focal ACK flag");
                    return new FocalManipulatorAckPayload(menu, session, expected, upgrade, accepted == 1, buffer.readLong());
                } catch (IllegalArgumentException failure) {
                    throw new DecoderException("Invalid focal ACK generation", failure);
                }
            });

    public FocalManipulatorAckPayload {
        // The same bounded domain in both directions; do not silently truncate ids.
        new FocalManipulatorStartPayload(containerId, sessionId, expectedRevision, upgradeId);
        if (serverRevision < 0) throw new IllegalArgumentException("Invalid focal server revision");
    }

    @Override public Type<FocalManipulatorAckPayload> type() { return TYPE; }
}
