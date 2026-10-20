package thaumcraft.common.lib.network;

import io.netty.handler.codec.DecoderException;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** The press of the misc toggle key (original PacketItemKeyToServer key 1): no data, the held shovel decides everything. */
public record ShovelOrientationPayload() implements CustomPacketPayload {
    public static final Type<ShovelOrientationPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("thaumcraft", "shovel_orientation"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ShovelOrientationPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {}, buffer -> {
                if (buffer.readableBytes() != 0) throw new DecoderException("Invalid shovel orientation length");
                return new ShovelOrientationPayload();
            });
    @Override public Type<ShovelOrientationPayload> type() { return TYPE; }
}
