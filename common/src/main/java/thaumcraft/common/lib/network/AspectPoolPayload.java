package thaumcraft.common.lib.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Original signed-short delta/queried total remain feedback; full native snapshot is the sole knowledge authority. */
public record AspectPoolPayload(String key,short amount,short total) implements CustomPacketPayload {
    public static final Type<AspectPoolPayload> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("thaumcraft","aspect_pool"));
    public static final StreamCodec<RegistryFriendlyByteBuf,AspectPoolPayload> STREAM_CODEC=StreamCodec.of(
            (buffer,value)-> { buffer.writeUtf(value.key,128); buffer.writeShort(value.amount); buffer.writeShort(value.total); },
            buffer->new AspectPoolPayload(buffer.readUtf(128),buffer.readShort(),buffer.readShort()));
    public AspectPoolPayload { if(key==null||key.isEmpty()||key.length()>128) throw new IllegalArgumentException("Invalid aspect message key"); }
    @Override public Type<AspectPoolPayload> type(){return TYPE;}
}
