package thaumcraft.common.lib.network;

import io.netty.handler.codec.DecoderException;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** A single G press. Neither dimensions, focus ids, nor block coordinates come from the client. */
public record WandArchitectPayload() implements CustomPacketPayload {
    public static final Type<WandArchitectPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("thaumcraft", "architect_toggle"));
    public static final StreamCodec<RegistryFriendlyByteBuf, WandArchitectPayload> STREAM_CODEC = StreamCodec.of((buffer, payload) -> {}, buffer -> {
        if (buffer.readableBytes() != 0) throw new DecoderException("Unexpected Architect key payload");
        return new WandArchitectPayload();
    });
    @Override public Type<WandArchitectPayload> type() { return TYPE; }
}
