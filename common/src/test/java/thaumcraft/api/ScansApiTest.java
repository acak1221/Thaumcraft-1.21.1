package thaumcraft.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import thaumcraft.api.ThaumcraftApiHelper.Scans;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.internal.IInternalMethodHandler;

/**
 * The value type of the scan helpers and the contract of the helpers against a handler that is not the installed one.
 * What the installed handler does with a real player is checked in a running world (GameTest
 * {@code knowledgeApiScansAndDiscovery}) and, for the markers themselves, in {@code ScanServicesTest}.
 */
class ScansApiTest {
    private IInternalMethodHandler installed;

    @BeforeAll static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @BeforeEach void remember() { installed = ThaumcraftApi.internalMethods; }

    @AfterEach void restore() { ThaumcraftApi.internalMethods = installed; }

    private static Set<String> set(String... keys) { return new LinkedHashSet<>(List.of(keys)); }

    @Test void theSnapshotKeepsTheOrderOfEveryKindAndIsImmutable() {
        Set<String> items = set("@minecraft:stone", "#minecraft:diamond", "@minecraft:dirt");
        Scans scans = new Scans(items, set("@entity:Pig"), set("@NODEabc"));
        assertEquals(List.of("@minecraft:stone", "#minecraft:diamond", "@minecraft:dirt"), List.copyOf(scans.items()));
        assertEquals(Set.of("@entity:Pig"), scans.entities());
        assertEquals(Set.of("@NODEabc"), scans.phenomena());
        assertEquals(5, scans.size());
        assertFalse(scans.isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> scans.items().add("@minecraft:gold_ingot"));
        assertThrows(UnsupportedOperationException.class, () -> scans.entities().clear());
        items.add("@minecraft:sand");
        assertEquals(3, scans.items().size(), "the snapshot is a copy of the set it was built from");
        assertEquals(scans, new Scans(set("@minecraft:stone", "#minecraft:diamond", "@minecraft:dirt"), set("@entity:Pig"), set("@NODEabc")));
        assertEquals(scans.hashCode(), new Scans(set("@minecraft:stone", "#minecraft:diamond", "@minecraft:dirt"), set("@entity:Pig"), set("@NODEabc")).hashCode());
    }

    @Test void theEmptySnapshotHoldsNothing() {
        assertTrue(Scans.EMPTY.isEmpty());
        assertEquals(0, Scans.EMPTY.size());
        assertEquals(Scans.EMPTY, new Scans(Set.of(), Set.of(), Set.of()));
        assertThrows(UnsupportedOperationException.class, () -> Scans.EMPTY.items().add("@x"));
    }

    @Test void markersAreNeverNullEmptyOrTooLongToSync() {
        assertThrows(NullPointerException.class, () -> new Scans(null, Set.of(), Set.of()));
        assertThrows(NullPointerException.class, () -> new Scans(Set.of(), null, Set.of()));
        assertThrows(NullPointerException.class, () -> new Scans(Set.of(), Set.of(), null));
        Set<String> withNull = new LinkedHashSet<>();
        withNull.add(null);
        assertThrows(IllegalArgumentException.class, () -> new Scans(withNull, Set.of(), Set.of()));
        assertThrows(IllegalArgumentException.class, () -> new Scans(Set.of(""), Set.of(), Set.of()));
        assertThrows(IllegalArgumentException.class, () -> new Scans(Set.of(), Set.of("x".repeat(Scans.MAX_KEY_LENGTH + 1)), Set.of()));
        Scans longest = new Scans(Set.of(), Set.of(), Set.of("x".repeat(Scans.MAX_KEY_LENGTH)));
        assertEquals(1, longest.size());
    }

    @Test void aHandlerThatDoesNotKnowTheServicesAnswersInertlyAndTheHelpersNormaliseIt() {
        // The defaults of IInternalMethodHandler are what a handler written for the original still gets.
        ThaumcraftApi.internalMethods = new IInternalMethodHandler() {};
        assertFalse(ThaumcraftApiHelper.hasScannedItem(null, new ItemStack(Items.DIAMOND)));
        assertSame(Scans.EMPTY, ThaumcraftApiHelper.getScans(null));
        assertEquals(0, ThaumcraftApiHelper.addScans(null, new Scans(Set.of("@minecraft:stone"), Set.of(), Set.of())));
        assertFalse(ThaumcraftApiHelper.discoverAspect(null, Aspect.FIRE));
    }

    @Test void aHandlerThatAnswersNullGivesAnEmptySnapshot() {
        ThaumcraftApi.internalMethods = new IInternalMethodHandler() {
            @Override public Scans getScans(net.minecraft.world.entity.player.Player player) { return null; }
        };
        assertSame(Scans.EMPTY, ThaumcraftApiHelper.getScans(null));
    }

    @Test void theHelpersHandTheirArgumentsToTheHandlerUnchanged() {
        List<Object> calls = new ArrayList<>();
        Scans scans = new Scans(Set.of("@minecraft:stone"), Set.of(), Set.of());
        ItemStack stack = new ItemStack(Items.DIAMOND);
        ThaumcraftApi.internalMethods = new IInternalMethodHandler() {
            @Override public boolean hasScannedItem(net.minecraft.world.entity.player.Player player, ItemStack given) {
                calls.add(given);
                return true;
            }
            @Override public int addScans(net.minecraft.world.entity.player.Player player, Scans given) {
                calls.add(given);
                return 7;
            }
            @Override public Scans getScans(net.minecraft.world.entity.player.Player player) { return scans; }
        };
        assertTrue(ThaumcraftApiHelper.hasScannedItem(null, stack));
        assertEquals(7, ThaumcraftApiHelper.addScans(null, scans));
        assertSame(scans, ThaumcraftApiHelper.getScans(null));
        assertNotSame(Scans.EMPTY, ThaumcraftApiHelper.getScans(null));
        assertSame(stack, calls.get(0));
        assertSame(scans, calls.get(1));
    }
}
