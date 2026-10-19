package thaumcraft.common.lib.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** A server-authorized open action, sent after the knowledge snapshot. */
public record OpenThaumonomiconPayload() implements CustomPacketPayload {
    public static final OpenThaumonomiconPayload INSTANCE = new OpenThaumonomiconPayload();
    public static final Type<OpenThaumonomiconPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("thaumcraft", "open_thaumonomicon"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenThaumonomiconPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);
    @Override public Type<OpenThaumonomiconPayload> type() { return TYPE; }
}
