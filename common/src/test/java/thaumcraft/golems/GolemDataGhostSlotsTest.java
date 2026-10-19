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
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumcraft.common.items.golems.GolemData;
import thaumcraft.common.items.golems.GolemUseOptions;

/** Fire upgrades enlarge the ghost-slot list of item-filter cores by whole pages of six (original EntityGolemBase:198-224). */
class GolemDataGhostSlotsTest {
    private static final int FIRE = 2, EARTH = 1;

    @BeforeAll static void bootstrapNativeStacks() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    private static List<ItemStack> empties(int size) { return Collections.nCopies(size, ItemStack.EMPTY); }
    private static List<Integer> zeros(int size) { return Collections.nCopies(size, 0); }

    private static GolemData data(int core, int slots, List<Integer> upgrades) {
        return new GolemData(core, empties(slots), zeros(slots), false, List.of(), GolemUseOptions.DEFAULT, upgrades);
    }

    @Test void slotCountIsOnePagePlusOnePerFireOnlyForItemFilterCores() {
        for (int core : new int[] {GolemData.FILL, GolemData.EMPTY_CORE, GolemData.GATHER, GolemData.USE}) {
            assertTrue(GolemData.hasItemGhosts(core), "core " + core);
            assertEquals(6, GolemData.slotCount(core, 0));
            assertEquals(12, GolemData.slotCount(core, 1));
            assertEquals(18, GolemData.slotCount(core, 2));
            assertEquals(18, GolemData.slotCount(core, 9), "no more than two Fire upgrades can exist");
            assertEquals(6, GolemData.slotCount(core, -3));
        }
        for (int core : new int[] {GolemData.NO_CORE, GolemData.HARVEST, GolemData.LUMBER, GolemData.GUARD, GolemData.LIQUID, GolemData.ALCHEMY,
                GolemData.BUTCHER, GolemData.SORTING, GolemData.FISHING}) {
            assertFalse(GolemData.hasItemGhosts(core), "core " + core);
            assertEquals(6, GolemData.slotCount(core, 2), "Fire changes nothing for core " + core);
        }
        assertEquals(GolemData.FILTER_SLOTS * 3, GolemData.MAX_FILTER_SLOTS);
    }

    @Test void fireIsCountedAmongTheUpgradeSlots() {
        assertEquals(0, GolemData.fireCount(List.of()));
        assertEquals(1, GolemData.fireCount(List.of(FIRE)));
        assertEquals(2, GolemData.fireCount(List.of(FIRE, EARTH, FIRE)));
        assertEquals(0, GolemData.fireCount(List.of(EARTH, -1)));
        assertEquals(0, GolemData.fireCount(null));
    }

    @Test void aSixSlotListIsPaddedToTheFirePagesAndKeepsItsContents() {
        var filters = new ArrayList<>(empties(6));
        var counts = new ArrayList<>(zeros(6));
        filters.set(2, new ItemStack(Items.COBBLESTONE));
        counts.set(2, 64);
        var padded = new GolemData(GolemData.FILL, filters, counts, false, List.of(), GolemUseOptions.DEFAULT, List.of(FIRE, FIRE));
        assertEquals(18, padded.filters().size());
        assertEquals(18, padded.wantedCounts().size());
        assertTrue(ItemStack.matches(new ItemStack(Items.COBBLESTONE), padded.filters().get(2)));
        assertEquals(64, padded.wantedCounts().get(2));
        for (int slot = 6; slot < 18; slot++) {
            assertTrue(padded.filters().get(slot).isEmpty(), "slot " + slot);
            assertEquals(0, padded.wantedCounts().get(slot), "slot " + slot);
        }
    }

    @Test void anExactListIsKeptAndAnyOtherSizeIsRefusedNotTruncated() {
        assertEquals(12, data(GolemData.FILL, 12, List.of(FIRE)).filters().size());
        assertEquals(12, data(GolemData.GATHER, 12, List.of(EARTH, FIRE)).filters().size());
        assertEquals(6, data(GolemData.FILL, 6, List.of()).filters().size());
        // Twelve slots without Fire, or eighteen with a single Fire, would hide data the golem cannot show.
        assertThrows(IllegalArgumentException.class, () -> data(GolemData.FILL, 12, List.of()));
        assertThrows(IllegalArgumentException.class, () -> data(GolemData.FILL, 18, List.of(FIRE)));
        assertThrows(IllegalArgumentException.class, () -> data(GolemData.FILL, 12, List.of(FIRE, FIRE)));
        // Cores that Fire does not enlarge keep exactly six.
        assertThrows(IllegalArgumentException.class, () -> data(GolemData.HARVEST, 12, List.of(FIRE)));
        assertThrows(IllegalArgumentException.class, () -> data(GolemData.LIQUID, 12, List.of(FIRE)));
        assertEquals(6, data(GolemData.HARVEST, 6, List.of(FIRE, FIRE)).filters().size());
        assertEquals(6, data(GolemData.LIQUID, 6, List.of(FIRE)).filters().size());
    }

