package thaumcraft.research;

import net.minecraft.nbt.*;
import org.junit.jupiter.api.Test;
import thaumcraft.common.lib.research.PlayerKnowledge;
import static org.junit.jupiter.api.Assertions.*;

class KnowledgeSaveIntegrityTest {
    @Test void nonCompoundOuterAttachmentSurvivesCloneAndSync() {
        PlayerKnowledge owner = new PlayerKnowledge();
        Tag bad = StringTag.valueOf("unread attachment");
        owner.loadAttachment(bad, "test player at 1,2,3");
        assertTrue(owner.isRejected());
        assertEquals(bad, owner.saveAttachment());
        assertEquals(bad, owner.copy().saveAttachment());
        PlayerKnowledge synced = new PlayerKnowledge(); synced.load(owner.save());
        assertEquals(bad, synced.saveAttachment());
        owner.reset(); owner.scanItem("item"); owner.initializePrimalAspects(null);
        assertEquals(bad, owner.saveAttachment());
    }
    @Test void malformedKnowledgeRetainsOwnerAndPreviousLiveValue() {
        PlayerKnowledge owner = new PlayerKnowledge(); owner.addResearch("KNOWN");
        CompoundTag bad = owner.save(); bad.putString("researchComplete", "unread research");
        owner.load(bad);
        assertEquals(bad, owner.save());
        assertTrue(owner.isResearchComplete("KNOWN"));
        assertFalse(owner.addResearch("NEW"));
        owner.setAspectPool("aer", 999); owner.setWarpPerm(99); owner.reset();
        assertThrows(IllegalStateException.class, () -> owner.copyFrom(new PlayerKnowledge()));
        assertEquals(bad, owner.save());
        assertEquals(bad, owner.copy().save());
        PlayerKnowledge synced = new PlayerKnowledge(); synced.load(owner.save());
        assertEquals(bad, synced.save());
    }
    @Test void wrongAspectElementAndUnknownVersionAreNotDefaults() {
        PlayerKnowledge owner = new PlayerKnowledge();
        CompoundTag bad = owner.save();
        ListTag list = new ListTag(); list.add(StringTag.valueOf("unread aspect")); bad.put("discoveredAspects", list);
        owner.load(bad); assertEquals(bad, owner.save());
        bad = new PlayerKnowledge().save(); bad.putInt("dataVersion", 77);
        owner.load(bad); assertEquals(bad, owner.save());
    }
}
