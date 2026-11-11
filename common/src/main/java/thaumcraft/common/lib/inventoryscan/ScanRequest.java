package thaumcraft.common.lib.inventoryscan;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** A hover intent, never a client-supplied item, aspect list, reward or player. */
public record ScanRequest(int containerId, int slot, int requestId, boolean active) implements CustomPacketPayload {
    public static final Type<ScanRequest> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("thaumcraft", "inventory_scan_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ScanRequest> STREAM_CODEC = StreamCodec.of(
            (buf, packet) -> {
                buf.writeVarInt(packet.containerId()); buf.writeVarInt(packet.slot());
                buf.writeVarInt(packet.requestId()); buf.writeBoolean(packet.active());
            },
            buf -> new ScanRequest(buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readBoolean()));
    @Override public Type<ScanRequest> type() { return TYPE; }
}
