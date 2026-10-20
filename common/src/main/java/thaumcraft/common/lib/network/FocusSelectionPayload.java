package thaumcraft.common.lib.network;

import io.netty.handler.codec.DecoderException;
import java.util.Objects;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import thaumcraft.common.items.wands.FocusSelectionSources;

/** Two legacy bytes for loose/remove intent; marker127 plus an unsigned short
 * addresses a physical pouch cell. No client item/components are accepted. */
public record FocusSelectionPayload(InteractionHand hand, int inventorySlot) implements CustomPacketPayload {
    public static final Type<FocusSelectionPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("thaumcraft", "focus_selection"));
    public static final StreamCodec<RegistryFriendlyByteBuf, FocusSelectionPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeByte(payload.hand == InteractionHand.MAIN_HAND ? 0 : 1);
                if(payload.inventorySlot<36)buffer.writeByte(payload.inventorySlot);
                else {buffer.writeByte(127);buffer.writeShort(payload.inventorySlot);}
            }, buffer -> {
                if (buffer.readableBytes() != 2&&buffer.readableBytes()!=4) throw new DecoderException("Invalid focus selection length");
                int hand = buffer.readUnsignedByte();
                int slot = buffer.readByte();
                if(slot==127&&buffer.readableBytes()==2) {
                    slot=buffer.readUnsignedShort();
                    if(slot<36)throw new DecoderException("Noncanonical loose focus source");
                }
                if (hand > 1 || buffer.isReadable() || !FocusSelectionSources.valid(slot)) throw new DecoderException("Invalid focus selection intent");
                return new FocusSelectionPayload(hand == 0 ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND, slot);
            });

    public FocusSelectionPayload {
        Objects.requireNonNull(hand, "hand");
        if (!FocusSelectionSources.valid(inventorySlot)) throw new IllegalArgumentException("Invalid physical focus source");
    }

    @Override public Type<FocusSelectionPayload> type() { return TYPE; }
}
