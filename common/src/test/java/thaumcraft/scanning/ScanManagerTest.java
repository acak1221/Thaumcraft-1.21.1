package thaumcraft.scanning;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.lib.research.PlayerKnowledge;
import thaumcraft.common.lib.research.ScanManager;
import thaumcraft.common.lib.research.ScanManager.ScanKind;
import static org.junit.jupiter.api.Assertions.*;

public class ScanManagerTest {
    @Test
    public void ordinaryNodeScansUseMinimumFourThenIntegerTenthsWithoutConsumingVis() {
        AspectList node = new AspectList().add(Aspect.FIRE, 49).add(Aspect.AIR, 25)
                .add(Aspect.EARTH, 50).add(Aspect.WATER, 1);
        AspectList rewards = ScanManager.nodeScanAspects(node);
        assertArrayEquals(new Aspect[]{Aspect.AIR, Aspect.WATER, Aspect.FIRE, Aspect.EARTH}, rewards.getAspects());
        assertEquals(4, rewards.getAmount(Aspect.AIR));
        assertEquals(4, rewards.getAmount(Aspect.WATER));
        assertEquals(4, rewards.getAmount(Aspect.FIRE));
        assertEquals(5, rewards.getAmount(Aspect.EARTH));

        PlayerKnowledge knowledge = new PlayerKnowledge();
        ScanManager.ScanOutcome result = ScanManager.awardScan(knowledge, "@NODE-native-id", rewards,
                ScanKind.PHENOMENA, 100);
        assertTrue(result.success());
        assertEquals(4, knowledge.getAspectPoolFor(Aspect.AIR.getTag()));
        assertEquals(5, knowledge.getAspectPoolFor(Aspect.EARTH.getTag()));
        assertTrue(knowledge.hasScannedPhenomena("@NODE-native-id"));
        assertTrue(knowledge.getScannedItems().isEmpty());
        assertTrue(knowledge.getScannedEntities().isEmpty());
        assertEquals(25, node.getAmount(Aspect.AIR));
        assertEquals(1, node.getAmount(Aspect.WATER));
        assertEquals(49, node.getAmount(Aspect.FIRE));
        assertEquals(50, node.getAmount(Aspect.EARTH));
        assertEquals(ScanManager.Failure.DUPLICATE,
                ScanManager.awardScan(knowledge, "@NODE-native-id", rewards, ScanKind.PHENOMENA, 100).failure());
        assertEquals(4, knowledge.getAspectPoolFor(Aspect.AIR.getTag()));
    }

    @Test
    public void nodeRewardConversionDoesNotAlterOrdinaryItemAwardsOrCompoundDiscoveryBonus() {
        PlayerKnowledge knowledge = new PlayerKnowledge();
        assertTrue(scan(knowledge, "@minecraft:raw_amount", new AspectList().add(Aspect.AIR, 25)).success());
        assertEquals(25, knowledge.getAspectPoolFor(Aspect.AIR.getTag()));
        AspectList compoundNode = new AspectList().add(Aspect.LIGHT, 25);
        ScanManager.ScanOutcome discovery = ScanManager.awardScan(knowledge, "@NODE-compound",
                ScanManager.nodeScanAspects(compoundNode), ScanKind.PHENOMENA, 100);
        assertTrue(discovery.success());
        assertEquals(6, knowledge.getAspectPoolFor(Aspect.LIGHT.getTag()));
        assertEquals(25, compoundNode.getAmount(Aspect.LIGHT));
        assertEquals(0, ScanManager.nodeScanAspects(new AspectList()).size());
    }

    @Test
    public void firstCompoundDiscoveryAddsTwoPointsThenAwardsNormalAmount() {
        PlayerKnowledge knowledge = new PlayerKnowledge();
        ScanManager.ScanOutcome first = scan(knowledge, "minecraft:torch", new AspectList().add(Aspect.LIGHT, 3));
        assertTrue(first.success());
        assertEquals(5, first.awards().get(Aspect.LIGHT.getTag()).intValue());
        assertTrue(first.discovered().contains(Aspect.LIGHT.getTag()));
        assertTrue(knowledge.hasDiscoveredAspect(Aspect.LIGHT.getTag()));
        assertEquals(5, knowledge.getAspectPoolFor(Aspect.LIGHT.getTag()));

        ScanManager.ScanOutcome second = scan(knowledge, "minecraft:glowstone", new AspectList().add(Aspect.LIGHT, 3));
        assertTrue(second.success());
        assertTrue(second.discovered().isEmpty());
        assertEquals(3, second.awards().get(Aspect.LIGHT.getTag()).intValue());
        assertEquals(8, knowledge.getAspectPoolFor(Aspect.LIGHT.getTag()));
    }

    @Test
    public void missingParentRejectsEntireScanWithoutChangingAnyPoolOrMarker() {
        PlayerKnowledge knowledge = new PlayerKnowledge();
        knowledge.setAspectPool(Aspect.AIR.getTag(), 17);
        CompoundTag before = knowledge.save();
        AspectList aspects = new AspectList().add(Aspect.AIR, 4).add(Aspect.MAGIC, 2);
        ScanManager.ScanOutcome rejected = scan(knowledge, "thaumcraft:magic_object", aspects);
        assertFalse(rejected.success());
        assertEquals(ScanManager.Failure.MISSING_PARENT, rejected.failure());
        assertEquals(Aspect.VOID, rejected.missingParent());
        assertEquals(before, knowledge.save());
        assertFalse(knowledge.hasScannedItem("thaumcraft:magic_object"));

        knowledge.addDiscoveredAspect(Aspect.VOID.getTag());
        knowledge.addDiscoveredAspect(Aspect.ENERGY.getTag());
        assertTrue(scan(knowledge, "thaumcraft:magic_object", aspects).success());
        assertEquals(21, knowledge.getAspectPoolFor(Aspect.AIR.getTag()));
        assertEquals(4, knowledge.getAspectPoolFor(Aspect.MAGIC.getTag()));
    }