    @Test void theValidationAppliesToEveryPageNotJustTheFirstSix() {
        var filters = new ArrayList<>(empties(12));
        var counts = new ArrayList<>(zeros(12));
        filters.set(9, new ItemStack(Items.IRON_INGOT));
        counts.set(9, 0); // A populated exemplar needs a positive wanted count.
        assertThrows(IllegalArgumentException.class,
                () -> new GolemData(GolemData.FILL, filters, counts, false, List.of(), GolemUseOptions.DEFAULT, List.of(FIRE)));
        counts.set(9, 257);
        assertThrows(IllegalArgumentException.class,
                () -> new GolemData(GolemData.FILL, filters, counts, false, List.of(), GolemUseOptions.DEFAULT, List.of(FIRE)));
        counts.set(9, 256);
        assertDoesNotThrow(() -> new GolemData(GolemData.FILL, filters, counts, false, List.of(), GolemUseOptions.DEFAULT, List.of(FIRE)));
        // Use is a whitelist: every page holds one-count exemplars only.
        counts.set(9, 5);
        assertThrows(IllegalArgumentException.class,
                () -> new GolemData(GolemData.USE, filters, counts, false, List.of(), GolemUseOptions.DEFAULT, List.of(FIRE)));
        counts.set(9, 1);
        assertDoesNotThrow(() -> new GolemData(GolemData.USE, filters, counts, false, List.of(), GolemUseOptions.DEFAULT, List.of(FIRE)));
    }

    @Test void jsonAndNativeNbtRoundTripAllThreePageSizes() {
        for (int fire = 0; fire <= 2; fire++) {
            int slots = 6 + 6 * fire;
            var filters = new ArrayList<>(empties(slots));
            var counts = new ArrayList<>(zeros(slots));
            filters.set(slots - 1, new ItemStack(Items.GOLD_INGOT));
            counts.set(slots - 1, 32);
            var upgrades = new ArrayList<Integer>();
            for (int index = 0; index < fire; index++) upgrades.add(FIRE);
            var original = new GolemData(GolemData.EMPTY_CORE, filters, counts, false, List.of(), GolemUseOptions.DEFAULT, upgrades);
            var json = GolemData.CODEC.encodeStart(JsonOps.INSTANCE, original).getOrThrow();
            assertEquals(original, GolemData.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow(), "json " + slots);
            var nbt = GolemData.CODEC.encodeStart(NbtOps.INSTANCE, original).getOrThrow();
            assertEquals(original, GolemData.CODEC.parse(NbtOps.INSTANCE, nbt).getOrThrow(), "nbt " + slots);
            // A real exemplar needs the item registry; the built-in registries stand in for a login's registry access.
            var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY));
            GolemData.STREAM_CODEC.encode(buffer, original);
            assertEquals(original, GolemData.STREAM_CODEC.decode(buffer), "network " + slots);
            assertEquals(0, buffer.readableBytes());
        }
    }

    @Test void aJsonListWithoutTheMatchingFireIsADecodeErrorNotARepair() {
        JsonObject plain = GolemData.CODEC.encodeStart(JsonOps.INSTANCE, data(GolemData.FILL, 6, List.of())).getOrThrow().getAsJsonObject();
        // Old-format data (six slots, no Fire) is untouched.
        assertEquals(6, GolemData.CODEC.parse(JsonOps.INSTANCE, plain).getOrThrow().filters().size());
        // An old six-slot list that names a Fire upgrade gains its pages.
        JsonObject withFire = plain.deepCopy();
        withFire.add("upgrades", JsonParser.parseString("[2]"));
        assertEquals(12, GolemData.CODEC.parse(JsonOps.INSTANCE, withFire).getOrThrow().filters().size());
        // Nineteen entries and a count that is not a whole number of pages are refused.
        JsonObject tooMany = plain.deepCopy();
        var list = new com.google.gson.JsonArray();
        for (int index = 0; index < 19; index++) list.add(0);
        tooMany.add("wanted_counts", list);
        assertTrue(GolemData.CODEC.parse(JsonOps.INSTANCE, tooMany).error().isPresent());
        JsonObject odd = plain.deepCopy();
        var seven = new com.google.gson.JsonArray();
        for (int index = 0; index < 7; index++) seven.add(0);
        odd.add("wanted_counts", seven);
        assertTrue(GolemData.CODEC.parse(JsonOps.INSTANCE, odd).error().isPresent());
    }

    @Test void theNetworkCodecRefusesAnInvalidSlotCount() {
        var valid = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        GolemData.STREAM_CODEC.encode(valid, data(GolemData.FILL, 6, List.of()));
        // [core varint][slot count varint]...: patch the count (second byte) to impossible values.
        for (int count : new int[] {0, 5, 7, 19, 24, 255}) {
            var bad = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
            bad.writeVarInt(GolemData.FILL);
            bad.writeVarInt(count);
            assertThrows(DecoderException.class, () -> GolemData.STREAM_CODEC.decode(bad), "slots " + count);
        }
    }
}
