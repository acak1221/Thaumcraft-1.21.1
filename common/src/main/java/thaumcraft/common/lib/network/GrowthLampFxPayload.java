package thaumcraft.common.lib.network;

import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Original fixed green block sparkle after an observed plant-state change; S2C effects only. */
public record GrowthLampFxPayload(ResourceLocation dimension, BlockPos target) implements CustomPacketPayload {
    public static final Type<GrowthLampFxPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("thaumcraft", "growth_lamp_fx"));
    public static final StreamCodec<RegistryFriendlyByteBuf, GrowthLampFxPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> { buffer.writeResourceLocation(payload.dimension); buffer.writeBlockPos(payload.target); },
            buffer -> new GrowthLampFxPayload(buffer.readResourceLocation(), buffer.readBlockPos()));
    public GrowthLampFxPayload { Objects.requireNonNull(dimension); target = Objects.requireNonNull(target).immutable(); }
    @Override public Type<GrowthLampFxPayload> type() { return TYPE; }
}
