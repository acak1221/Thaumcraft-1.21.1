package thaumcraft.common.lib.network;

import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import java.util.Objects;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.common.lib.research.ResearchNoteData;
import thaumcraft.common.lib.research.ResearchNoteEditing;
import thaumcraft.common.tiles.TileResearchTable;

/** Bounded intent only: no world/player address, note NBT, price, pools or random outcomes. */
public record ResearchTableEditPayload(int containerId, long sessionToken, long expectedTableRevision,
                                       ResearchNoteEditing.Action action, int q, int r, String aspect)
        implements CustomPacketPayload {
    public static final int MAX_ENCODED_BYTES = 256;
    public static final Type<ResearchTableEditPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("thaumcraft", "research_table_edit"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ResearchTableEditPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override public ResearchTableEditPayload decode(RegistryFriendlyByteBuf buffer) {
            try {
                int start = buffer.readerIndex();
                int bytes = buffer.readVarInt();
                if (bytes <= 0 || bytes > MAX_ENCODED_BYTES - (buffer.readerIndex() - start)
                        || bytes != buffer.readableBytes()) throw new DecoderException("Invalid table edit frame");
                RegistryFriendlyByteBuf body = new RegistryFriendlyByteBuf(buffer.readSlice(bytes), buffer.registryAccess());
                int menu = body.readVarInt();
                long session = body.readLong();
                long revision = body.readLong();
                int kind = body.readUnsignedByte();
                ResearchNoteEditing.Action action = switch (kind) {
                    case 0 -> ResearchNoteEditing.Action.PLACE;
                    case 1 -> ResearchNoteEditing.Action.ERASE;
                    default -> throw new DecoderException("Unknown table edit action");
                };
                int q = body.readByte();
                int r = body.readByte();
                String aspect = action == ResearchNoteEditing.Action.PLACE ? body.readUtf(ResearchNoteData.MAX_ASPECT_LENGTH) : null;
                if (body.isReadable()) throw new DecoderException("Trailing table edit bytes");
                return new ResearchTableEditPayload(menu, session, revision, action, q, r, aspect);
            } catch (DecoderException failure) {
                throw failure;
            } catch (RuntimeException failure) {
                throw new DecoderException("Invalid research table edit", failure);
            }
        }

        @Override public void encode(RegistryFriendlyByteBuf buffer, ResearchTableEditPayload payload) {
            RegistryFriendlyByteBuf body = new RegistryFriendlyByteBuf(Unpooled.buffer(160, MAX_ENCODED_BYTES), buffer.registryAccess());
            try {
                body.writeVarInt(payload.containerId);
                body.writeLong(payload.sessionToken);
                body.writeLong(payload.expectedTableRevision);
                body.writeByte(payload.action == ResearchNoteEditing.Action.PLACE ? 0 : 1);
                body.writeByte(payload.q);
                body.writeByte(payload.r);
                if (payload.action == ResearchNoteEditing.Action.PLACE) body.writeUtf(payload.aspect, ResearchNoteData.MAX_ASPECT_LENGTH);
                // At most two frame-length bytes at this budget; UTF-8 byte
                // length is independently checked by the record constructor.
                if (body.readableBytes() > MAX_ENCODED_BYTES - 2) throw new EncoderException("Table edit exceeds byte budget");
                buffer.writeVarInt(body.readableBytes());
                buffer.writeBytes(body);
            } finally {
                body.release();
            }
        }
    };

    public ResearchTableEditPayload {
        if (containerId < 0 || containerId > 255 || sessionToken <= 0 || expectedTableRevision < 0) {
            throw new IllegalArgumentException("Invalid table menu generation");
        }
        Objects.requireNonNull(action, "action");
        int radius = ResearchNoteData.MAX_RADIUS;
        if (q < -radius || q > radius || r < -radius || r > radius || Math.abs((long) q + r) > radius) {
            throw new IllegalArgumentException("Invalid table edit coordinate");
        }
        if (action == ResearchNoteEditing.Action.PLACE ? !TileResearchTable.isCanonicalBonusTag(aspect) : aspect != null) {
            throw new IllegalArgumentException("Invalid table edit aspect");
        }
    }

    @Override public Type<ResearchTableEditPayload> type() { return TYPE; }
}
