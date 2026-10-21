package thaumcraft.equipment;

import org.junit.jupiter.api.Test;
import thaumcraft.common.lib.items.EquipmentRules;

import static org.junit.jupiter.api.Assertions.*;

/** The numbers and decisions of the original equipment and relic items. */
class LegacyItemsRulesTest {
    @Test void boneBowDrawsTwiceAsFastAsTheVanillaBowAndCapsAtOne() {
        // Original ItemBowBone.func_77615_a:79-84: f = (float)j / 10.0f; f = (f * f + f * 2.0f) / 3.0f, capped at one.
        // The single precision sum is not always the decimal literal (the draw of two ticks is 0.14666666, not
        // 0.14666667), so the expectation is the original's own expression.
        assertEquals(0.0F, EquipmentRules.boneBowPower(0));
        assertEquals(boneBowPower(1), EquipmentRules.boneBowPower(1));
        assertEquals(boneBowPower(2), EquipmentRules.boneBowPower(2));
        assertEquals(boneBowPower(5), EquipmentRules.boneBowPower(5));
        assertEquals(1.0F, EquipmentRules.boneBowPower(10));
        assertEquals(1.0F, EquipmentRules.boneBowPower(18));
        assertEquals(1.0F, EquipmentRules.boneBowPower(72000));
        // The vanilla bow needs twenty ticks for full power, the bone bow ten.
        assertTrue(EquipmentRules.boneBowPower(10) > 1.0F - 1.0E-6F);
    }

    /** The original's own draw power: {@code f = (float)draw / 10.0f; f = (f * f + f * 2.0f) / 3.0f}. */
    private static float boneBowPower(int draw) {
        float f = draw / 10.0F;
        return (f * f + f * 2.0F) / 3.0F;
    }

    @Test void boneBowOnlyFiresFromTheOriginalMinimumPower() {
        assertFalse(EquipmentRules.boneBowFires(0));
        assertFalse(EquipmentRules.boneBowFires(1));
        assertTrue(EquipmentRules.boneBowFires(2));
        assertTrue(EquipmentRules.boneBowFires(18));
    }

    @Test void boneBowLetsGoAfterEighteenTicksOfDrawing() {
        assertFalse(EquipmentRules.boneBowAutoRelease(18));
        assertTrue(EquipmentRules.boneBowAutoRelease(19));
        assertTrue(EquipmentRules.boneBowAutoRelease(20));
    }

    @Test void boneBowArrowCarriesTheOriginalDamageAndSpeed() {
        assertEquals(2.5D, EquipmentRules.boneBowArrowDamage(0));
        // Original: the arrow damage is two and a half plus the Power bonus of half a point per level and another half point.
        assertEquals(3.5D, EquipmentRules.boneBowArrowDamage(1));
        assertEquals(4.0D, EquipmentRules.boneBowArrowDamage(2));
        assertEquals(2.5F * 1.0F, EquipmentRules.boneBowArrowSpeed(1.0F));
        assertEquals(2.5F * 0.5F, EquipmentRules.boneBowArrowSpeed(0.5F));
        // The native Power effect adds half a point per level, so the base carries the original's extra half point.
        assertEquals(2.5D, EquipmentRules.boneBowArrowBaseDamage(0));
        assertEquals(3.0D, EquipmentRules.boneBowArrowBaseDamage(1));
        // The base the arrow carries plus the native Power effect is the original damage.
        assertEquals(EquipmentRules.boneBowArrowDamage(1), EquipmentRules.boneBowArrowBaseDamage(1) + 0.5D * 1);
        assertEquals(EquipmentRules.boneBowArrowDamage(2), EquipmentRules.boneBowArrowBaseDamage(2) + 0.5D * 2);
        assertEquals(EquipmentRules.boneBowArrowDamage(0), EquipmentRules.boneBowArrowBaseDamage(0));
    }

    @Test void crimsonBladeStrikesForSevenAndAHalfPointsAndSaps() {
        assertEquals(7.5D, EquipmentRules.crimsonSwordDamage());
        assertEquals(200, EquipmentRules.CRIMSON_SWORD_USES);
        assertEquals(20, EquipmentRules.CRIMSON_SWORD_ENCHANTABILITY);
        assertEquals(2, EquipmentRules.CRIMSON_SWORD_WARP);
        assertEquals(60, EquipmentRules.CRIMSON_SWORD_SAP_WEAKNESS_TICKS);
        assertEquals(120, EquipmentRules.CRIMSON_SWORD_SAP_HUNGER_TICKS);
        // The sap is skipped on the client and when a player hits a player with player versus player off.
        assertFalse(EquipmentRules.crimsonSwordSaps(true, false, false, true));
        assertFalse(EquipmentRules.crimsonSwordSaps(false, true, true, false));
        assertTrue(EquipmentRules.crimsonSwordSaps(false, true, true, true));
        assertTrue(EquipmentRules.crimsonSwordSaps(false, true, false, false));
        assertTrue(EquipmentRules.crimsonSwordSaps(false, false, true, false));
    }

