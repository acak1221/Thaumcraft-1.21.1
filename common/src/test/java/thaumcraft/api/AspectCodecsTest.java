package thaumcraft.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;

/**
 * The codecs of {@link Aspect} and {@link AspectList}: what a data component, a data file or a packet of an add-on
 * holds when it holds aspects. The registry is global, so every test that registers an add-on aspect removes it again
 * (the 48-aspect assertions of the other test classes keep holding).
 */
class AspectCodecsTest {
    private final List<Aspect> registered = new ArrayList<>();

    @BeforeAll static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @AfterEach void unregister() {
        for (Aspect aspect : registered) Aspect.aspects.values().removeIf(value -> value == aspect);
        registered.clear();
        assertEquals(48, Aspect.aspects.size(), "test aspects must not leak into other tests");
    }

    private Aspect addonAspect() {
        String tag = "codec_test_" + UUID.randomUUID().toString().replace("-", "");
        Aspect aspect = new Aspect(tag, 0xB68CFF, new Aspect[]{Aspect.VOID, Aspect.ORDER},
                ResourceLocation.fromNamespaceAndPath("myaddon", "textures/aspects/" + tag + ".png"), 1);
        registered.add(aspect);
        return aspect;
    }

    private static <T> JsonElement json(Codec<T> codec, T value) {
        return codec.encodeStart(JsonOps.INSTANCE, value).getOrThrow();
    }

    private static <T> AspectList roundTrip(DynamicOps<T> ops, AspectList list) {
        return AspectList.CODEC.parse(ops, AspectList.CODEC.encodeStart(ops, list).getOrThrow()).getOrThrow();
    }

    private static <T> String decodeError(Codec<T> codec, String json) {
        DataResult<T> result = codec.parse(JsonOps.INSTANCE, JsonParser.parseString(json));
        assertTrue(result.error().isPresent(), "decoding " + json + " must fail, got " + result.result());
        return result.error().get().message();
    }

    // ------------------------------------------------------------------------------------------------ Aspect.CODEC

    @Test void anAspectIsWrittenAsItsTagAndReadBackAsTheRegisteredInstance() {
        assertEquals(new JsonPrimitive("ignis"), json(Aspect.CODEC, Aspect.FIRE));
        assertSame(Aspect.FIRE, Aspect.CODEC.parse(JsonOps.INSTANCE, new JsonPrimitive("ignis")).getOrThrow());
        for (Aspect aspect : Aspect.aspects.values()) {
            assertSame(aspect, Aspect.CODEC.parse(NbtOps.INSTANCE, Aspect.CODEC.encodeStart(NbtOps.INSTANCE, aspect).getOrThrow()).getOrThrow(),
                    "NBT round trip of " + aspect.getTag());
        }
    }

    @Test void anAddonAspectRoundTripsLikeAnOriginalOne() {
        Aspect tempus = addonAspect();
        assertEquals(new JsonPrimitive(tempus.getTag()), json(Aspect.CODEC, tempus));
        assertSame(tempus, Aspect.CODEC.parse(JsonOps.INSTANCE, json(Aspect.CODEC, tempus)).getOrThrow());
        assertSame(tempus, Aspect.CODEC.parse(NbtOps.INSTANCE, Aspect.CODEC.encodeStart(NbtOps.INSTANCE, tempus).getOrThrow()).getOrThrow());
    }

    @Test void anUnknownTagIsADecodeErrorThatNamesTheTag() {
        String message = decodeError(Aspect.CODEC, "\"no_such_aspect\"");
        assertTrue(message.contains("Unknown aspect tag 'no_such_aspect'"), message);
        assertTrue(decodeError(Aspect.CODEC, "\"\"").contains("Unknown aspect tag ''"), "the empty tag is unknown too");
        // The lookup is by the exact tag: the original tags are lower case.
        assertTrue(decodeError(Aspect.CODEC, "\"IGNIS\"").contains("Unknown aspect tag 'IGNIS'"));
        assertFalse(Aspect.CODEC.parse(JsonOps.INSTANCE, new JsonPrimitive(5)).result().isPresent(), "5 is not a registered tag");
    }

