package thaumcraft.golems;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumcraft.common.items.golems.GolemData;
import thaumcraft.common.items.golems.GolemMarker;
import thaumcraft.common.items.golems.GolemUseOptions;

/** Order: marker colours, slot colours and the bell's colour cycle (original Marker, ItemGolemBell.changeMarkers). */
class GolemColorsTest {
    private static final ResourceLocation OVERWORLD = ResourceLocation.withDefaultNamespace("overworld");
    private static final int FIRE = 2, ORDER = 4;

    @BeforeAll static void bootstrapNativeStacks() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    private static GolemMarker marker(int x, int color) { return new GolemMarker(OVERWORLD, new BlockPos(x, 64, 0), Direction.UP, color); }
    private static List<ItemStack> empties(int size) { return Collections.nCopies(size, ItemStack.EMPTY); }
    private static List<Integer> zeros(int size) { return Collections.nCopies(size, 0); }
    private static List<Integer> colors(int size, int fill) { return new ArrayList<>(Collections.nCopies(size, fill)); }

    private static GolemData data(int core, List<Integer> upgrades, List<Integer> colors, List<GolemMarker> markers) {
        int slots = GolemData.slotCount(core, GolemData.fireCount(upgrades));
        return new GolemData(core, empties(slots), zeros(slots), false, markers, GolemUseOptions.DEFAULT, upgrades, colors);
    }

    // ---- markers -------------------------------------------------------------------------------------------------

    @Test void anOrdinaryMarkerHasNoColourAndStaysThreeArgumentCompatible() {
        var plain = new GolemMarker(OVERWORLD, new BlockPos(1, 2, 3), Direction.NORTH);
        assertEquals(GolemMarker.ANY, plain.color());
        assertEquals(plain, plain.withColor(GolemMarker.ANY));
        assertNotEquals(plain.withColor(3), plain, "the colour takes part in equality, like the original Marker.equals");
        assertTrue(plain.sameSpot(plain.withColor(7)));
        assertFalse(plain.sameSpot(new GolemMarker(OVERWORLD, new BlockPos(1, 2, 3), Direction.SOUTH)));
        assertThrows(IllegalArgumentException.class, () -> plain.withColor(16));
        assertThrows(IllegalArgumentException.class, () -> plain.withColor(-2));
    }

    @Test void aSlotColourAcceptsAnyMarkerWhenAnyAndOtherwiseOnlyItsOwnColour() {
        for (int marker = -1; marker <= 15; marker++) {
            assertTrue(GolemMarker.accepts(-1, marker), "any accepts " + marker);
            for (int slot = 0; slot <= 15; slot++) assertEquals(slot == marker, GolemMarker.accepts(slot, marker), slot + " vs " + marker);
        }
        assertTrue(marker(0, 9).acceptedBy(-1) && marker(0, 9).acceptedBy(9) && !marker(0, 9).acceptedBy(8) && !marker(0, -1).acceptedBy(0));
    }

    @Test void markerColoursRoundTripJsonAndNbtAndTheDefaultKeepsItsOldForm() {
        for (int color = -1; color <= 15; color++) {
            var original = marker(5, color);
            var json = GolemMarker.CODEC.encodeStart(JsonOps.INSTANCE, original).getOrThrow();
            assertEquals(original, GolemMarker.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow(), "json " + color);
            assertEquals(color != -1, json.getAsJsonObject().has("color"), "only a colour other than any is written: " + color);
            var nbt = GolemMarker.CODEC.encodeStart(NbtOps.INSTANCE, original).getOrThrow();
            assertEquals(original, GolemMarker.CODEC.parse(NbtOps.INSTANCE, nbt).getOrThrow(), "nbt " + color);
        }
    }