    @Test void primalCrusherHarvestsThePlaneAcrossTheFaceThatWasLookedAt() {
        int[] down = EquipmentRules.primalCrusherOffsets(0);
        assertEquals(27, down.length);
        // Side zero and one (down and up) harvest the x and z plane.
        assertEquals(-1, down[0]);
        assertEquals(0, down[1]);
        assertEquals(-1, down[2]);
        // Side two and three (north and south) harvest the x and y plane.
        int[] north = EquipmentRules.primalCrusherOffsets(2);
        assertEquals(-1, north[0]);
        assertEquals(-1, north[1]);
        assertEquals(0, north[2]);
        // Side four and five (west and east) harvest the z and y plane.
        int[] west = EquipmentRules.primalCrusherOffsets(4);
        assertEquals(0, west[0]);
        assertEquals(-1, west[1]);
        assertEquals(-1, west[2]);
        // The original loop harvests the nine blocks of the plane, the centre included.
        int centre = 0;
        for (int i = 0; i < 9; i++) {
            if (down[i * 3] == 0 && down[i * 3 + 1] == 0 && down[i * 3 + 2] == 0) centre++;
        }
        assertEquals(1, centre);
        assertTrue(EquipmentRules.primalCrusherHarvests(true, false));
        assertTrue(EquipmentRules.primalCrusherHarvests(false, true));
        assertFalse(EquipmentRules.primalCrusherHarvests(false, false));
        assertEquals(500, EquipmentRules.PRIMAL_CRUSHER_USES);
        assertEquals(20, EquipmentRules.PRIMAL_CRUSHER_ENCHANTABILITY);
        assertEquals(3.5F, EquipmentRules.PRIMAL_CRUSHER_TOOL_DAMAGE);
        assertEquals(4.0F, EquipmentRules.PRIMAL_CRUSHER_MATERIAL_DAMAGE);
        assertEquals(2, EquipmentRules.PRIMAL_CRUSHER_WARP);
    }

    @Test void primalArrowKeepsTheOriginalDamageOfEveryElement() {
        assertEquals(2.1D, EquipmentRules.primalArrowDamage(EquipmentRules.ARROW_AIR));
        assertEquals(2.1D, EquipmentRules.primalArrowDamage(EquipmentRules.ARROW_FIRE));
        assertEquals(2.1D, EquipmentRules.primalArrowDamage(EquipmentRules.ARROW_WATER));
        assertEquals(2.1D * 1.5D, EquipmentRules.primalArrowDamage(EquipmentRules.ARROW_EARTH));
        assertEquals(2.1D * 0.8D, EquipmentRules.primalArrowDamage(EquipmentRules.ARROW_ORDER));
        assertEquals(2.1D * 0.8D, EquipmentRules.primalArrowDamage(EquipmentRules.ARROW_ENTROPY));
        assertEquals("aer", EquipmentRules.primalArrowAspect(EquipmentRules.ARROW_AIR));
        assertEquals("perditio", EquipmentRules.primalArrowAspect(EquipmentRules.ARROW_ENTROPY));
        assertNull(EquipmentRules.primalArrowAspect(6));
        // A burning water arrow never burns, the fire arrow adds five seconds.
        assertEquals(0, EquipmentRules.primalArrowFireTicks(EquipmentRules.ARROW_WATER, true));
        assertEquals(5, EquipmentRules.primalArrowFireTicks(EquipmentRules.ARROW_AIR, true));
        assertEquals(0, EquipmentRules.primalArrowFireTicks(EquipmentRules.ARROW_AIR, false));
        assertEquals(5, EquipmentRules.primalArrowExtraFire(EquipmentRules.ARROW_FIRE));
        assertEquals(0, EquipmentRules.primalArrowExtraFire(EquipmentRules.ARROW_AIR));
    }

    @Test void sanityCheckerReadsTheThreeKindsOfWarpTogether() {
        assertEquals(0, EquipmentRules.sanity(0, 0, 0));
        assertEquals(7, EquipmentRules.sanity(3, 2, 2));
        assertEquals(12, EquipmentRules.sanity(10, 1, 1));
    }

