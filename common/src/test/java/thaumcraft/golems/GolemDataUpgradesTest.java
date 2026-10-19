package thaumcraft.golems;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import java.util.Collections;
import java.util.List;
import net.minecraft.SharedConstants;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumcraft.common.items.golems.GolemData;
import thaumcraft.common.items.golems.GolemUseOptions;

/** Placer-side persistence of the upgrade slots: JSON / NBT / network codec, validation and equality. */
class GolemDataUpgradesTest {
    // Must precede the ItemStack constant below: Gradle runs a previously failed class first, before any other test bootstrapped the registries.
    static {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    private static final List<ItemStack> NO_FILTERS = Collections.nCopies(GolemData.FILTER_SLOTS, ItemStack.EMPTY);
    private static final List<Integer> NO_COUNTS = Collections.nCopies(GolemData.FILTER_SLOTS, 0);

    @BeforeAll static void bootstrapNativeStacks() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    private static GolemData data(int core, List<Integer> upgrades) {
        return new GolemData(core, NO_FILTERS, NO_COUNTS, false, List.of(), GolemUseOptions.DEFAULT, upgrades);
    }

    @Test void existingConstructorsAndTheDefaultComponentHaveNoUpgrades() {
        assertEquals(List.of(), GolemData.EMPTY.upgrades());
        assertEquals(List.of(), new GolemData(GolemData.NO_CORE, NO_FILTERS).upgrades());
        assertEquals(List.of(), new GolemData(GolemData.NO_CORE, NO_FILTERS, NO_COUNTS, false, List.of()).upgrades());
        assertEquals(List.of(), new GolemData(GolemData.NO_CORE, NO_FILTERS, NO_COUNTS, false, List.of(), GolemUseOptions.DEFAULT).upgrades());
    }

    @Test void upgradesRoundTripThroughJsonAndNativeNbtInSlotOrder() {
        for (List<Integer> upgrades : List.of(List.of(1), List.of(1, 5), List.of(-1, 0), List.of(0, 1, 5), List.of(3), List.of(3, 1),
                List.of(2), List.of(4), List.of(2, 4), List.of(-1, 4, 2))) {
            var original = data(GolemData.HARVEST, upgrades);
            var json = GolemData.CODEC.encodeStart(JsonOps.INSTANCE, original).getOrThrow();
            assertEquals(original, GolemData.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow(), upgrades.toString());
            var nbt = GolemData.CODEC.encodeStart(NbtOps.INSTANCE, original).getOrThrow();
            assertEquals(original, GolemData.CODEC.parse(NbtOps.INSTANCE, nbt).getOrThrow(), upgrades.toString());
            assertEquals(upgrades, GolemData.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow().upgrades());
        }
    }

    @Test void anEmptyListIsOmittedSoExistingStacksKeepTheirExactSerializedForm() {
        JsonObject json = GolemData.CODEC.encodeStart(JsonOps.INSTANCE, GolemData.EMPTY).getOrThrow().getAsJsonObject();
        assertFalse(json.has("upgrades"));
        assertEquals(GolemData.EMPTY, GolemData.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow());
        assertEquals(List.of(1), GolemData.CODEC.parse(JsonOps.INSTANCE, withUpgrades(json, "[1]")).getOrThrow().upgrades());
    }

    @Test void unsupportedOrMalformedUpgradeListsNeverBecomeAPlainGolem() {
        var base = GolemData.CODEC.encodeStart(JsonOps.INSTANCE, GolemData.EMPTY).getOrThrow().getAsJsonObject();
        // 2 (Fire) and 4 (Order) are valid since the third group; only ids outside the original 0..5 are refused.
        for (String bad : List.of("[6]", "[7]", "[-2]", "[1,1,1,1]", "[\"earth\"]", "[1.5]", "[null]", "1", "{}",
                "[4294967297]")) {
            var decoded = GolemData.CODEC.parse(JsonOps.INSTANCE, withUpgrades(base, bad));
            assertTrue(decoded.error().isPresent() && decoded.result().isEmpty(), bad);
        }
    }

    @Test void trailingEmptySlotsAreCanonicalButOrderIsKept() {
        assertEquals(data(GolemData.NO_CORE, List.of(1)), data(GolemData.NO_CORE, List.of(1, -1)));
        assertEquals(data(GolemData.NO_CORE, List.of()), data(GolemData.NO_CORE, List.of(-1, -1)));
        assertEquals(List.of(-1, 1), data(GolemData.NO_CORE, List.of(-1, 1, -1)).upgrades());
        assertNotEquals(data(GolemData.NO_CORE, List.of(-1, 1)), data(GolemData.NO_CORE, List.of(1)));
    }

    @Test void upgradesTakePartInEqualityAndHashing() {
        var plain = data(GolemData.LUMBER, List.of());
        var earth = data(GolemData.LUMBER, List.of(1));
        assertNotEquals(plain, earth);
        assertNotEquals(earth, data(GolemData.LUMBER, List.of(5)));
        assertEquals(earth, data(GolemData.LUMBER, List.of(1)));
        assertEquals(earth.hashCode(), data(GolemData.LUMBER, List.of(1)).hashCode());
    }

    @Test void theConstructorRefusesWhatThePortCannotHonour() {
        for (List<Integer> bad : List.of(List.of(6), List.of(7), List.of(-2), List.of(1, 1, 1, 1))) {
            assertThrows(IllegalArgumentException.class, () -> data(GolemData.NO_CORE, bad), bad.toString());
        }
        assertThrows(IllegalArgumentException.class, () -> data(GolemData.NO_CORE, null));
    }

    @Test void networkCodecRoundTripsAndRefusesCorruptCounts() {
        for (List<Integer> upgrades : List.of(List.<Integer>of(), List.of(1), List.of(-1, 5), List.of(0, 1, 5), List.of(3), List.of(2, 4))) {
            var original = data(GolemData.GUARD, upgrades);
            var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
            GolemData.STREAM_CODEC.encode(buffer, original);
            assertEquals(original, GolemData.STREAM_CODEC.decode(buffer), upgrades.toString());
            assertEquals(0, buffer.readableBytes());
        }
        // Valid prefix, then a fourth slot / a byte outside the original 0..5 types.
        var encoded = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        GolemData.STREAM_CODEC.encode(encoded, data(GolemData.NO_CORE, List.of()));
        // Layout of the end: [upgrade count 0][colour count 6][six colour bytes][Guard mask]. The sections after the upgrades stay
        // intact in every corrupt stream, so only the upgrade section can be what the decoder refuses.
        int rest = 1 + 6 + 1; // colour count, six colours, mask
        int tail = encoded.writerIndex() - rest - 1; // start of the zero upgrade count
        for (int count : new int[]{4, 5}) {
            var corrupt = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
            corrupt.writeBytes(encoded, 0, tail);
            corrupt.writeVarInt(count);
            for (int slot = 0; slot < count; slot++) corrupt.writeByte(1);
            corrupt.writeBytes(encoded, encoded.writerIndex() - rest, rest);
            assertThrows(DecoderException.class, () -> GolemData.STREAM_CODEC.decode(corrupt), "count " + count);
        }
        for (int outside : new int[]{6, -2}) {
            var unknown = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
            unknown.writeBytes(encoded, 0, tail);
            unknown.writeVarInt(1);
            unknown.writeByte(outside);
            unknown.writeBytes(encoded, encoded.writerIndex() - rest, rest);
            assertThrows(DecoderException.class, () -> GolemData.STREAM_CODEC.decode(unknown), "type " + outside);
        }
        // Control: the same splice with a valid type decodes, so the two refusals above come from the type and the count alone.
        var valid = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        valid.writeBytes(encoded, 0, tail);
        valid.writeVarInt(1);
        valid.writeByte(1);
        valid.writeBytes(encoded, encoded.writerIndex() - rest, rest);
        assertEquals(List.of(1), GolemData.STREAM_CODEC.decode(valid).upgrades());
    }

    private static com.google.gson.JsonElement withUpgrades(JsonObject base, String upgradesJson) {
        JsonObject copy = base.deepCopy();
        copy.add("upgrades", JsonParser.parseString(upgradesJson));
        return copy;
    }
}
