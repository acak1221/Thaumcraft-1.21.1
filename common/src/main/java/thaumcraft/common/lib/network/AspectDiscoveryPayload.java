package thaumcraft.common.lib.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Original PacketAspectDiscovery's canonical aspect key. */
public record AspectDiscoveryPayload(String key) implements CustomPacketPayload {
    public static final Type<AspectDiscoveryPayload> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("thaumcraft","aspect_discovery"));
    public static final StreamCodec<RegistryFriendlyByteBuf,AspectDiscoveryPayload> STREAM_CODEC=StreamCodec.of(
            (buffer,value)->buffer.writeUtf(value.key,128),buffer->new AspectDiscoveryPayload(buffer.readUtf(128)));
    public AspectDiscoveryPayload { if(key==null||key.isEmpty()||key.length()>128) throw new IllegalArgumentException("Invalid discovered-aspect key"); }
    @Override public Type<AspectDiscoveryPayload> type(){return TYPE;}
}