    @Test void taintedResourceInfectsItsCarrierOnceInFourThousandThreeHundredAndTwentyOneTicks() {
        assertEquals(4321, EquipmentRules.TAINT_ITEM_CHANCE);
        assertEquals(120, EquipmentRules.TAINT_ITEM_POISON_TICKS);
        // The original rolls nextInt(4321) and compares it with the max damage of the stack, which is zero.
        assertFalse(EquipmentRules.taintItemInfects(0, 0));
        assertTrue(EquipmentRules.taintItemInfects(1, 0));
        assertTrue(EquipmentRules.taintItemInfects(4320, 0));
    }

    @Test void nuggetAndResourceAndShardIdentitiesAreTheOriginalOnes() {
        assertTrue(EquipmentRules.nuggetIsCluster(EquipmentRules.NUGGET_CLUSTER_IRON));
        assertTrue(EquipmentRules.nuggetIsCluster(EquipmentRules.NUGGET_CLUSTER_CINNABAR));
        assertTrue(EquipmentRules.nuggetIsCluster(EquipmentRules.NUGGET_CLUSTER_GOLD));
        assertFalse(EquipmentRules.nuggetIsCluster(EquipmentRules.NUGGET_THAUMIUM));
        assertFalse(EquipmentRules.nuggetIsCluster(EquipmentRules.NUGGET_QUICKSILVER));
        assertTrue(EquipmentRules.resourceIsTainted(EquipmentRules.RESOURCE_TAINTED_GOO));
        assertTrue(EquipmentRules.resourceIsTainted(EquipmentRules.RESOURCE_TAINT_TENDRIL));
        assertFalse(EquipmentRules.resourceIsTainted(EquipmentRules.RESOURCE_QUICKSILVER));
        assertEquals("aer", EquipmentRules.shardAspect(EquipmentRules.SHARD_AIR));
        assertEquals("perditio", EquipmentRules.shardAspect(EquipmentRules.SHARD_ENTROPY));
        assertNull(EquipmentRules.shardAspect(EquipmentRules.SHARD_BALANCED));
        assertEquals(10, EquipmentRules.NUGGET_EDIBLE_USE_DURATION);
        assertEquals(1, EquipmentRules.NUGGET_EDIBLE_HUNGER);
        assertEquals(0.3F, EquipmentRules.NUGGET_EDIBLE_SATURATION);
    }

    @Test void keyRemembersTheDoorItWasLinkedTo() {
        assertEquals("12,64,-7", EquipmentRules.keyLocation(12, 64, -7));
        // Original ItemKey.onItemUseFirst: the access entry is the metadata of the key followed by the name of the player.
        assertEquals("0Steve", EquipmentRules.keyAccessEntry(EquipmentRules.KEY_IRON, "Steve"));
        assertEquals("1Steve", EquipmentRules.keyAccessEntry(EquipmentRules.KEY_GOLD, "Steve"));
        assertTrue(EquipmentRules.keyMayLink(true, false, EquipmentRules.KEY_GOLD));
        assertTrue(EquipmentRules.keyMayLink(false, true, EquipmentRules.KEY_IRON));
        assertFalse(EquipmentRules.keyMayLink(false, true, EquipmentRules.KEY_GOLD));
        assertFalse(EquipmentRules.keyMayLink(false, false, EquipmentRules.KEY_IRON));
    }

    @Test void tripleMeatTreatKeepsTheOriginalFoodValues() {
        assertEquals(6, EquipmentRules.TREAT_HUNGER);
        assertEquals(0.8F, EquipmentRules.TREAT_SATURATION);
        assertEquals(0.66F, EquipmentRules.TREAT_EFFECT_CHANCE);
        assertEquals(100, EquipmentRules.TREAT_REGENERATION_TICKS);
    }

    @Test void compassStoneForgetsNodesAfterTenSecondsAndSeesOnlyTheOnesInSight() {
        assertEquals(10000L, EquipmentRules.COMPASS_STONE_EXPIRY_MILLIS);
        assertTrue(EquipmentRules.compassStoneExpired(1000L, 12000L));
        assertFalse(EquipmentRules.compassStoneExpired(3000L, 12000L));
        assertTrue(EquipmentRules.compassStoneVisible(10.0D, 256.0D, 1.0D));
        assertFalse(EquipmentRules.compassStoneVisible(300.0D, 256.0D, 1.0D));
        assertFalse(EquipmentRules.compassStoneVisible(0.0D, 256.0D, 1.0D));
        assertFalse(EquipmentRules.compassStoneVisible(10.0D, 256.0D, 0.0D));
    }

