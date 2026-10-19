package thaumcraft.common.lib.network;

import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.common.lib.research.ResearchNoteData;
import thaumcraft.common.tiles.TileResearchTable;

/** Shared table generation/bonus only; note/tool use native menu sync, actor pools stay private. */
public record ResearchTableStatePayload(int containerId, long sessionToken, long tableRevision,
                                        Map<String, Integer> bonusPools) implements CustomPacketPayload {
    public static final int MAX_ENCODED_BYTES = 40_960;
    public static final Type<ResearchTableStatePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("thaumcraft", "research_table_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ResearchTableStatePayload> STREAM_CODEC = new StreamCodec<>() {
        @Override public ResearchTableStatePayload decode(RegistryFriendlyByteBuf buffer) {
            try {
                int start = buffer.readerIndex();
                int bytes = buffer.readVarInt();
                if (bytes <= 0 || bytes > MAX_ENCODED_BYTES - (buffer.readerIndex() - start)
                        || bytes != buffer.readableBytes()) throw new DecoderException("Invalid table state frame");
                RegistryFriendlyByteBuf body = new RegistryFriendlyByteBuf(buffer.readSlice(bytes), buffer.registryAccess());
                int menu = body.readVarInt();
                long session = body.readLong();
                long revision = body.readLong();
                int count = body.readVarInt();
                if (count < 0 || count > TileResearchTable.MAX_BONUS_ASPECTS || count > body.readableBytes() / 3) {
                    throw new DecoderException("Invalid table bonus count");
                }
                Map<String, Integer> bonus = new LinkedHashMap<>();
                for (int index = 0; index < count; index++) {
                    String aspect = body.readUtf(ResearchNoteData.MAX_ASPECT_LENGTH);
                    int amount = body.readVarInt();
                    if (bonus.putIfAbsent(aspect, amount) != null) throw new DecoderException("Duplicate table bonus aspect");
                }
                if (body.isReadable()) throw new DecoderException("Trailing table state bytes");
                return new ResearchTableStatePayload(menu, session, revision, bonus);
            } catch (DecoderException failure) {
                throw failure;
            } catch (RuntimeException failure) {
                throw new DecoderException("Invalid research table state", failure);
            }
        }

        @Override public void encode(RegistryFriendlyByteBuf buffer, ResearchTableStatePayload payload) {
            RegistryFriendlyByteBuf body = new RegistryFriendlyByteBuf(Unpooled.buffer(256, MAX_ENCODED_BYTES), buffer.registryAccess());
            try {
                body.writeVarInt(payload.containerId);
                body.writeLong(payload.sessionToken);
                body.writeLong(payload.tableRevision);
                body.writeVarInt(payload.bonusPools.size());
                payload.bonusPools.forEach((aspect, amount) -> {
                    body.writeUtf(aspect, ResearchNoteData.MAX_ASPECT_LENGTH);
                    body.writeVarInt(amount);
                });
                if (body.readableBytes() > MAX_ENCODED_BYTES - 3) throw new EncoderException("Table state exceeds byte budget");
                buffer.writeVarInt(body.readableBytes());
                buffer.writeBytes(body);
            } finally {
                body.release();
            }
        }
    };

    public ResearchTableStatePayload {
        if (containerId < 0 || containerId > 255 || sessionToken <= 0 || tableRevision < 0) {
            throw new IllegalArgumentException("Invalid table state generation");
        }
        bonusPools = TileResearchTable.canonicalBonusPools(bonusPools);
    }

    @Override public Type<ResearchTableStatePayload> type() { return TYPE; }
}
