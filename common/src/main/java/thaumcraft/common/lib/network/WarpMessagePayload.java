package thaumcraft.common.lib.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Original int requested delta, signed byte kind. This packet never mutates client knowledge. */
public record WarpMessagePayload(int delta, byte kind) implements CustomPacketPayload {
    public static final Type<WarpMessagePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("thaumcraft", "warp_message"));
    public static final StreamCodec<RegistryFriendlyByteBuf, WarpMessagePayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> { buffer.writeInt(payload.delta); buffer.writeByte(payload.kind); },
            buffer -> new WarpMessagePayload(buffer.readInt(), buffer.readByte()));
    @Override public Type<WarpMessagePayload> type() { return TYPE; }
}
