package thaumcraft.common.lib.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Original PacketFXWispZap's two entity IDs; no gameplay is accepted from the client. */
public record WispZapPayload(int source,int target) implements CustomPacketPayload {
    public static final Type<WispZapPayload> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("thaumcraft","wisp_zap"));
    public static final StreamCodec<RegistryFriendlyByteBuf,WispZapPayload> STREAM_CODEC=StreamCodec.of((buf,payload) -> {buf.writeInt(payload.source);buf.writeInt(payload.target);},buf -> new WispZapPayload(buf.readInt(),buf.readInt()));
    @Override public Type<WispZapPayload> type() { return TYPE; }
}
