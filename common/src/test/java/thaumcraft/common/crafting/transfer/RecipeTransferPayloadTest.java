package thaumcraft.common.crafting.transfer;

import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import java.util.List;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import thaumcraft.common.lib.network.RecipeTransferPayload;
import static org.junit.jupiter.api.Assertions.*;

class RecipeTransferPayloadTest {
    private static final ResourceLocation RECIPE = ResourceLocation.parse("thaumcraft:arcane/arcane_stone");

    private static RegistryFriendlyByteBuf wire() {
        return new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
    }

    @Test void onlyBoundedRecipeIntentSurvivesTheNativeCodec() {
        for (int menu : List.of(0, 1, 255)) for (boolean maximum : List.of(false, true)) for (boolean fromBook : List.of(false, true)) {
            var intent = new RecipeTransferPayload(menu, RECIPE, maximum, fromBook);
            var buffer = wire();
            try {
                RecipeTransferPayload.STREAM_CODEC.encode(buffer, intent);
                assertTrue(buffer.readableBytes() < 64);
                assertEquals(intent, RecipeTransferPayload.STREAM_CODEC.decode(buffer));
                assertFalse(buffer.isReadable());
            } finally { buffer.release(); }
        }
    }

    @Test void constructorRejectsValuesThatWouldOtherwiseBeSilentlyTruncated() {
        for (int menu : List.of(-1, 256, Integer.MAX_VALUE))
            assertThrows(IllegalArgumentException.class, () -> new RecipeTransferPayload(menu, RECIPE, false, false));
        assertThrows(NullPointerException.class, () -> new RecipeTransferPayload(1, null, false, false));
        var oversized = ResourceLocation.fromNamespaceAndPath("thaumcraft", "a".repeat(257));
        assertThrows(IllegalArgumentException.class, () -> new RecipeTransferPayload(1, oversized, true, true));
    }

    @Test void forgedFlagsTrailingDataAndInvalidRecipeNamesCannotBeInterpretedAsInventoryInstructions() {
        for (int flags : List.of(4, 128, 255)) {
            var buffer = wire();
            try {
                buffer.writeByte(1); buffer.writeUtf(RECIPE.toString(), 256); buffer.writeByte(flags);
                assertThrows(DecoderException.class, () -> RecipeTransferPayload.STREAM_CODEC.decode(buffer));
            } finally { buffer.release(); }
        }
        var trailing = wire();
        try {
            RecipeTransferPayload.STREAM_CODEC.encode(trailing, new RecipeTransferPayload(1, RECIPE, true, false));
            trailing.writeInt(64); // A client cannot append quantities, result stacks or a replacement inventory.
            assertThrows(DecoderException.class, () -> RecipeTransferPayload.STREAM_CODEC.decode(trailing));
        } finally { trailing.release(); }
        var malformed = wire();
        try {
            malformed.writeByte(1); malformed.writeUtf("Not A Valid:Recipe", 256); malformed.writeByte(0);
            assertThrows(DecoderException.class, () -> RecipeTransferPayload.STREAM_CODEC.decode(malformed));
        } finally { malformed.release(); }
    }

    @Test void emptyTruncatedAndOversizedFramesFailBeforeProducingAnIntent() {
        for (int length : List.of(0, 1, 2, 1025)) {
            var buffer = wire();
            try {
                buffer.writeZero(length);
                assertThrows(RuntimeException.class, () -> RecipeTransferPayload.STREAM_CODEC.decode(buffer));
            } finally { buffer.release(); }
        }
    }
}
