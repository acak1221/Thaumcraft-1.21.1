package thaumcraft.common.lib.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Original int entityId + signed short charge/max; eight bytes, S2C only. */
public record RunicChargePayload(int source, short charge, short maximum) implements CustomPacketPayload {
    public static final Type<RunicChargePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("thaumcraft","runic_charge"));
    public static final StreamCodec<RegistryFriendlyByteBuf,RunicChargePayload> STREAM_CODEC = StreamCodec.of((buffer,payload)-> {
        buffer.writeInt(payload.source); buffer.writeShort(payload.charge); buffer.writeShort(payload.maximum);
    },buffer->new RunicChargePayload(buffer.readInt(),buffer.readShort(),buffer.readShort()));
    @Override public Type<RunicChargePayload> type() { return TYPE; }
}
