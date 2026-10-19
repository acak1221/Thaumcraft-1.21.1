package thaumcraft.common.lib.network;

import io.netty.handler.codec.DecoderException;
import java.util.Objects;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;

/** One byte of item-key intent; subject/markers come only from the actual held bell. */
public record GolemBellResetPayload(InteractionHand hand) implements CustomPacketPayload {
    public static final Type<GolemBellResetPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("thaumcraft", "golem_bell_reset"));
    public static final StreamCodec<RegistryFriendlyByteBuf, GolemBellResetPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> buffer.writeByte(payload.hand == InteractionHand.MAIN_HAND ? 0 : 1), buffer -> {
                if (buffer.readableBytes() != 1) throw new DecoderException("Invalid bell reset length");
                int hand = buffer.readUnsignedByte();
                if (hand > 1) throw new DecoderException("Invalid bell reset hand");
                return new GolemBellResetPayload(hand == 0 ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND);
            });
    public GolemBellResetPayload { Objects.requireNonNull(hand); }
    @Override public Type<GolemBellResetPayload> type() { return TYPE; }
}