    @Test void aMalformedColourOrFutureFieldIsADecodeErrorNeverAnOrdinaryMarker() {
        String base = "{\"dimension\":\"minecraft:overworld\",\"pos\":[1,2,3],\"face\":\"up\"";
        assertTrue(GolemMarker.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(base + "}")).result().isPresent());
        for (String color : List.of("16", "-2", "3.7", "4294967297", "\"red\"", "null")) {
            assertTrue(GolemMarker.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(base + ",\"color\":" + color + "}")).error().isPresent(), color);
        }
        assertTrue(GolemMarker.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(base + ",\"upgrade\":1}")).error().isPresent());
        assertTrue(GolemMarker.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("{\"dimension\":\"minecraft:overworld\",\"pos\":[1,2,3]}")).error().isPresent());
    }

    // ---- the bell -----------------------------------------------------------------------------------------------

    @Test void withoutOrderTheBellTogglesAnUncolouredMarker() {
        var spot = marker(1, -1);
        var added = GolemMarker.click(List.of(), spot, false, false, 64);
        assertEquals(List.of(spot), added);
        assertEquals(List.of(), GolemMarker.click(added, spot, false, false, 64));
        // Original ItemGolemBell.changeMarkers: without Order only the UNCOLOURED marker of that spot is searched
        // (markers.indexOf(new Marker(x, y, z, dim, side, -1))), so a leftover colour of a former Order golem
        // survives the click and an uncoloured marker is added next to it.
        assertEquals(List.of(marker(1, 7), spot), GolemMarker.click(List.of(marker(1, 7)), spot, false, false, 64));
        assertEquals(List.of(marker(1, 7)), GolemMarker.click(List.of(marker(1, 7), spot), spot, false, false, 64),
                "the uncoloured marker of that spot is the one the second click removes");
        // Other markers are untouched and the limit is honoured.
        assertEquals(List.of(marker(2, -1), spot), GolemMarker.click(List.of(marker(2, -1)), spot, false, false, 64));
        assertNull(GolemMarker.click(List.of(marker(2, -1)), spot, false, false, 1));
    }

    @Test void withOrderAClickCyclesAnyThroughSixteenColoursThenRemovesTheMarker() {
        var spot = marker(1, -1);
        List<GolemMarker> markers = GolemMarker.click(List.of(), spot, true, false, 64);
        assertEquals(List.of(marker(1, -1)), markers, "the first click adds an uncoloured marker");
        for (int color = 0; color <= 15; color++) {
            markers = GolemMarker.click(markers, spot, true, false, 64);
            assertEquals(List.of(marker(1, color)), markers, "click " + (color + 2));
        }
        assertEquals(List.of(), GolemMarker.click(markers, spot, true, false, 64), "after colour 15 the marker is removed");
    }

    @Test void aSneakingClickRemovesAColouredMarkerAtOnceAndARecolouredMarkerMovesToTheEnd() {
        var first = marker(1, 4);
        var other = marker(2, 6);
        assertEquals(List.of(other), GolemMarker.click(List.of(first, other), marker(1, -1), true, true, 64));
        // The original removes then re-adds, so the recoloured marker becomes the last in routing order.
        assertEquals(List.of(other, marker(1, 5)), GolemMarker.click(List.of(first, other), marker(1, -1), true, false, 64));
        // A new spot is always added uncoloured, with or without Order.
        assertEquals(List.of(first, marker(9, -1)), GolemMarker.click(List.of(first), marker(9, -1), true, false, 64));
    }

    // ---- placer data ---------------------------------------------------------------------------------------------

    @Test void slotColoursDefaultToAnyAndAreOmittedFromJsonSoOldStacksKeepTheirForm() {
        var fill = data(GolemData.FILL, List.of(), List.of(), List.of());
        assertEquals(colors(6, -1), fill.colors());
        assertTrue(fill.allColorsDefault());
        JsonObject json = GolemData.CODEC.encodeStart(JsonOps.INSTANCE, fill).getOrThrow().getAsJsonObject();
        assertFalse(json.has("colors"));
        assertEquals(fill, GolemData.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow());
        assertEquals(colors(6, -1), GolemData.EMPTY.colors());
    }

    @Test void colouredSlotsNeedAnOrderUpgradeAndAFillEmptyOrUseCore() {
        var coloured = colors(6, -1);
        coloured.set(2, 7);
        assertThrows(IllegalArgumentException.class, () -> data(GolemData.FILL, List.of(), coloured, List.of()), "no Order");
        assertThrows(IllegalArgumentException.class, () -> data(GolemData.FILL, List.of(FIRE), coloured, List.of()), "Fire is not Order");
        assertDoesNotThrow(() -> data(GolemData.FILL, List.of(ORDER), coloured, List.of()));
        assertDoesNotThrow(() -> data(GolemData.EMPTY_CORE, List.of(ORDER), coloured, List.of()));
        assertDoesNotThrow(() -> data(GolemData.USE, List.of(ORDER), coloured, List.of()));
        for (int core : new int[] {GolemData.GATHER, GolemData.HARVEST, GolemData.LIQUID, GolemData.GUARD, GolemData.SORTING, GolemData.FISHING}) {
            assertThrows(IllegalArgumentException.class, () -> data(core, List.of(ORDER), coloured, List.of()), "core " + core + " has no routed colours");
        }
        var invalid = colors(6, -1);
        invalid.set(0, 16);
        assertThrows(IllegalArgumentException.class, () -> data(GolemData.FILL, List.of(ORDER), invalid, List.of()));
        invalid.set(0, -2);
        assertThrows(IllegalArgumentException.class, () -> data(GolemData.FILL, List.of(ORDER), invalid, List.of()));
    }

    @Test void colouredMarkersNeedOrderToo() {
        assertThrows(IllegalArgumentException.class, () -> data(GolemData.EMPTY_CORE, List.of(), List.of(), List.of(marker(1, 3))));
        assertDoesNotThrow(() -> data(GolemData.EMPTY_CORE, List.of(ORDER), List.of(), List.of(marker(1, 3), marker(2, -1))));
        // An uncoloured marker never needs the upgrade.
        assertDoesNotThrow(() -> data(GolemData.EMPTY_CORE, List.of(), List.of(), List.of(marker(1, -1))));
    }

    @Test void slotColoursFollowTheFirePagesAndAreNeverTruncated() {
        var six = colors(6, -1);
        six.set(5, 15);
        var padded = data(GolemData.FILL, List.of(ORDER, FIRE), six, List.of());
        assertEquals(12, padded.colors().size());
        assertEquals(15, padded.colors().get(5));
        assertEquals(-1, padded.colors().get(11));
        assertThrows(IllegalArgumentException.class, () -> data(GolemData.FILL, List.of(ORDER, FIRE), colors(18, -1), List.of()));
        assertThrows(IllegalArgumentException.class, () -> data(GolemData.FILL, List.of(ORDER), colors(12, -1), List.of()));
        var twelve = colors(12, -1);
        twelve.set(11, 3);
        assertEquals(3, data(GolemData.FILL, List.of(ORDER, FIRE), twelve, List.of()).colors().get(11));
    }

    @Test void coloursAndMarkersRoundTripJsonNbtAndTheNetwork() {
        var slotColors = colors(12, -1);
        slotColors.set(0, 0);
        slotColors.set(7, 12);
        var original = data(GolemData.EMPTY_CORE, List.of(ORDER, FIRE), slotColors, List.of(marker(1, 3), marker(2, -1), marker(3, 15)));
        var json = GolemData.CODEC.encodeStart(JsonOps.INSTANCE, original).getOrThrow();
        var fromJson = GolemData.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
        assertEquals(original, fromJson);
        assertEquals(slotColors, fromJson.colors());
        assertEquals(List.of(3, -1, 15), fromJson.markers().stream().map(GolemMarker::color).toList());
        assertTrue(json.getAsJsonObject().has("colors"));
        var nbt = GolemData.CODEC.encodeStart(NbtOps.INSTANCE, original).getOrThrow();
        var fromNbt = GolemData.CODEC.parse(NbtOps.INSTANCE, nbt).getOrThrow();
        assertEquals(original, fromNbt);
        assertEquals(slotColors, fromNbt.colors());
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY));
        GolemData.STREAM_CODEC.encode(buffer, original);
        var decoded = GolemData.STREAM_CODEC.decode(buffer);
        assertEquals(original, decoded);
        assertEquals(slotColors, decoded.colors());
        assertEquals(List.of(3, -1, 15), decoded.markers().stream().map(GolemMarker::color).toList());
        assertEquals(0, buffer.readableBytes());
    }

    /** A record equals that skipped a field would make every round-trip assertion above vacuous for that field. */
    @Test void equalityCoversSlotColoursMarkerColoursAndEveryGhostPage() {
        var slotColors = colors(12, -1);
        slotColors.set(1, 4);
        var markers = List.of(marker(1, 3), marker(2, -1));
        var base = data(GolemData.EMPTY_CORE, List.of(ORDER, FIRE), slotColors, markers);
        assertEquals(base, data(GolemData.EMPTY_CORE, List.of(ORDER, FIRE), new ArrayList<>(slotColors), List.of(marker(1, 3), marker(2, -1))));
        assertEquals(base.hashCode(), data(GolemData.EMPTY_CORE, List.of(ORDER, FIRE), new ArrayList<>(slotColors), markers).hashCode());
        var recoloured = new ArrayList<>(slotColors);
        recoloured.set(7, 11);
        assertNotEquals(base, data(GolemData.EMPTY_CORE, List.of(ORDER, FIRE), recoloured, markers), "a second-page slot colour");
        assertNotEquals(base.hashCode(), data(GolemData.EMPTY_CORE, List.of(ORDER, FIRE), recoloured, markers).hashCode());
        assertNotEquals(base, data(GolemData.EMPTY_CORE, List.of(ORDER, FIRE), slotColors, List.of(marker(1, 3), marker(2, 14))), "a marker colour");

        // Only the item differs, on the third page, with the same wanted count: the first page alone cannot tell.
        var filters = new ArrayList<>(empties(18));
        var counts = new ArrayList<>(zeros(18));
        filters.set(17, new ItemStack(Items.STONE));
        counts.set(17, 1);
        var stone = new GolemData(GolemData.FILL, filters, counts, false, List.of(), GolemUseOptions.DEFAULT, List.of(FIRE, FIRE));
        filters.set(17, new ItemStack(Items.DIRT));
        var dirt = new GolemData(GolemData.FILL, filters, counts, false, List.of(), GolemUseOptions.DEFAULT, List.of(FIRE, FIRE));
        assertNotEquals(stone, dirt, "a third-page exemplar");
        filters.set(17, new ItemStack(Items.STONE));
        assertEquals(stone, new GolemData(GolemData.FILL, filters, counts, false, List.of(), GolemUseOptions.DEFAULT, List.of(FIRE, FIRE)));
    }

    @Test void aBadColourListInJsonOrTheNetworkIsRefused() {
        JsonObject plain = GolemData.CODEC.encodeStart(JsonOps.INSTANCE, data(GolemData.FILL, List.of(ORDER), colors(6, -1), List.of())).getOrThrow().getAsJsonObject();
        for (String bad : List.of("[1,2,3]", "[0,0,0,0,0,16]", "[0,0,0,0,0,3.5]", "[0,0,0,0,0,-2]", "\"red\"")) {
            JsonObject copy = plain.deepCopy();
            copy.add("colors", JsonParser.parseString(bad));
            assertTrue(GolemData.CODEC.parse(JsonOps.INSTANCE, copy).error().isPresent(), bad);
        }
        // The list may be omitted or present: a plain Fill golem with a (non-default) colour but no Order is refused.
        JsonObject noOrder = GolemData.CODEC.encodeStart(JsonOps.INSTANCE, data(GolemData.FILL, List.of(), colors(6, -1), List.of())).getOrThrow().getAsJsonObject();
        noOrder.add("colors", JsonParser.parseString("[0,-1,-1,-1,-1,-1]"));
        assertTrue(GolemData.CODEC.parse(JsonOps.INSTANCE, noOrder).error().isPresent());
        var valid = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY));
        GolemData.STREAM_CODEC.encode(valid, data(GolemData.FILL, List.of(), List.of(), List.of()));
        assertTrue(valid.readableBytes() > 0);
        for (int count : new int[] {0, 5, 7, 19}) {
            var corrupt = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY));
            corrupt.writeBytes(valid, 0, valid.writerIndex() - 8); // everything before the colour count (varint + six bytes + the Guard mask byte)
            corrupt.writeVarInt(count);
            for (int slot = 0; slot < Math.min(count, 24); slot++) corrupt.writeByte(-1);
            assertThrows(DecoderException.class, () -> GolemData.STREAM_CODEC.decode(corrupt), "colour count " + count);
        }
        assertEquals(Items.AIR, ItemStack.EMPTY.getItem());
    }
}
