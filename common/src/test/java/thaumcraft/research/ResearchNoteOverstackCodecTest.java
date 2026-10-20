package thaumcraft.research;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import net.minecraft.SharedConstants;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.Bootstrap;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumcraft.common.lib.research.ResearchNoteOverstackCodec;

/**
 * Research notes alone may be saved above the vanilla stack-count cap of 99 (copying grows a note in place). Everything else must go
 * through the vanilla codec untouched. The note path itself needs the registered note item and is covered in the game tests.
 */
class ResearchNoteOverstackCodecTest {
    private static RegistryOps<JsonElement> ops;
    private static Codec<ItemStack> wrapped;

    @BeforeAll static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        ops = RegistryOps.create(JsonOps.INSTANCE, VanillaRegistries.createLookup());
        wrapped = ResearchNoteOverstackCodec.wrap(ItemStack.CODEC);
    }

    private static JsonElement stack(String count) {
        return JsonParser.parseString("{\"id\":\"minecraft:stone\"" + (count == null ? "" : ",\"count\":" + count) + "}");
    }

    @Test void aCountUpToTheVanillaCapDecodesExactlyAsVanillaDoes() {
        for (String count : new String[] {"1", "64", "99"}) {
            var plain = ItemStack.CODEC.decode(ops, stack(count)).getOrThrow();
            var viaWrapper = wrapped.decode(ops, stack(count)).getOrThrow();
            assertTrue(ItemStack.matches(plain.getFirst(), viaWrapper.getFirst()), count);
            assertEquals(Integer.parseInt(count), viaWrapper.getFirst().getCount());
        }
    }

    @Test void aMissingCountDefaultsToOneAsInVanilla() {
        assertEquals(1, wrapped.decode(ops, stack(null)).getOrThrow().getFirst().getCount());
    }

    @Test void anOversizedCountOnAnythingButANoteIsAnErrorAndNeverAWideStack() {
        for (String count : new String[] {"100", "5000", "2147483647"}) {
            var result = wrapped.decode(ops, stack(count));
            assertTrue(result.isError(), count);
            assertTrue(result.error().orElseThrow().message().contains("research notes"), result.error().orElseThrow().message());
        }
    }

    @Test void aFractionalOrMalformedCountIsLeftToVanillaToJudge() {
        for (String count : new String[] {"150.5", "\"many\"", "-3", "0"}) {
            assertEquals(ItemStack.CODEC.decode(ops, stack(count)).isError(), wrapped.decode(ops, stack(count)).isError(), count);
        }
    }

    @Test void inputThatIsNotAMapIsLeftToVanilla() {
        for (String input : new String[] {"\"minecraft:stone\"", "17", "[1,2]", "null"}) {
            JsonElement json = JsonParser.parseString(input);
            assertEquals(ItemStack.CODEC.decode(ops, json).isError(), wrapped.decode(ops, json).isError(), input);
        }
    }

    @Test void encodingAnOrdinaryStackMatchesVanillaForEveryCountTheCodecAccepts() {
        for (int count : new int[] {1, 2, 64, 99}) {
            ItemStack stack = new ItemStack(Items.STONE, count);
            JsonElement plain = ItemStack.CODEC.encodeStart(ops, stack).getOrThrow();
            JsonElement viaWrapper = wrapped.encodeStart(ops, stack).getOrThrow();
            assertEquals(plain, viaWrapper, "count " + count);
            assertEquals(count, ((JsonObject) viaWrapper).get("count") == null ? 1 : ((JsonObject) viaWrapper).get("count").getAsInt());
        }
    }

    @Test void encodingAnOversizedOrdinaryStackStillFailsLikeVanilla() {
        ItemStack stack = new ItemStack(Items.STONE, 100);
        assertTrue(ItemStack.CODEC.encodeStart(ops, stack).isError());
        assertTrue(wrapped.encodeStart(ops, stack).isError(), "only research notes may grow past 99");
    }

    @Test void aWrappedCodecRoundTripsAnOrdinaryStack() {
        ItemStack stack = new ItemStack(Items.DIAMOND, 12);
        var decoded = wrapped.decode(ops, wrapped.encodeStart(ops, stack).getOrThrow()).getOrThrow().getFirst();
        assertTrue(ItemStack.matches(stack, decoded));
    }
}