    @Test
    public void parentContainedInSameScanDoesNotCountAsPreviouslyDiscovered() {
        PlayerKnowledge knowledge = new PlayerKnowledge();
        AspectList aspects = new AspectList().add(Aspect.VOID, 2).add(Aspect.ENERGY, 2).add(Aspect.MAGIC, 2);
        ScanManager.ScanOutcome rejected = scan(knowledge, "thaumcraft:complex_object", aspects);
        assertFalse(rejected.success());
        assertFalse(knowledge.hasDiscoveredAspect(Aspect.VOID.getTag()));
        assertFalse(knowledge.hasDiscoveredAspect(Aspect.ENERGY.getTag()));
        assertFalse(knowledge.hasDiscoveredAspect(Aspect.MAGIC.getTag()));
    }

    @Test
    public void duplicateScanCannotRepeatAwardsEvenWithHigherSuppliedAmounts() {
        PlayerKnowledge knowledge = new PlayerKnowledge();
        assertTrue(scan(knowledge, "minecraft:stone", new AspectList().add(Aspect.EARTH, 2)).success());
        CompoundTag before = knowledge.save();
        ScanManager.ScanOutcome replay = scan(knowledge, "minecraft:stone", new AspectList().add(Aspect.EARTH, 100));
        assertFalse(replay.success());
        assertEquals(ScanManager.Failure.DUPLICATE, replay.failure());
        assertTrue(replay.awards().isEmpty());
        assertEquals(before, knowledge.save());
    }

    @Test
    public void diminishingReturnsUseInclusiveThresholdsRatherThanHardCap() {
        assertEquals(9, earnedAtPool(99, 9));
        assertEquals(3, earnedAtPool(100, 9));
        assertEquals(3, earnedAtPool(124, 9));
        assertEquals(1, earnedAtPool(125, 9));
        assertEquals(1, earnedAtPool(1000, 9));
        assertEquals(2, earnedAtPool(100, 8));
    }

    @Test public void zeroAndNegativeConfiguredCapsKeepOriginalSoftCapArithmetic() {
        for (int cap : new int[]{0, -1, Integer.MIN_VALUE}) {
            var knowledge = new PlayerKnowledge();
            var result = ScanManager.awardScan(knowledge, "cap:" + cap, new AspectList().add(Aspect.AIR, 9), ScanKind.ITEM, cap);
            assertTrue(result.success()); assertEquals(1, result.awards().get("aer").intValue());
            assertEquals(1, knowledge.getAspectPoolFor("aer"));
        }
    }

    @Test
    public void crossingThresholdDuringScanUsesPoolBeforeTheAward() {
        PlayerKnowledge knowledge = new PlayerKnowledge();
        knowledge.setAspectPool(Aspect.AIR.getTag(), 99);
        ScanManager.ScanOutcome result = scan(knowledge, "minecraft:threshold", new AspectList().add(Aspect.AIR, 9));
        assertEquals(9, result.awards().get(Aspect.AIR.getTag()).intValue());
        assertEquals(108, knowledge.getAspectPoolFor(Aspect.AIR.getTag()));
    }

    @Test
    public void emptyScanDoesNotRecordTargetAndZeroCompoundCanStillBeDiscovered() {
        PlayerKnowledge knowledge = new PlayerKnowledge();
        assertFalse(scan(knowledge, "minecraft:empty", new AspectList()).success());
        assertFalse(knowledge.hasScannedItem("minecraft:empty"));
        ScanManager.ScanOutcome zero = scan(knowledge, "minecraft:zero_light", new AspectList().add(Aspect.LIGHT, 0));
        assertTrue(zero.success());
        assertEquals(2, zero.awards().get(Aspect.LIGHT.getTag()).intValue());
    }

    @Test
    public void scanKindsMaintainSeparateDuplicateSetsAndSurviveSave() {
        PlayerKnowledge knowledge = new PlayerKnowledge();
        AspectList aspects = new AspectList().add(Aspect.AIR, 1);
        for (ScanKind kind : ScanKind.values()) {
            assertTrue(ScanManager.awardScan(knowledge, "same_native_id", aspects, kind, 100).success());
        }
        assertEquals(3, knowledge.getAspectPoolFor(Aspect.AIR.getTag()));
        PlayerKnowledge loaded = new PlayerKnowledge();
        loaded.load(knowledge.save());
        for (ScanKind kind : ScanKind.values()) {
            assertEquals(ScanManager.Failure.DUPLICATE,
                    ScanManager.awardScan(loaded, "same_native_id", aspects, kind, 100).failure());
        }
    }

    @Test
    public void callerOwnedAspectAmountsRemainUnchanged() {
        PlayerKnowledge knowledge = new PlayerKnowledge();
        AspectList aspects = new AspectList().add(Aspect.LIGHT, 3);
        assertTrue(scan(knowledge, "minecraft:immutable_input", aspects).success());
        assertEquals(3, aspects.getAmount(Aspect.LIGHT));
    }

    private static ScanManager.ScanOutcome scan(PlayerKnowledge knowledge, String key, AspectList aspects) {
        return ScanManager.awardScan(knowledge, key, aspects, ScanKind.ITEM, 100);
    }

    private static int earnedAtPool(int pool, int amount) {
        PlayerKnowledge knowledge = new PlayerKnowledge();
        knowledge.setAspectPool(Aspect.AIR.getTag(), pool);
        return scan(knowledge, "minecraft:cap", new AspectList().add(Aspect.AIR, amount))
                .awards().get(Aspect.AIR.getTag());
    }
}
