package thaumcraft.common.lib.network;

import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Copy intent, never client-supplied prices, note data, slot selection or player identity. */
public record ResearchTableCopyPayload(int containerId, long sessionToken, long expectedTableRevision) implements CustomPacketPayload {
    public static final int MAX_ENCODED_BYTES = 24;
    public static final Type<ResearchTableCopyPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("thaumcraft", "research_table_copy"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ResearchTableCopyPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override public ResearchTableCopyPayload decode(RegistryFriendlyByteBuf buffer) {
            try {
                int size = buffer.readVarInt();
                if (size <= 0 || size > MAX_ENCODED_BYTES || size != buffer.readableBytes()) throw new DecoderException("Invalid research-copy frame");
                var body = new RegistryFriendlyByteBuf(buffer.readSlice(size), buffer.registryAccess());
                var payload = new ResearchTableCopyPayload(body.readVarInt(), body.readLong(), body.readLong());
                if (body.isReadable()) throw new DecoderException("Trailing research-copy bytes");
                return payload;
            } catch (DecoderException failure) { throw failure; }
            catch (RuntimeException failure) { throw new DecoderException("Invalid research-copy intent", failure); }
        }
        @Override public void encode(RegistryFriendlyByteBuf buffer, ResearchTableCopyPayload payload) {
            var body = new RegistryFriendlyByteBuf(Unpooled.buffer(MAX_ENCODED_BYTES, MAX_ENCODED_BYTES), buffer.registryAccess());
            try {
                body.writeVarInt(payload.containerId); body.writeLong(payload.sessionToken); body.writeLong(payload.expectedTableRevision);
                buffer.writeVarInt(body.readableBytes()); buffer.writeBytes(body);
            } finally { body.release(); }
        }
    };
    public ResearchTableCopyPayload {
        if (containerId < 0 || containerId > 255 || sessionToken <= 0 || expectedTableRevision < 0) throw new IllegalArgumentException("Invalid research-copy intent");
    }
    @Override public Type<ResearchTableCopyPayload> type() { return TYPE; }
}
