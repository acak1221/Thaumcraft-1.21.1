package thaumcraft.common.lib.network;

import java.util.Objects;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.common.lib.research.ScanRunes;

/**
 * The S2C rune of the original thaumometer scan.
 *
 * <p>The original drew the rune on the client ({@code thaumcraft/common/items/relics/ItemThaumometer.java:92,103,128}
 * calls {@code Thaumcraft.proxy.blockRunes}, whose client half is
 * {@code thaumcraft/client/ClientProxy.java:918-922}). The native scan is server owned, so the
 * server sends the target of the rune and the client draws the particle and its colour, exactly
 * as the original drew the colour from the client world random.</p>
 */
public record ScanRuneFxPayload(ResourceLocation dimension, double x, double y, double z,
                                 int duration, float gravity) implements CustomPacketPayload {
    public static final Type<ScanRuneFxPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("thaumcraft", "scan_rune"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ScanRuneFxPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeResourceLocation(payload.dimension);
                buffer.writeDouble(payload.x); buffer.writeDouble(payload.y); buffer.writeDouble(payload.z);
                buffer.writeVarInt(payload.duration);
                buffer.writeFloat(payload.gravity);
            },
            buffer -> new ScanRuneFxPayload(buffer.readResourceLocation(), buffer.readDouble(),
                    buffer.readDouble(), buffer.readDouble(), buffer.readVarInt(), buffer.readFloat()));

    public ScanRuneFxPayload {
        Objects.requireNonNull(dimension, "dimension");
        if (duration <= 0) throw new IllegalArgumentException("A scan rune needs a positive duration");
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z))
            throw new IllegalArgumentException("A scan rune needs a finite position");
    }

    /** The rune of a block or of a node of the original scan. */
    public static ScanRuneFxPayload of(ResourceLocation dimension, ScanRunes.Rune rune) {
        return new ScanRuneFxPayload(dimension, rune.x(), rune.y(), rune.z(), rune.duration(), rune.gravity());
    }

    @Override public Type<ScanRuneFxPayload> type() { return TYPE; }
}
