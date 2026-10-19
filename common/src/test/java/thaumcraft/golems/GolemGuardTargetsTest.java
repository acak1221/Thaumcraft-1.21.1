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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumcraft.common.items.golems.GolemData;
import thaumcraft.common.items.golems.GolemGuardTargets;
import thaumcraft.common.items.golems.GolemUseOptions;

/** Order Guard target switches (original EntityGolemBase:800-831, 1150-1180, ContainerGolem:92) and their placer data. */
class GolemGuardTargetsTest {
    private static final int FIRE = 2, ORDER = 4;
    private static final int M = GolemGuardTargets.MONSTERS, A = GolemGuardTargets.ANIMALS, P = GolemGuardTargets.PLAYERS, C = GolemGuardTargets.CREEPERS;

    @BeforeAll static void bootstrapNativeStacks() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    private static GolemData guard(List<Integer> upgrades, int mask) { return data(GolemData.GUARD, upgrades, mask); }

    private static GolemData data(int core, List<Integer> upgrades, int mask) {
        return new GolemData(core, Collections.nCopies(6, ItemStack.EMPTY), Collections.nCopies(6, 0), false, List.of(),
                GolemUseOptions.DEFAULT, upgrades, List.of(), mask);
    }

    private static RegistryFriendlyByteBuf buffer() {
        return new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY));
    }

    // ---- the pure rules ---------------------------------------------------------------------------------------------

    @Test void theDefaultAllowsHostileMonstersOnlyWhileTheOriginalDefaultAllowedEverything() {
        assertEquals(M, GolemGuardTargets.DEFAULT);
        assertTrue(GolemGuardTargets.isDefault(M) && !GolemGuardTargets.isDefault(GolemGuardTargets.ALL) && !GolemGuardTargets.isDefault(0));
        // A fresh original Guard has an all-clear toggles byte, i.e. every category allowed: the unsafe default this port does not copy.
        assertEquals(GolemGuardTargets.ALL, GolemGuardTargets.fromOriginalToggles((byte) 0));
        // The port's default, written in the original's inverted bits, forbids animals (2), players (3) and creepers (4).
        assertEquals((byte) 0b11100, GolemGuardTargets.toOriginalToggles(GolemGuardTargets.DEFAULT));
        assertEquals((byte) 0, GolemGuardTargets.toOriginalToggles(GolemGuardTargets.ALL));
        assertEquals((byte) 0b11110, GolemGuardTargets.toOriginalToggles(0));
    }

    @Test void theOriginalTogglesByteMapsBothWaysAndIgnoresTheBitsOfOtherCores() {
        for (int mask = 0; mask <= GolemGuardTargets.ALL; mask++) {
            assertEquals(mask, GolemGuardTargets.fromOriginalToggles(GolemGuardTargets.toOriginalToggles(mask)), "mask " + mask);
        }
        // Bit 0 (Fill precise amount) and bits 5..7 (Entropy fuzzy matching, Use modes) are not Guard state.
        assertEquals(GolemGuardTargets.ALL, GolemGuardTargets.fromOriginalToggles((byte) 0b11100001));
        assertEquals(0, GolemGuardTargets.fromOriginalToggles((byte) 0b00011110));
    }

    @Test void validMasksAreTheFourLowBitsAndToggleFlipsExactlyOneCategory() {
        assertTrue(GolemGuardTargets.valid(0) && GolemGuardTargets.valid(15));
        for (int bad : new int[] {16, 17, 32, 255, -1, Integer.MIN_VALUE}) assertFalse(GolemGuardTargets.valid(bad), "mask " + bad);
        for (int mask = 0; mask <= GolemGuardTargets.ALL; mask++) {
            for (int category : new int[] {M, A, P, C}) {
                int flipped = GolemGuardTargets.toggle(mask, category);
                assertEquals(mask ^ category, flipped);
                assertEquals(mask, GolemGuardTargets.toggle(flipped, category));
                assertEquals((mask & category) != 0, GolemGuardTargets.allows(mask, category));
            }
            for (int notACategory : new int[] {0, 3, 5, 6, 7, 15, 16, 32, -1}) {
                assertEquals(mask, GolemGuardTargets.toggle(mask, notACategory), "toggle(" + mask + ", " + notACategory + ")");
                assertFalse(GolemGuardTargets.allows(mask, notACategory), "allows(" + mask + ", " + notACategory + ")");
            }
        }
    }

    @Test void menuButtonsKeepTheOriginalIds51To54() {
        assertEquals(51, GolemGuardTargets.FIRST_BUTTON);
        assertEquals(M, GolemGuardTargets.categoryOfButton(51));
        assertEquals(A, GolemGuardTargets.categoryOfButton(52));
        assertEquals(P, GolemGuardTargets.categoryOfButton(53));
        assertEquals(C, GolemGuardTargets.categoryOfButton(54));
        for (int other : new int[] {0, 49, 50, 55, 57, 60, 61, 100, -1}) assertEquals(0, GolemGuardTargets.categoryOfButton(other), "button " + other);
        for (int category : new int[] {M, A, P, C}) assertEquals(category, GolemGuardTargets.categoryOfButton(GolemGuardTargets.buttonOf(category)));
        assertEquals(-1, GolemGuardTargets.buttonOf(3));
        assertEquals(-1, GolemGuardTargets.buttonOf(0));
        assertEquals(-1, GolemGuardTargets.buttonOf(16));
    }

    @Test void switchesExistOnlyOnAGuardThatCarriesOrder() {
        assertTrue(GolemGuardTargets.configurable(GolemData.GUARD, 1) && GolemGuardTargets.configurable(GolemData.GUARD, 2));
        assertFalse(GolemGuardTargets.configurable(GolemData.GUARD, 0));
        for (int core : new int[] {GolemData.NO_CORE, GolemData.FILL, GolemData.EMPTY_CORE, GolemData.GATHER, GolemData.HARVEST, GolemData.LIQUID,
                GolemData.ALCHEMY, GolemData.LUMBER, GolemData.USE, GolemData.BUTCHER, GolemData.SORTING, GolemData.FISHING}) {
            assertFalse(GolemGuardTargets.configurable(core, 2), "core " + core);
            assertTrue(GolemGuardTargets.acceptable(GolemGuardTargets.DEFAULT, core, 0), "the default is acceptable anywhere: " + core);
            assertFalse(GolemGuardTargets.acceptable(GolemGuardTargets.ALL, core, 2), "a non-default mask is not: " + core);
        }
        assertTrue(GolemGuardTargets.acceptable(0, GolemData.GUARD, 1));
        assertTrue(GolemGuardTargets.acceptable(GolemGuardTargets.ALL, GolemData.GUARD, 1));
        assertFalse(GolemGuardTargets.acceptable(GolemGuardTargets.ALL, GolemData.GUARD, 0));
        assertFalse(GolemGuardTargets.acceptable(0, GolemData.GUARD, 0), "even switching Monsters off needs Order");
        assertFalse(GolemGuardTargets.acceptable(16, GolemData.GUARD, 1));
        assertFalse(GolemGuardTargets.acceptable(-1, GolemData.GUARD, 1));
    }

    // ---- placer data ------------------------------------------------------------------------------------------------

    @Test void everyCallerBeforeTheSwitchesGetsTheDefaultAndKeepsItsSerialisedForm() {
        assertEquals(GolemGuardTargets.DEFAULT, GolemData.EMPTY.guardTargets());
        var plain = new GolemData(GolemData.GUARD, Collections.nCopies(6, ItemStack.EMPTY), Collections.nCopies(6, 0), false, List.of(),
                GolemUseOptions.DEFAULT, List.of(ORDER));
        assertEquals(GolemGuardTargets.DEFAULT, plain.guardTargets());
        JsonObject json = GolemData.CODEC.encodeStart(JsonOps.INSTANCE, plain).getOrThrow().getAsJsonObject();
        assertFalse(json.has("guard_targets"), "the default is omitted: " + json);
        assertEquals(plain, GolemData.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow());
        assertEquals(GolemGuardTargets.DEFAULT, GolemData.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow().guardTargets());
    }

    @Test void everyMaskOfAnOrderGuardRoundTripsJsonNbtAndTheNetwork() {
        for (int mask = 0; mask <= GolemGuardTargets.ALL; mask++) {
            var original = guard(List.of(ORDER), mask);
            assertEquals(mask, original.guardTargets());
            var json = GolemData.CODEC.encodeStart(JsonOps.INSTANCE, original).getOrThrow();
            assertEquals(mask != GolemGuardTargets.DEFAULT, json.getAsJsonObject().has("guard_targets"), "json omits only the default: " + mask);
            var fromJson = GolemData.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
            assertEquals(original, fromJson);
            assertEquals(mask, fromJson.guardTargets());
            var nbt = GolemData.CODEC.encodeStart(NbtOps.INSTANCE, original).getOrThrow();
            assertEquals(mask, GolemData.CODEC.parse(NbtOps.INSTANCE, nbt).getOrThrow().guardTargets(), "nbt " + mask);
            var wire = buffer();
            GolemData.STREAM_CODEC.encode(wire, original);
            var decoded = GolemData.STREAM_CODEC.decode(wire);
            assertEquals(original, decoded);
            assertEquals(mask, decoded.guardTargets(), "network " + mask);
            assertEquals(0, wire.readableBytes());
        }
    }

    @Test void aMaskNeedsAGuardCoreAndAnOrderUpgrade() {
        assertThrows(IllegalArgumentException.class, () -> guard(List.of(), A), "no Order");
        assertThrows(IllegalArgumentException.class, () -> guard(List.of(FIRE), A), "Fire is not Order");
        assertThrows(IllegalArgumentException.class, () -> guard(List.of(), 0), "switching Monsters off also needs Order");
        for (int core : new int[] {GolemData.HARVEST, GolemData.BUTCHER, GolemData.SORTING, GolemData.FISHING, GolemData.LIQUID, GolemData.ALCHEMY, GolemData.LUMBER}) {
            assertThrows(IllegalArgumentException.class, () -> data(core, List.of(ORDER), A), "core " + core + " has no target switches");
            assertDoesNotThrow(() -> data(core, List.of(ORDER), GolemGuardTargets.DEFAULT));
        }
        assertDoesNotThrow(() -> guard(List.of(ORDER), A | P | C));
        assertDoesNotThrow(() -> guard(List.of(), GolemGuardTargets.DEFAULT));
        assertThrows(IllegalArgumentException.class, () -> guard(List.of(ORDER), 16));
        assertThrows(IllegalArgumentException.class, () -> guard(List.of(ORDER), -1));
    }

    @Test void aMalformedOrOutOfRangeMaskInJsonIsADecodeErrorNeverTheDefault() {
        JsonObject base = GolemData.CODEC.encodeStart(JsonOps.INSTANCE, guard(List.of(ORDER), A)).getOrThrow().getAsJsonObject();
        for (String bad : List.of("16", "-1", "1.5", "\"all\"", "null", "4294967297", "[1]")) {
            JsonObject copy = base.deepCopy();
            copy.add("guard_targets", JsonParser.parseString(bad));
            assertTrue(GolemData.CODEC.parse(JsonOps.INSTANCE, copy).error().isPresent(), bad);
        }
        JsonObject noOrder = GolemData.CODEC.encodeStart(JsonOps.INSTANCE, guard(List.of(), GolemGuardTargets.DEFAULT)).getOrThrow().getAsJsonObject();
        noOrder.add("guard_targets", JsonParser.parseString("2"));
        assertTrue(GolemData.CODEC.parse(JsonOps.INSTANCE, noOrder).error().isPresent(), "a mask without Order is refused, not flattened");
        JsonObject wrongCore = GolemData.CODEC.encodeStart(JsonOps.INSTANCE, data(GolemData.HARVEST, List.of(ORDER), GolemGuardTargets.DEFAULT)).getOrThrow().getAsJsonObject();
        wrongCore.add("guard_targets", JsonParser.parseString("2"));
        assertTrue(GolemData.CODEC.parse(JsonOps.INSTANCE, wrongCore).error().isPresent(), "a mask on a Harvest golem is refused");
    }

    @Test void theNetworkRefusesAnOutOfRangeMask() {
        var wire = buffer();
        GolemData.STREAM_CODEC.encode(wire, guard(List.of(ORDER), A));
        int last = wire.writerIndex() - 1;
        assertEquals(A, wire.getUnsignedByte(last), "the mask is the final byte");
        // Bit 4 (16) is the advanced-golem flag and bit 5 (32) announces a decoration string; only the bits above them are corrupt.
        for (int bad : new int[] {64, 96, 128, 255}) {
            var corrupt = buffer();
            corrupt.writeBytes(wire, 0, last);
            corrupt.writeByte(bad);
            assertThrows(DecoderException.class, () -> GolemData.STREAM_CODEC.decode(corrupt), "mask byte " + bad);
        }
    }

    /** A record equals that skipped the mask would make every round-trip assertion above vacuous for it. */
    @Test void equalityAndHashingCoverTheMask() {
        var base = guard(List.of(ORDER), A);
        assertEquals(base, guard(List.of(ORDER), A));
        assertEquals(base.hashCode(), guard(List.of(ORDER), A).hashCode());
        for (int other : new int[] {0, M, P, C, A | P, GolemGuardTargets.ALL}) {
            assertNotEquals(base, guard(List.of(ORDER), other), "mask " + other);
            assertNotEquals(base.hashCode(), guard(List.of(ORDER), other).hashCode(), "hash of mask " + other);
        }
    }
}