    @Test void anAspectThatIsNotRegisteredUnderItsOwnTagCannotBeWritten() {
        Aspect tempus = addonAspect();
        String original = tempus.getTag();
        tempus.setTag("retagged_" + original);
        try {
            DataResult<JsonElement> result = Aspect.CODEC.encodeStart(JsonOps.INSTANCE, tempus);
            assertTrue(result.error().isPresent(), "the written tag could not be read back as this aspect");
            assertTrue(result.error().get().message().contains("retagged_" + original), result.error().get().message());
        } finally {
            tempus.setTag(original);
        }
        assertTrue(Aspect.CODEC.encodeStart(JsonOps.INSTANCE, null).error().isPresent(), "null is an error, not a crash");
    }

    // ------------------------------------------------------------------------------------- Aspect.STREAM_CODEC

    @Test void anAspectCrossesTheNetworkAsItsTagOnAnyByteBuf() {
        Aspect tempus = addonAspect();
        for (Aspect aspect : List.of(Aspect.FIRE, Aspect.TRAP, tempus)) {
            ByteBuf plain = Unpooled.buffer();
            Aspect.STREAM_CODEC.encode(plain, aspect);
            assertSame(aspect, Aspect.STREAM_CODEC.decode(plain));
            assertEquals(0, plain.readableBytes(), "the whole message was read");

            // A data component's networkSynchronized codec reads a RegistryFriendlyByteBuf, which is a ByteBuf.
            FriendlyByteBuf friendly = new FriendlyByteBuf(Unpooled.buffer());
            Aspect.STREAM_CODEC.encode(friendly, aspect);
            assertSame(aspect, Aspect.STREAM_CODEC.decode(friendly));
        }
        ByteBuf expected = Unpooled.buffer();
        ByteBufCodecs.STRING_UTF8.encode(expected, "ignis");
        ByteBuf actual = Unpooled.buffer();
        Aspect.STREAM_CODEC.encode(actual, Aspect.FIRE);
        assertEquals(expected, actual, "the wire form is the UTF-8 tag string");
    }

    @Test void theStreamCodecIsUsableForARegistryFriendlyDataComponentCodec() {
        // Compile-time contract: StreamCodec<ByteBuf, Aspect> widens to StreamCodec<? super RegistryFriendlyByteBuf, Aspect>.
        net.minecraft.network.codec.StreamCodec<? super RegistryFriendlyByteBuf, Aspect> asComponentCodec = Aspect.STREAM_CODEC;
        assertSame(Aspect.STREAM_CODEC, asComponentCodec);
    }

    @Test void anUnknownTagOnTheWireIsADecoderExceptionWithAClearMessage() {
        ByteBuf buffer = Unpooled.buffer();
        ByteBufCodecs.STRING_UTF8.encode(buffer, "no_such_aspect");
        DecoderException error = assertThrows(DecoderException.class, () -> Aspect.STREAM_CODEC.decode(buffer));
        assertTrue(error.getMessage().contains("Unknown aspect tag 'no_such_aspect'"), error.getMessage());
    }

    @Test void writingAnUnregisteredOrNullAspectIsAnEncoderException() {
        Aspect tempus = addonAspect();
        String original = tempus.getTag();
        tempus.setTag("retagged_" + original);
        try {
            assertThrows(EncoderException.class, () -> Aspect.STREAM_CODEC.encode(Unpooled.buffer(), tempus));
        } finally {
            tempus.setTag(original);
        }
        assertThrows(EncoderException.class, () -> Aspect.STREAM_CODEC.encode(Unpooled.buffer(), null));
    }

    // ------------------------------------------------------------------------------------------ AspectList.CODEC

    @Test void aListIsWrittenAsOrderedTagAndAmountEntries() {
        AspectList list = new AspectList().add(Aspect.MAGIC, 2).add(Aspect.FIRE, 4);
        JsonElement written = json(AspectList.CODEC, list);
        assertEquals(JsonParser.parseString("[{\"tag\":\"praecantatio\",\"amount\":2},{\"tag\":\"ignis\",\"amount\":4}]"), written);

        AspectList read = AspectList.CODEC.parse(JsonOps.INSTANCE, written).getOrThrow();
        assertEquals(List.of(Aspect.MAGIC, Aspect.FIRE), List.of(read.getAspects()), "the written order is the insertion order");
        assertEquals(2, read.getAmount(Aspect.MAGIC));
        assertEquals(4, read.getAmount(Aspect.FIRE));
        assertNotSame(list, read, "every decode is a new, mutable list");
        read.add(Aspect.AIR, 1);
        assertEquals(0, list.getAmount(Aspect.AIR));
    }