    @Test void eldritchObjectRaritiesAndNodeStabilizerFollowTheOriginal() {
        assertEquals(0, EquipmentRules.ELDRITCH_EYE);
        assertEquals(1, EquipmentRules.ELDRITCH_CRIMSON_RITES);
        assertEquals(2, EquipmentRules.ELDRITCH_TABLET);
        assertEquals(3, EquipmentRules.ELDRITCH_NODE_STABILIZER);
        assertEquals(4, EquipmentRules.ELDRITCH_OBELISK_PLACER);
        // Original ItemEldritchObject.onItemUseFirst: the base vis of a primal aspect is the old value less two plus a roll.
        assertEquals(5, EquipmentRules.nodeStabilizerPrimalVis(3, 4, false));
        assertEquals(9, EquipmentRules.nodeStabilizerPrimalVis(3, 8, true));
        assertEquals(1, EquipmentRules.nodeStabilizerPrimalVis(3, 0, true));
        // A compound aspect only grows when the roll is above the current value.
        assertEquals(2, EquipmentRules.nodeStabilizerCompoundVis(2, 2, false));
        assertEquals(5, EquipmentRules.nodeStabilizerCompoundVis(5, 2, false));
        assertEquals(2, EquipmentRules.nodeStabilizerCompoundVis(1, 2, false));
        assertFalse(EquipmentRules.nodeStabilizerAddsVis(5, 2, false));
        assertTrue(EquipmentRules.nodeStabilizerAddsVis(1, 2, false));
        // Fading turns pale, pale turns plain and a plain node may turn bright.
        assertEquals("PALE", EquipmentRules.nodeStabilizerModifier("FADING", true, 0));
        assertEquals("FADING", EquipmentRules.nodeStabilizerModifier("FADING", false, 0));
        assertNull(EquipmentRules.nodeStabilizerModifier("PALE", true, 0));
        assertEquals("BRIGHT", EquipmentRules.nodeStabilizerModifier(null, false, 0));
        assertNull(EquipmentRules.nodeStabilizerModifier(null, false, 1));
    }

    @Test void bucketOfDeathPoursIntoEmptyOrSoftGroundOnly() {
        assertTrue(EquipmentRules.bucketDeathCanPour(true, false, false));
        assertTrue(EquipmentRules.bucketDeathCanPour(false, false, false));
        assertFalse(EquipmentRules.bucketDeathCanPour(false, true, false));
        assertEquals(1, EquipmentRules.BUCKET_DEATH_MAX_STACK_SIZE);
        assertEquals("death", EquipmentRules.BUCKET_DEATH_FLUID);
    }

    @Test void bottleOfTaintKeepsTheOriginalSpreadAndPoison() {
        assertEquals(8, EquipmentRules.BOTTLE_TAINT_MAX_STACK_SIZE);
        assertEquals(5.0D, EquipmentRules.BOTTLE_TAINT_SPREAD);
        assertEquals(10, EquipmentRules.BOTTLE_TAINT_PATCHES);
        assertEquals(100, EquipmentRules.BOTTLE_TAINT_POISON_TICKS);
        assertEquals(0.05F, EquipmentRules.BOTTLE_TAINT_GRAVITY);
        assertEquals(0.5F, EquipmentRules.BOTTLE_TAINT_SPEED);
        assertEquals(-20.0F, EquipmentRules.BOTTLE_TAINT_PITCH);
    }

    @Test void handMirrorOnlyLinksToTheMirrorBlock() {
        assertTrue(EquipmentRules.isMirrorBlock("thaumcraft:mirror"));
        assertFalse(EquipmentRules.isMirrorBlock("thaumcraft:arcane_door"));
        assertFalse(EquipmentRules.isMirrorBlock("minecraft:stone"));
        assertEquals(0.3D, EquipmentRules.MIRROR_PUSH_OFFSET);
        assertEquals(20, EquipmentRules.MIRROR_ITEM_LIFETIME);
    }

    @Test void alumentumDispenserKeepsTheOriginalLaunch() {
        assertEquals(1.1F, EquipmentRules.ALUMENTUM_DISPENSE_POWER);
        assertEquals(6.0F, EquipmentRules.ALUMENTUM_DISPENSE_UNCERTAINTY);
        assertEquals(0.1D, EquipmentRules.ALUMENTUM_DISPENSE_LIFT);
        assertEquals(1009, EquipmentRules.ALUMENTUM_DISPENSE_EVENT);
    }
}
