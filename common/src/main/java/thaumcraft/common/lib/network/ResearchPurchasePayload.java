package thaumcraft.common.lib.network;

import java.util.Objects;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Client intent only. Sender, prices, balances and prerequisites are server-owned. */
public record ResearchPurchasePayload(String key) implements CustomPacketPayload {
    public static final int MAX_KEY_LENGTH = 128;
    public static final Type<ResearchPurchasePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("thaumcraft", "research_purchase"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ResearchPurchasePayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> buffer.writeUtf(payload.key, MAX_KEY_LENGTH),
            buffer -> new ResearchPurchasePayload(buffer.readUtf(MAX_KEY_LENGTH)));

    public ResearchPurchasePayload {
        Objects.requireNonNull(key, "key");
        if (key.length() > MAX_KEY_LENGTH) throw new IllegalArgumentException("Research key exceeds wire limit");
    }

    @Override public Type<ResearchPurchasePayload> type() { return TYPE; }
}