    @Test void anAddonAspectAndTheEmptyListRoundTrip() {
        Aspect tempus = addonAspect();
        AspectList list = new AspectList().add(tempus, 7).add(Aspect.VOID, 1).add(Aspect.AIR, Integer.MAX_VALUE);
        for (AspectList read : List.of(roundTrip(JsonOps.INSTANCE, list), roundTrip(NbtOps.INSTANCE, list))) {
            assertEquals(List.of(tempus, Aspect.VOID, Aspect.AIR), List.of(read.getAspects()));
            assertEquals(7, read.getAmount(tempus));
            assertEquals(Integer.MAX_VALUE, read.getAmount(Aspect.AIR));
        }
        assertEquals(JsonParser.parseString("[]"), json(AspectList.CODEC, new AspectList()));
        assertEquals(0, AspectList.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("[]")).getOrThrow().size());
    }

    @Test void decodingIsStrictAndNeverTrimsOrSumsTheData() {
        for (String bad : List.of("[{\"tag\":\"ignis\",\"amount\":0}]", "[{\"tag\":\"ignis\",\"amount\":-3}]")) {
            assertTrue(AspectList.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(bad)).error().isPresent(), bad);
        }
        String unknown = decodeError(AspectList.CODEC, "[{\"tag\":\"ignis\",\"amount\":1},{\"tag\":\"no_such_aspect\",\"amount\":1}]");
        assertTrue(unknown.contains("Unknown aspect tag 'no_such_aspect'"), unknown);
        String duplicate = decodeError(AspectList.CODEC, "[{\"tag\":\"ignis\",\"amount\":1},{\"tag\":\"ignis\",\"amount\":2}]");
        assertTrue(duplicate.contains("Duplicate aspect 'ignis'"), duplicate);
        assertTrue(AspectList.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("[{\"tag\":\"ignis\"}]")).error().isPresent(), "amount is required");
        assertTrue(AspectList.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("{\"ignis\":1}")).error().isPresent(), "a map is not the written form");
    }

    @Test void entriesWithoutAnAmountAreNotWritten() {
        // AspectList.reduce keeps a zero key (a documented original quirk); such a list must still save.
        AspectList list = new AspectList().add(Aspect.FIRE, 4).add(Aspect.WATER, 2);
        assertTrue(list.reduce(Aspect.WATER, 2));
        assertEquals(2, list.size(), "the zero key is still in the list");
        AspectList read = AspectList.CODEC.parse(JsonOps.INSTANCE, json(AspectList.CODEC, list)).getOrThrow();
        assertEquals(List.of(Aspect.FIRE), List.of(read.getAspects()));
        assertEquals(0, read.getAmount(Aspect.WATER), "the amount is the same: zero");

        list.aspects.put(Aspect.AIR, -5);
        assertEquals(1, AspectList.CODEC.parse(JsonOps.INSTANCE, json(AspectList.CODEC, list)).getOrThrow().size(), "a negative amount holds no aspect either");
    }

    @Test void aNullListAndAnUnregisteredAspectInAListCannotBeWritten() {
        assertTrue(AspectList.CODEC.encodeStart(JsonOps.INSTANCE, null).error().isPresent());
        Aspect tempus = addonAspect();
        AspectList list = new AspectList().add(tempus, 1);
        String original = tempus.getTag();
        tempus.setTag("retagged_" + original);
        try {
            assertTrue(AspectList.CODEC.encodeStart(JsonOps.INSTANCE, list).error().isPresent());
        } finally {
            tempus.setTag(original);
        }
    }

    // ------------------------------------------------------------------------------------ AspectList.STREAM_CODEC

    @Test void aListCrossesTheNetworkInOrderAndSkipsEntriesWithoutAnAmount() {
        Aspect tempus = addonAspect();
        AspectList list = new AspectList().add(tempus, 300).add(Aspect.FIRE, 1).add(Aspect.WATER, 9);
        list.reduce(Aspect.WATER, 9);
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        AspectList.STREAM_CODEC.encode(buffer, list);
        AspectList read = AspectList.STREAM_CODEC.decode(buffer);
        assertEquals(0, buffer.readableBytes());
        assertEquals(List.of(tempus, Aspect.FIRE), List.of(read.getAspects()));
        assertEquals(300, read.getAmount(tempus));
        assertEquals(1, read.getAmount(Aspect.FIRE));

        ByteBuf empty = Unpooled.buffer();
        AspectList.STREAM_CODEC.encode(empty, new AspectList());
        assertEquals(0, AspectList.STREAM_CODEC.decode(empty).size());
    }

    @Test void theWireFormIsACountThenTagAndAmountPerEntry() {
        ByteBuf expected = Unpooled.buffer();
        ByteBufCodecs.VAR_INT.encode(expected, 2);
        ByteBufCodecs.STRING_UTF8.encode(expected, "ignis");
        ByteBufCodecs.VAR_INT.encode(expected, 4);
        ByteBufCodecs.STRING_UTF8.encode(expected, "aqua");
        ByteBufCodecs.VAR_INT.encode(expected, 1);
        ByteBuf actual = Unpooled.buffer();
        AspectList.STREAM_CODEC.encode(actual, new AspectList().add(Aspect.FIRE, 4).add(Aspect.WATER, 1));
        assertEquals(expected, actual);
    }

    @Test void badNetworkDataIsADecoderExceptionNotAPartialList() {
        ByteBuf oversized = Unpooled.buffer();
        ByteBufCodecs.VAR_INT.encode(oversized, Aspect.aspects.size() + 1);
        assertTrue(assertThrows(DecoderException.class, () -> AspectList.STREAM_CODEC.decode(oversized)).getMessage().contains("Invalid aspect list size"));

        ByteBuf negative = Unpooled.buffer();
        ByteBufCodecs.VAR_INT.encode(negative, -1);
        assertThrows(DecoderException.class, () -> AspectList.STREAM_CODEC.decode(negative));

        ByteBuf unknown = Unpooled.buffer();
        ByteBufCodecs.VAR_INT.encode(unknown, 1);
        ByteBufCodecs.STRING_UTF8.encode(unknown, "no_such_aspect");
        ByteBufCodecs.VAR_INT.encode(unknown, 1);
        assertTrue(assertThrows(DecoderException.class, () -> AspectList.STREAM_CODEC.decode(unknown)).getMessage().contains("Unknown aspect tag 'no_such_aspect'"));

        ByteBuf noAmount = Unpooled.buffer();
        ByteBufCodecs.VAR_INT.encode(noAmount, 1);
        ByteBufCodecs.STRING_UTF8.encode(noAmount, "ignis");
        ByteBufCodecs.VAR_INT.encode(noAmount, 0);
        assertTrue(assertThrows(DecoderException.class, () -> AspectList.STREAM_CODEC.decode(noAmount)).getMessage().contains("Invalid amount 0 of aspect 'ignis'"));

        ByteBuf duplicate = Unpooled.buffer();
        ByteBufCodecs.VAR_INT.encode(duplicate, 2);
        for (int i = 0; i < 2; i++) {
            ByteBufCodecs.STRING_UTF8.encode(duplicate, "ignis");
            ByteBufCodecs.VAR_INT.encode(duplicate, 1);
        }
        assertTrue(assertThrows(DecoderException.class, () -> AspectList.STREAM_CODEC.decode(duplicate)).getMessage().contains("Duplicate aspect 'ignis'"));
    }

    @Test void theListCountBoundFollowsTheLiveRegistry() {
        // An add-on aspect raises the number of distinct aspects a list can hold, so a list of all 49 still decodes.
        Aspect tempus = addonAspect();
        AspectList all = new AspectList();
        for (Aspect aspect : Aspect.aspects.values()) all.add(aspect, 1);
        assertEquals(49, all.size());
        ByteBuf buffer = Unpooled.buffer();
        AspectList.STREAM_CODEC.encode(buffer, all);
        AspectList read = AspectList.STREAM_CODEC.decode(buffer);
        assertEquals(49, read.size());
        assertEquals(1, read.getAmount(tempus));
    }
}
