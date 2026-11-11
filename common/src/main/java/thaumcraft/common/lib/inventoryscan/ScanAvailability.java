package thaumcraft.common.lib.inventoryscan;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Whether the server admits inventory scanning, sent once when a player joins.
 *
 * <p>The setting {@code inventory_scanning} is read on each side. Without this fact a client with the
 * setting on would offer scanning on a server that has it off: the tooltip of the thaumometer would
 * promise it and a hover would simply get no answer.</p>
 */
public record ScanAvailability(boolean enabled) implements CustomPacketPayload {
    public static final Type<ScanAvailability> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("thaumcraft", "inventory_scan_availability"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ScanAvailability> STREAM_CODEC = StreamCodec.of(
            (buf, packet) -> buf.writeBoolean(packet.enabled()), buf -> new ScanAvailability(buf.readBoolean()));
    @Override public Type<ScanAvailability> type() { return TYPE; }
}
