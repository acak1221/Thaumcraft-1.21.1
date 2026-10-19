package thaumcraft.common.lib.network;

import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** S2C visual cue for an already committed infusion action, never a crafting request. */
public record InfusionFxPayload(ResourceLocation dimension, BlockPos matrix, int event, BlockPos target, int data)
        implements CustomPacketPayload {
    public static final Type<InfusionFxPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("thaumcraft", "infusion_fx"));
    public static final StreamCodec<RegistryFriendlyByteBuf, InfusionFxPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeResourceLocation(payload.dimension);
                buffer.writeBlockPos(payload.matrix);
                buffer.writeByte(payload.event);
                buffer.writeBlockPos(payload.target);
                buffer.writeVarInt(payload.data);
            }, buffer -> new InfusionFxPayload(buffer.readResourceLocation(), buffer.readBlockPos(),
                    buffer.readUnsignedByte(), buffer.readBlockPos(), buffer.readVarInt()));

    public InfusionFxPayload {
        Objects.requireNonNull(dimension, "dimension");
        matrix = Objects.requireNonNull(matrix, "matrix").immutable();
        target = Objects.requireNonNull(target, "target").immutable();
        if (event < 0 || event > 7) throw new IllegalArgumentException("Unknown infusion visual event: " + event);
        if (event == 4 && (data < 0 || data > 5)) throw new IllegalArgumentException("Unknown infusion ejection effect: " + data);
    }

    @Override public Type<InfusionFxPayload> type() { return TYPE; }
}
