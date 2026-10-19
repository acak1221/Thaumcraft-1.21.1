package thaumcraft.common.lib.network;

import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/** Eligible SERVER channel geometry/cracks and paid-attempt outcome; never C2S targeting or debit. */
public record ExcavationFxPayload(ResourceLocation dimension, int casterId, Vec3 origin, Vec3 target,
        BlockPos blockPos, int crackStage, boolean impact, boolean completed, boolean success) implements CustomPacketPayload {
    public static final Type<ExcavationFxPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("thaumcraft", "excavation_fx"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ExcavationFxPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeResourceLocation(payload.dimension); buffer.writeVarInt(payload.casterId);
                buffer.writeDouble(payload.origin.x); buffer.writeDouble(payload.origin.y); buffer.writeDouble(payload.origin.z);
                buffer.writeDouble(payload.target.x); buffer.writeDouble(payload.target.y); buffer.writeDouble(payload.target.z);
                buffer.writeBlockPos(payload.blockPos); buffer.writeVarInt(payload.crackStage);
                buffer.writeBoolean(payload.impact); buffer.writeBoolean(payload.completed); buffer.writeBoolean(payload.success);
            }, buffer -> new ExcavationFxPayload(buffer.readResourceLocation(), buffer.readVarInt(),
                    new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble()),
                    new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble()), buffer.readBlockPos(), buffer.readVarInt(),
                    buffer.readBoolean(), buffer.readBoolean(), buffer.readBoolean()));

    public ExcavationFxPayload {
        Objects.requireNonNull(dimension, "dimension"); Objects.requireNonNull(blockPos, "blockPos");
        blockPos = blockPos.immutable();
        // >9 is intentional: inherited progress on a softer replacement block
        // can produce it, and native destroyBlockProgress removes that crack.
        if (casterId < 0 || !validGeometry(origin, target) || crackStage < -1
                || crackStage >= 0 && !impact || completed && !impact || success && !completed
                || impact && Vec3.atCenterOf(blockPos).distanceToSqr(target) > 3)
            throw new IllegalArgumentException("Invalid Excavation visual geometry/outcome");
    }

    static boolean validGeometry(Vec3 origin, Vec3 target) {
        return finite(origin) && finite(target) && origin.distanceToSqr(target) <= 16 * 16;
    }
    private static boolean finite(Vec3 point) {
        return point != null && Double.isFinite(point.x) && Double.isFinite(point.y) && Double.isFinite(point.z)
                && Math.abs(point.x) <= 30_000_032 && Math.abs(point.y) <= 30_000_032 && Math.abs(point.z) <= 30_000_032;
    }
    @Override public Type<ExcavationFxPayload> type() { return TYPE; }
}
