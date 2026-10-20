package thaumcraft.common.lib.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Original PacketFXSonic contains only the source entity id. There is no client-to-server counterpart. */
public record GuardianSonicPayload(int source) implements CustomPacketPayload {
    public static final Type<GuardianSonicPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("thaumcraft", "guardian_sonic"));
    public static final StreamCodec<RegistryFriendlyByteBuf, GuardianSonicPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> buffer.writeInt(payload.source), buffer -> new GuardianSonicPayload(buffer.readInt()));
    @Override public Type<GuardianSonicPayload> type() { return TYPE; }
}
