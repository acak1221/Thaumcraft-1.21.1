package thaumcraft.common.lib.network;

import java.util.Objects;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Primary-note intent: the connection and current inventory supply all other state. */
public record ResearchNoteRequestPayload(String key) implements CustomPacketPayload {
    public static final int MAX_KEY_LENGTH = 128;
    public static final Type<ResearchNoteRequestPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("thaumcraft", "research_note_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ResearchNoteRequestPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> buffer.writeUtf(payload.key, MAX_KEY_LENGTH),
            buffer -> new ResearchNoteRequestPayload(buffer.readUtf(MAX_KEY_LENGTH)));

    public ResearchNoteRequestPayload {
        Objects.requireNonNull(key, "key");
        if (key.length() > MAX_KEY_LENGTH) throw new IllegalArgumentException("Research key exceeds note request limit");
    }

    @Override public Type<ResearchNoteRequestPayload> type() { return TYPE; }
}
