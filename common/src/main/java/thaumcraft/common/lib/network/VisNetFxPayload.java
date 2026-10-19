package thaumcraft.common.lib.network;

import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** S2C appearance only, AFTER a real grant. Full coordinates/dimension and a
 * validated RGB replace original shorts/unchecked primal-index decoding. */
public record VisNetFxPayload(ResourceLocation dimension, BlockPos origin, BlockPos target, int rgb)
        implements CustomPacketPayload {
    public static final Type<VisNetFxPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("thaumcraft", "vis_net_fx"));
    public static final StreamCodec<RegistryFriendlyByteBuf, VisNetFxPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeResourceLocation(payload.dimension);
                position(buffer, payload.origin); position(buffer, payload.target);
                buffer.writeInt(payload.rgb);
            }, buffer -> new VisNetFxPayload(buffer.readResourceLocation(), position(buffer), position(buffer), buffer.readInt()));

    public VisNetFxPayload {
        Objects.requireNonNull(dimension, "dimension");
        Objects.requireNonNull(origin, "origin"); Objects.requireNonNull(target, "target");
        origin = origin.immutable(); target = target.immutable();
        if (!validPosition(origin) || !validPosition(target) || origin.distSqr(target) > 64 * 64
                || origin.equals(target) || rgb < 0 || rgb > 0xffffff) throw new IllegalArgumentException("Invalid VisNet visual geometry/color");
    }

    private static void position(RegistryFriendlyByteBuf buffer, BlockPos pos) {
        buffer.writeInt(pos.getX()); buffer.writeInt(pos.getY()); buffer.writeInt(pos.getZ());
    }
    private static BlockPos position(RegistryFriendlyByteBuf buffer) { return new BlockPos(buffer.readInt(), buffer.readInt(), buffer.readInt()); }
    private static boolean validPosition(BlockPos pos) {
        return Math.abs((long) pos.getX()) <= 30_000_000 && Math.abs((long) pos.getZ()) <= 30_000_000
                && Math.abs((long) pos.getY()) <= 20_000_000;
    }
    @Override public Type<VisNetFxPayload> type() { return TYPE; }
}
