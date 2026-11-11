package thaumcraft.common.lib.inventoryscan;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Correlated server receipt; knowledge and discoveries use Thaumcraft's own sync. */
public record ScanReply(int requestId, int progress, int status) implements CustomPacketPayload {
    public static final int SCANNING = 0;
    public static final int SUCCESS = 1;
    public static final int FAILED = 2;
    public static final int ALREADY_SCANNED = 3;
    public static final int CANCELLED = 4;
    public static final Type<ScanReply> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("thaumcraft", "inventory_scan_reply"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ScanReply> STREAM_CODEC = StreamCodec.of(
            (buf, packet) -> {
                buf.writeVarInt(packet.requestId()); buf.writeVarInt(packet.progress()); buf.writeVarInt(packet.status());
            },
            buf -> new ScanReply(buf.readVarInt(), buf.readVarInt(), buf.readVarInt()));
    @Override public Type<ScanReply> type() { return TYPE; }
}
