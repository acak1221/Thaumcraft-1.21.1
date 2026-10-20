package thaumcraft.common.lib.network;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
/** The original three PacketMiscEvent kinds, server-to-client only. */
public record WarpEventPayload(int event) implements CustomPacketPayload {
    public static final Type<WarpEventPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("thaumcraft", "warp_event"));
    public static final StreamCodec<RegistryFriendlyByteBuf, WarpEventPayload> STREAM_CODEC = StreamCodec.of((buffer, payload) -> buffer.writeShort(payload.event), buffer -> new WarpEventPayload(buffer.readShort()));
    public WarpEventPayload { if (event < 0 || event > 2) throw new IllegalArgumentException("Unknown warp event"); }
    @Override public Type<WarpEventPayload> type() { return TYPE; }
}
