package thaumcraft.common.lib.network;

import io.netty.handler.codec.DecoderException;
import java.util.Objects;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Bounded recipe intent: no client inventory, ingredient list, output or price. */
public record RecipeTransferPayload(int containerId, ResourceLocation recipeId, boolean maximum, boolean fromBook)
        implements CustomPacketPayload {
    public static final Type<RecipeTransferPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("thaumcraft", "recipe_transfer"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RecipeTransferPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, value) -> {
                buffer.writeByte(value.containerId());
                buffer.writeUtf(value.recipeId().toString(), 256);
                buffer.writeByte((value.maximum() ? 1 : 0) | (value.fromBook() ? 2 : 0));
            }, buffer -> {
                if (buffer.readableBytes() > 1024) throw new DecoderException("Recipe transfer intent too large");
                int id = buffer.readUnsignedByte();
                ResourceLocation recipe = ResourceLocation.tryParse(buffer.readUtf(256));
                int flags = buffer.readUnsignedByte();
                if (recipe == null || flags > 3 || buffer.readableBytes() != 0) throw new DecoderException("Invalid recipe transfer intent");
                return new RecipeTransferPayload(id, recipe, (flags & 1) != 0, (flags & 2) != 0);
            });

    public RecipeTransferPayload {
        Objects.requireNonNull(recipeId);
        if (containerId < 0 || containerId > 255 || recipeId.toString().length() > 256)
            throw new IllegalArgumentException("Invalid recipe transfer context");
    }

    @Override public Type<RecipeTransferPayload> type() { return TYPE; }
}
