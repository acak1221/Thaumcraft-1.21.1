package thaumcraft.api.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumcraft.api.ThaumcraftApiHelper.Scans;
import thaumcraft.common.lib.research.PlayerKnowledge;

/**
 * The rules of the scan services of the installed handler, on a real {@link PlayerKnowledge} (the player itself, and the
 * sync packet, are checked in a running world by the GameTest {@code knowledgeApiScansAndDiscovery}).
 */
class ScanServicesTest {
    @BeforeAll static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    private static Set<String> set(String... keys) { return new LinkedHashSet<>(List.of(keys)); }

    private static Scans items(String... keys) { return new Scans(set(keys), Set.of(), Set.of()); }

    // ------------------------------------------------------------------------------------------ hasScannedItem

    @Test void anItemIsScannedUnderTheMarkerTheThaumometerWritesForIt() {
        PlayerKnowledge knowledge = new PlayerKnowledge();
        ItemStack diamond = new ItemStack(Items.DIAMOND, 3);
        assertFalse(ThaumcraftInternalMethods.scannedItem(knowledge, diamond));
        knowledge.scanItem("@minecraft:diamond");
        assertTrue(ThaumcraftInternalMethods.scannedItem(knowledge, diamond), "the thaumometer marker is the registry id");
        assertTrue(ThaumcraftInternalMethods.scannedItem(knowledge, new ItemStack(Items.DIAMOND)), "the count of the stack is no part of it");
        assertFalse(ThaumcraftInternalMethods.scannedItem(knowledge, new ItemStack(Items.EMERALD)));
    }

    @Test void theAdvancedMarkerCountsAsScannedAndOtherKindsDoNot() {
        PlayerKnowledge knowledge = new PlayerKnowledge();
        knowledge.scanItem("#minecraft:stone");
        assertTrue(ThaumcraftInternalMethods.scannedItem(knowledge, new ItemStack(Items.STONE)));
        // An entity or a phenomenon with the same text is not an item scan.
        PlayerKnowledge mobs = new PlayerKnowledge();
        mobs.scanEntity("@minecraft:diamond");
        mobs.scanPhenomena("@minecraft:diamond");
        assertFalse(ThaumcraftInternalMethods.scannedItem(mobs, new ItemStack(Items.DIAMOND)));
    }

    @Test void nothingIsScannedForNoStackNoKnowledgeOrAnEmptyStack() {
        PlayerKnowledge knowledge = new PlayerKnowledge();
        knowledge.scanItem("@minecraft:air");
        assertFalse(ThaumcraftInternalMethods.scannedItem(knowledge, ItemStack.EMPTY));
        assertFalse(ThaumcraftInternalMethods.scannedItem(knowledge, null));
        assertFalse(ThaumcraftInternalMethods.scannedItem(null, new ItemStack(Items.DIAMOND)));
    }

    // -------------------------------------------------------------------------------------------- addScans

    @Test void everyKindIsAddedAndCounted() {
        PlayerKnowledge knowledge = new PlayerKnowledge();
        Scans scans = new Scans(set("@minecraft:stone", "#minecraft:dirt"), set("@entity:Pig"), set("@NODEabc"));
        assertEquals(4, ThaumcraftInternalMethods.mergeScans(knowledge, scans));
        assertEquals(scans.items(), knowledge.getScannedItems());
        assertEquals(scans.entities(), knowledge.getScannedEntities());
        assertEquals(scans.phenomena(), knowledge.getScannedPhenomena());
        assertEquals(List.of("@minecraft:stone", "#minecraft:dirt"), List.copyOf(knowledge.getScannedItems()), "order kept");
    }

    @Test void addingTheSameScansAgainChangesNothing() {
        PlayerKnowledge knowledge = new PlayerKnowledge();
        Scans scans = new Scans(set("@minecraft:stone", "#minecraft:dirt"), set("@entity:Pig"), set("@NODEabc"));
        ThaumcraftInternalMethods.mergeScans(knowledge, scans);
        var before = knowledge.save();
        assertEquals(0, ThaumcraftInternalMethods.mergeScans(knowledge, scans));
        assertEquals(before, knowledge.save());
        assertEquals(0, ThaumcraftInternalMethods.mergeScans(knowledge, Scans.EMPTY));
    }

