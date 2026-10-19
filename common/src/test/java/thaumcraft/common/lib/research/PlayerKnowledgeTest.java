package thaumcraft.common.lib.research;

import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class PlayerKnowledgeTest {
    @Test
    public void newPlayerReceivesTc4PrimalPoolsOnlyOnce() {
        PlayerKnowledge knowledge = new PlayerKnowledge();
        assertFalse(knowledge.hasInitializedAspects());
        knowledge.initializePrimalAspects(RandomSource.create(1234));
        for (String primal : PlayerKnowledge.PRIMAL_TAGS) {
            assertTrue(knowledge.hasDiscoveredAspect(primal));
            assertTrue(knowledge.getAspectPoolFor(primal) >= 15);
            assertTrue(knowledge.getAspectPoolFor(primal) <= 19);
        }
        knowledge.setAspectPool("aer", 0);
        knowledge.initializePrimalAspects(RandomSource.create(999));
        assertEquals(0, knowledge.getAspectPoolFor("aer"));
        assertTrue(knowledge.hasDiscoveredAspect("aer"));
    }

    @Test
    public void nativeRoundTripPreservesZeroDiscoveryResearchScansAndWarp() {
        PlayerKnowledge source = new PlayerKnowledge();
        source.initializePrimalAspects(RandomSource.create(1234));
        source.setAspectPool("praecantatio", 0);
        source.setAspectPool("aer", -3);
        source.addResearch("ASPECTS");
        source.addResearch("@NITOR");
        source.scanItem("@minecraft:stone");
        source.scanEntity("@minecraft:zombie");
        source.scanPhenomena("@NODE:example");
        source.setWarpPerm(5);
        source.setWarpSticky(3);
        source.setWarpTemp(1);
        source.setWarpCounter(-9);
        CompoundTag saved = source.save();
        PlayerKnowledge restored = new PlayerKnowledge();
        restored.load(saved);

        assertEquals(PlayerKnowledge.DATA_VERSION, saved.getInt("dataVersion"));
        assertTrue(restored.hasInitializedAspects());
        assertTrue(restored.hasDiscoveredAspect("praecantatio"));
        assertEquals(0, restored.getAspectPoolFor("praecantatio"));
        assertEquals(-3, restored.getAspectPoolFor("aer"));
        assertEquals(source.getResearchComplete(), restored.getResearchComplete());
        assertEquals(source.getScannedItems(), restored.getScannedItems());
        assertEquals(source.getScannedEntities(), restored.getScannedEntities());
        assertEquals(source.getScannedPhenomena(), restored.getScannedPhenomena());
        assertEquals(9, restored.getTotalWarp());
        assertEquals(-9, restored.getWarpCounter());
    }

    @Test
    public void deathSnapshotDoesNotAliasOldPlayerCollections() {
        PlayerKnowledge original = new PlayerKnowledge();
        original.initializePrimalAspects(RandomSource.create(1));
        original.addResearch("ASPECTS");
        original.scanItem("@minecraft:stone");
        PlayerKnowledge clone = original.copy();

        original.removeResearch("ASPECTS");
        original.setAspectPool("aer", 99);
        clone.scanItem("@minecraft:dirt");
        assertTrue(clone.isResearchComplete("ASPECTS"));
        assertNotEquals(99, clone.getAspectPoolFor("aer"));
        assertFalse(original.hasScannedItem("@minecraft:dirt"));
        assertTrue(clone.hasInitializedAspects());
    }

    @Test
    public void receivedSnapshotReplacesPreviousPlayerState() {
        PlayerKnowledge oldClient = new PlayerKnowledge();
        oldClient.addResearch("OLD");
        oldClient.scanItem("@old");
        PlayerKnowledge newServerState = new PlayerKnowledge();
        newServerState.addResearch("NEW");
        newServerState.scanItem("@new");
        oldClient.load(newServerState.save());

        assertEquals(Set.of("NEW"), oldClient.getResearchComplete());
        assertEquals(Set.of("@new"), oldClient.getScannedItems());
    }

    @Test
    public void poolOverdrawKeepsTc4ObservableQuirkAndZeroDiscovery() {
        PlayerKnowledge knowledge = new PlayerKnowledge();
        knowledge.setAspectPool("aer", 2);
        assertTrue(knowledge.addAspectPool("aer", -5));
        assertEquals(2, knowledge.getAspectPoolFor("aer"));
        assertTrue(knowledge.addAspectPool("aer", -2));
        assertEquals(0, knowledge.getAspectPoolFor("aer"));
        assertTrue(knowledge.hasDiscoveredAspect("aer"));
        assertFalse(knowledge.addAspectPool("aer", -1));
    }

    @Test
    public void advancedScanReplacesThaumometerMarkerOnlyForSameObject() {
        PlayerKnowledge knowledge = new PlayerKnowledge();
        knowledge.scanItem("@stone");
        knowledge.scanItem("@dirt");
        knowledge.scanItem("#stone");
        assertFalse(knowledge.hasScannedItem("@stone"));
        assertTrue(knowledge.hasScannedItem("#stone"));
        assertTrue(knowledge.hasScannedItem("@dirt"));
    }

    @Test
    public void warpPartsClampButCounterRemainsSignedAcrossSave() {
        PlayerKnowledge source = new PlayerKnowledge();
        source.setWarpPerm(-1);
        source.setWarpSticky(2);
        source.addWarpSticky(-10);
        source.setWarpTemp(-3);
        source.setWarpCounter(-4);
        PlayerKnowledge restored = new PlayerKnowledge();
        restored.load(source.save());
        assertEquals(0, restored.getTotalWarp());
        assertEquals(-4, restored.getWarpCounter());
    }
}
