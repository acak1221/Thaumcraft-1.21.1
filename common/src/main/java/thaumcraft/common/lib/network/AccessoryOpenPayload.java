package thaumcraft.common.lib.network;

import io.netty.handler.codec.DecoderException;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Open intent only: the client chooses no slot, item, charge or player. */
public record AccessoryOpenPayload() implements CustomPacketPayload {
    public static final Type<AccessoryOpenPayload> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("thaumcraft","accessories_open"));
    public static final StreamCodec<RegistryFriendlyByteBuf,AccessoryOpenPayload> STREAM_CODEC=StreamCodec.of((buf,p)->{},buf->{if(buf.readableBytes()!=0)throw new DecoderException("Invalid accessories-open length");return new AccessoryOpenPayload();});
    @Override public Type<AccessoryOpenPayload> type(){return TYPE;}
}