    @Test void aBasicMarkerNeverReplacesAnAdvancedOneButAnAdvancedOneReplacesABasicOne() {
        PlayerKnowledge knowledge = new PlayerKnowledge();
        knowledge.scanItem("#minecraft:gold_ingot");
        knowledge.scanItem("@minecraft:dirt");
        knowledge.scanEntity("#entity:Pig");
        knowledge.scanPhenomena("@NODEabc");
        Scans incoming = new Scans(set("@minecraft:gold_ingot", "#minecraft:dirt"), set("@entity:Pig"), set("#NODEabc"));

        assertEquals(2, ThaumcraftInternalMethods.mergeScans(knowledge, incoming), "only the two upgrades are new");
        assertEquals(Set.of("#minecraft:gold_ingot", "#minecraft:dirt"), knowledge.getScannedItems(),
                "the advanced marker stays, and replaced the basic marker of the dirt");
        assertEquals(Set.of("#entity:Pig"), knowledge.getScannedEntities());
        assertEquals(Set.of("#NODEabc"), knowledge.getScannedPhenomena());
        assertTrue(ThaumcraftInternalMethods.scannedItem(knowledge, new ItemStack(Items.DIRT)));
    }

    @Test void aBasicMarkerAlreadyHeldIsNotNewAndTheOtherPlayersScansAreUnchanged() {
        PlayerKnowledge source = new PlayerKnowledge();
        source.scanItem("@minecraft:stone");
        source.scanItem("@minecraft:dirt");
        PlayerKnowledge target = new PlayerKnowledge();
        target.scanItem("@minecraft:dirt");
        Scans scans = new Scans(source.getScannedItems(), source.getScannedEntities(), source.getScannedPhenomena());
        assertEquals(1, ThaumcraftInternalMethods.mergeScans(target, scans), "the dirt was already held");
        assertEquals(Set.of("@minecraft:dirt", "@minecraft:stone"), target.getScannedItems());
        assertEquals(Set.of("@minecraft:stone", "@minecraft:dirt"), source.getScannedItems(), "the source is not touched");
    }

    @Test void knowledgeThatCouldNotBeReadTakesNoScans() {
        PlayerKnowledge rejected = new PlayerKnowledge();
        rejected.loadAttachment(net.minecraft.nbt.StringTag.valueOf("not a compound"), "test");
        assertTrue(rejected.isRejected());
        assertEquals(0, ThaumcraftInternalMethods.mergeScans(rejected, items("@minecraft:stone")), "unread knowledge is retained untouched");
        assertTrue(rejected.getScannedItems().isEmpty());
    }

    /** Pins the claim of the discoverAspect Javadoc: a positive pool entry is what "discovered" means in the knowledge. */
    @Test void anAspectIsDiscoveredExactlyWhenItHasAPoolEntry() {
        PlayerKnowledge knowledge = new PlayerKnowledge();
        assertFalse(knowledge.hasDiscoveredAspect("lux"));
        assertTrue(knowledge.addDiscoveredAspect("lux"), "the quiet discovery that discoverAspect ends in");
        assertTrue(knowledge.hasDiscoveredAspect("lux"));
        assertEquals(0, knowledge.getAspectPoolFor("lux"), "with an empty pool");
        assertFalse(knowledge.addDiscoveredAspect("lux"), "a second discovery is no news");
        knowledge.addAspectPool("lux", 3);
        assertEquals(3, knowledge.getAspectPoolFor("lux"));
        assertFalse(knowledge.addDiscoveredAspect("lux"), "and a discovery never touches a pool");
        assertEquals(3, knowledge.getAspectPoolFor("lux"));

        assertFalse(knowledge.hasDiscoveredAspect("gelum"));
        knowledge.addAspectPool("gelum", 1);
        assertTrue(knowledge.hasDiscoveredAspect("gelum"), "a positive pool discovers the aspect as well");
    }

    @Test void aScansSnapshotComesBackAsWhatTheKnowledgeHolds() {
        PlayerKnowledge knowledge = new PlayerKnowledge();
        knowledge.scanItem("@minecraft:stone");
        knowledge.scanEntity("@entity:Pig");
        knowledge.scanPhenomena("@NODEabc");
        Scans scans = new Scans(knowledge.getScannedItems(), knowledge.getScannedEntities(), knowledge.getScannedPhenomena());
        assertEquals(new Scans(Set.of("@minecraft:stone"), Set.of("@entity:Pig"), Set.of("@NODEabc")), scans);
    }
}
