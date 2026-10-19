package thaumcraft.common.lib.network;

import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.common.lib.research.ResearchNoteData;
import thaumcraft.common.tiles.TileResearchTable;

/** Bounded menu intent, never a client-selected player/world, bonus flag or price. */
public record ResearchTableCombinePayload(int containerId, long sessionToken, long expectedTableRevision,
                                          String left, String right) implements CustomPacketPayload {
    public static final int MAX_ENCODED_BYTES = 384;
    public static final Type<ResearchTableCombinePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("thaumcraft", "research_table_combine"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ResearchTableCombinePayload> STREAM_CODEC = new StreamCodec<>() {
        @Override public ResearchTableCombinePayload decode(RegistryFriendlyByteBuf buffer) {
            try {
                int start = buffer.readerIndex();
                int bytes = buffer.readVarInt();
                if (bytes <= 0 || bytes > MAX_ENCODED_BYTES - (buffer.readerIndex() - start)
                        || bytes != buffer.readableBytes()) throw new DecoderException("Invalid table combine frame");
                RegistryFriendlyByteBuf body = new RegistryFriendlyByteBuf(buffer.readSlice(bytes), buffer.registryAccess());
                var payload = new ResearchTableCombinePayload(body.readVarInt(), body.readLong(), body.readLong(),
                        body.readUtf(ResearchNoteData.MAX_ASPECT_LENGTH), body.readUtf(ResearchNoteData.MAX_ASPECT_LENGTH));
                if (body.isReadable()) throw new DecoderException("Trailing table combine bytes");
                return payload;
            } catch (DecoderException failure) {
                throw failure;
            } catch (RuntimeException failure) {
                throw new DecoderException("Invalid research table combination", failure);
            }
        }

        @Override public void encode(RegistryFriendlyByteBuf buffer, ResearchTableCombinePayload payload) {
            RegistryFriendlyByteBuf body = new RegistryFriendlyByteBuf(Unpooled.buffer(160, MAX_ENCODED_BYTES), buffer.registryAccess());
            try {
                body.writeVarInt(payload.containerId);
                body.writeLong(payload.sessionToken);
                body.writeLong(payload.expectedTableRevision);
                body.writeUtf(payload.left, ResearchNoteData.MAX_ASPECT_LENGTH);
                body.writeUtf(payload.right, ResearchNoteData.MAX_ASPECT_LENGTH);
                if (body.readableBytes() > MAX_ENCODED_BYTES - 2) throw new EncoderException("Table combine exceeds byte budget");
                buffer.writeVarInt(body.readableBytes());
                buffer.writeBytes(body);
            } finally {
                body.release();
            }
        }
    };

    public ResearchTableCombinePayload {
        if (containerId < 0 || containerId > 255 || sessionToken <= 0 || expectedTableRevision < 0
                || !TileResearchTable.isCanonicalBonusTag(left) || !TileResearchTable.isCanonicalBonusTag(right)) {
            throw new IllegalArgumentException("Invalid table combination intent");
        }
    }

    @Override public Type<ResearchTableCombinePayload> type() { return TYPE; }
}
