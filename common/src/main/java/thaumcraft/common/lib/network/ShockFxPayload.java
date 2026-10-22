package thaumcraft.common.lib.network;

import java.util.Objects;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/** Only a committed server pulse's visual geometry/seed; never a C2S target or damage request. */
public record ShockFxPayload(ResourceLocation dimension, int casterId, Vec3 origin, Vec3 target, long seed, boolean impact,
                              Vec3 blockHit, Vec3 entityHit)
        implements CustomPacketPayload {
    public static final Type<ShockFxPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("thaumcraft", "shock_fx"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ShockFxPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeResourceLocation(payload.dimension);
                buffer.writeVarInt(payload.casterId);
                buffer.writeDouble(payload.origin.x); buffer.writeDouble(payload.origin.y); buffer.writeDouble(payload.origin.z);
                buffer.writeDouble(payload.target.x); buffer.writeDouble(payload.target.y); buffer.writeDouble(payload.target.z);
                buffer.writeLong(payload.seed);
                buffer.writeBoolean(payload.impact);
                writeOptional(buffer, payload.blockHit);
                writeOptional(buffer, payload.entityHit);
            }, buffer -> new ShockFxPayload(buffer.readResourceLocation(), buffer.readVarInt(),
                    new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble()),
                    new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble()), buffer.readLong(), buffer.readBoolean(),
                    readOptional(buffer), readOptional(buffer)));

    private static void writeOptional(RegistryFriendlyByteBuf buffer, Vec3 point) {
        buffer.writeBoolean(point != null);
        if (point != null) {
            buffer.writeDouble(point.x); buffer.writeDouble(point.y); buffer.writeDouble(point.z);
        }
    }

    private static Vec3 readOptional(RegistryFriendlyByteBuf buffer) {
        return buffer.readBoolean() ? new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble()) : null;
    }

    public ShockFxPayload {
        Objects.requireNonNull(dimension, "dimension");
        if (casterId < 0 || !validGeometry(origin, target))
            throw new IllegalArgumentException("Invalid Shock visual geometry");
        if (blockHit != null && !finite(blockHit)) throw new IllegalArgumentException("Invalid Shock block hit point");
        if (entityHit != null && !finite(entityHit)) throw new IllegalArgumentException("Invalid Shock entity hit point");
    }

    /** The payload of the chain lightning, which has no hit points of its own. */
    public static ShockFxPayload chain(ResourceLocation dimension, int casterId, Vec3 origin, Vec3 target, long seed) {
        return new ShockFxPayload(dimension, casterId, origin, target, seed, false, null, null);
    }

    // Geometry bound is NOT gameplay reach: the midpoint of a large native
    // entity can lie beyond the20-block ray which intersected its expanded box.
    static boolean validGeometry(Vec3 origin, Vec3 target) {
        return finite(origin) && finite(target) && origin.distanceToSqr(target) <= 64 * 64;
    }

    private static boolean finite(Vec3 point) {
        return point != null && Double.isFinite(point.x) && Double.isFinite(point.y) && Double.isFinite(point.z)
                && Math.abs(point.x) <= 30_000_032 && Math.abs(point.y) <= 30_000_032 && Math.abs(point.z) <= 30_000_032;
    }

    @Override public Type<ShockFxPayload> type() { return TYPE; }
}
