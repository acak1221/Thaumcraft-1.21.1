package thaumcraft.common.lib.research;

import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.StringTag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Item-source caches must survive unrelated syncs and forget every actual membership change. */
class PlayerKnowledgeItemScanRevisionTest {
    @Test void additionsDuplicatesAndAdvancedReplacementHaveDistinctTokens() {
        PlayerKnowledge knowledge = new PlayerKnowledge();
        assertEquals(0, knowledge.itemScanRevision());
        assertTrue(knowledge.scanItem("@stone"));
        long ordinary = knowledge.itemScanRevision();
        Set<String> beforeAdvanced = knowledge.getScannedItems();
        assertTrue(ordinary > 0);
        assertFalse(knowledge.scanItem("@stone"));
        assertFalse(knowledge.scanItem(null));
        assertFalse(knowledge.scanItem(""));
        assertEquals(ordinary, knowledge.itemScanRevision());

        assertTrue(knowledge.scanItem("#stone"));
        assertEquals(ordinary + 1, knowledge.itemScanRevision());
        assertEquals(Set.of("#stone"), knowledge.getScannedItems());
        assertEquals(Set.of("@stone"), beforeAdvanced, "existing immutable snapshots remain independent");
        assertThrows(UnsupportedOperationException.class, () -> beforeAdvanced.add("@dirt"));
        assertFalse(knowledge.scanItem("#stone"));
        assertEquals(ordinary + 1, knowledge.itemScanRevision());
    }

    @Test void duplicateAdvancedMarkerStillInvalidatesWhenItRemovesAnOrdinaryMarker() {
        PlayerKnowledge knowledge = new PlayerKnowledge();
        knowledge.scanItem("#stone");
        knowledge.scanItem("@stone");
        long before = knowledge.itemScanRevision();
        assertFalse(knowledge.scanItem("#stone"), "preserve the existing added-marker return value");
        assertEquals(Set.of("#stone"), knowledge.getScannedItems());
        assertEquals(before + 1, knowledge.itemScanRevision());
    }

    @Test void unrelatedKnowledgeAndAnAlreadyEmptyResetDoNotInvalidateItemSources() {
        PlayerKnowledge knowledge = new PlayerKnowledge();
        knowledge.reset();
        assertEquals(0, knowledge.itemScanRevision());
        knowledge.scanItem("@stone");
        long before = knowledge.itemScanRevision();
        knowledge.addResearch("ASPECTS");
        knowledge.setAspectPool("aer", 99);
        knowledge.scanEntity("@entity:Pig");
        knowledge.scanPhenomena("@NODEexample");
        knowledge.setWarpPerm(4);
        assertEquals(before, knowledge.itemScanRevision());
        knowledge.reset();
        assertEquals(before + 1, knowledge.itemScanRevision());
        knowledge.reset();
        assertEquals(before + 1, knowledge.itemScanRevision());
    }

    @Test void copyComparesMembershipAndKeepsEachOwnersRevisionIndependent() {
        PlayerKnowledge target = new PlayerKnowledge();
        target.scanItem("@stone");
        target.scanItem("@dirt");
        long before = target.itemScanRevision();
        PlayerKnowledge same = new PlayerKnowledge();
        same.scanItem("@dirt");
        same.scanItem("@stone");
        same.scanEntity("@entity:Pig");
        same.addResearch("NEW");
        target.copyFrom(same);
        assertEquals(before, target.itemScanRevision(), "set order and non-item data do not affect membership");
        assertTrue(target.isResearchComplete("NEW"));
        target.copyFrom(target);
        assertEquals(before, target.itemScanRevision());

        PlayerKnowledge replacement = new PlayerKnowledge();
        replacement.scanItem("@stone");
        replacement.scanItem("#clay");
        target.copyFrom(replacement);
        assertEquals(before + 1, target.itemScanRevision(), "same-size replacement still invalidates");
        assertEquals(replacement.getScannedItems(), target.getScannedItems());
        assertEquals(2, replacement.itemScanRevision(), "a copy never imports or alters the source token");
    }

    @Test void fullLoadOnlyInvalidatesWhenTheFinalItemSetChanges() {
        PlayerKnowledge target = new PlayerKnowledge();
        target.scanItem("@stone");
        target.scanItem("@dirt");
        long before = target.itemScanRevision();
        PlayerKnowledge same = new PlayerKnowledge();
        same.scanItem("@dirt");
        same.scanItem("@stone");
        same.setAspectPool("aer", 71);
        same.addResearch("NEW");
        target.load(same.save());
        assertEquals(before, target.itemScanRevision(), "unrelated server snapshots must not restart item resolution");
        assertEquals(71, target.getAspectPoolFor("aer"));

        PlayerKnowledge replacement = new PlayerKnowledge();
        replacement.scanItem("@clay");
        replacement.scanItem("@stone");
        target.load(replacement.save());
        assertEquals(before + 1, target.itemScanRevision());
        target.load(new PlayerKnowledge().save());
        assertEquals(before + 2, target.itemScanRevision());
        target.load(new PlayerKnowledge().save());
        assertEquals(before + 2, target.itemScanRevision());
        assertFalse(target.save().contains("itemScanRevision"), "cache tokens are not persistent player data");
    }

    @Test void rejectedLoadsAndBlockedMutationsKeepTheExistingMembershipToken() {
        PlayerKnowledge target = new PlayerKnowledge();
        target.scanItem("@stone");
        long before = target.itemScanRevision();
        CompoundTag malformed = target.save();
        malformed.putString("scannedItems", "unread item scans");
        target.load(malformed);
        assertTrue(target.isRejected());
        assertEquals(before, target.itemScanRevision());
        assertEquals(Set.of("@stone"), target.getScannedItems());
        target.reset();
        assertFalse(target.scanItem("@clay"));
        assertEquals(before, target.itemScanRevision());
        target.loadAttachment(StringTag.valueOf("unread attachment"), "item revision fixture");
        assertEquals(before, target.itemScanRevision());
        target.load(target.save());
        assertEquals(before, target.itemScanRevision(), "a retained rejection envelope does not change live scans");

        PlayerKnowledge replacement = new PlayerKnowledge();
        replacement.scanItem("@clay");
        target.loadAttachment(replacement.save(), "repaired revision fixture");
        assertFalse(target.isRejected());
        assertEquals(before + 1, target.itemScanRevision());
        assertEquals(Set.of("@clay"), target.getScannedItems());
    }
}
