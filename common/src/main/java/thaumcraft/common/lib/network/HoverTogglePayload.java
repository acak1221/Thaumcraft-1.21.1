package thaumcraft.common.lib.network;

import io.netty.handler.codec.DecoderException;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Empty toggle intent: source/actor/slot/fuel/flight rights are server-owned. */
public record HoverTogglePayload() implements CustomPacketPayload {
    public static final Type<HoverTogglePayload> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("thaumcraft","hover_toggle"));
    public static final StreamCodec<RegistryFriendlyByteBuf,HoverTogglePayload> STREAM_CODEC=StreamCodec.of((buffer,payload)->{},buffer->{if(buffer.isReadable())throw new DecoderException("Invalid hover intent length");return new HoverTogglePayload();});
    @Override public Type<HoverTogglePayload> type() {return TYPE;}
}
