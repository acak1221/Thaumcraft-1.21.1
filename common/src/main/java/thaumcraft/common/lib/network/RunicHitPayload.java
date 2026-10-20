package thaumcraft.common.lib.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Original shield sphere victim/attacker ids, with -1/-2/-3 hemisphere modes. */
public record RunicHitPayload(int source,int target) implements CustomPacketPayload {
    public static final Type<RunicHitPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("thaumcraft","runic_hit"));
    public static final StreamCodec<RegistryFriendlyByteBuf,RunicHitPayload> STREAM_CODEC = StreamCodec.of((buffer,payload)-> {
        buffer.writeInt(payload.source); buffer.writeInt(payload.target);
    },buffer->new RunicHitPayload(buffer.readInt(),buffer.readInt()));
    @Override public Type<RunicHitPayload> type() { return TYPE; }
}
