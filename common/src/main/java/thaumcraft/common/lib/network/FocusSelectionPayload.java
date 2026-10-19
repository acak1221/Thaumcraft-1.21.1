package thaumcraft.common.lib.network;

import io.netty.handler.codec.DecoderException;
import java.util.Objects;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;

/** Exactly two bytes of intent: hand and actual main-inventory slot (-1 removes). */
public record FocusSelectionPayload(InteractionHand hand, int inventorySlot) implements CustomPacketPayload {
    public static final Type<FocusSelectionPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("thaumcraft", "focus_selection"));
    public static final StreamCodec<RegistryFriendlyByteBuf, FocusSelectionPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeByte(payload.hand == InteractionHand.MAIN_HAND ? 0 : 1);
                buffer.writeByte(payload.inventorySlot);
            }, buffer -> {
                if (buffer.readableBytes() != 2) throw new DecoderException("Invalid focus selection length");
                int hand = buffer.readUnsignedByte();
                int slot = buffer.readByte();
                if (hand > 1 || slot < -1 || slot > 35) throw new DecoderException("Invalid focus selection intent");
                return new FocusSelectionPayload(hand == 0 ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND, slot);
            });

    public FocusSelectionPayload {
        Objects.requireNonNull(hand, "hand");
        if (inventorySlot < -1 || inventorySlot > 35) throw new IllegalArgumentException("Focus inventory slot must be -1 or 0..35");
    }

    @Override public Type<FocusSelectionPayload> type() { return TYPE; }
}
