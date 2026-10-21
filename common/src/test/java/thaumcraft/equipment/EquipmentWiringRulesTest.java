package thaumcraft.equipment;

import org.junit.jupiter.api.Test;
import thaumcraft.common.lib.items.EquipmentRules;
import thaumcraft.common.lib.world.EldritchBlockRules;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The rules of the wiring between the equipment items, the projectiles, the blocks and the tiles of the original.
 *
 * <p>Every value is quoted from the TC4 4.2.3.5 oracle: the bone bow and the primal arrow of
 * {@code thaumcraft/common/lib/events/EventHandlerEntity.java} and {@code thaumcraft/common/entities/projectile/EntityPrimalArrow.java},
 * the bottle of taint of {@code thaumcraft/common/items/ItemBottleTaint.java}, the bucket of death of
 * {@code thaumcraft/common/items/ItemBucketDeath.java}, the key of {@code thaumcraft/common/items/ItemKey.java} and the obelisk placer of
 * {@code thaumcraft/common/items/ItemEldritchObject.java}.
 */
class EquipmentWiringRulesTest {
    @Test void theBoneBowFiresThePrimalArrowWithTheOriginalSpeed() {
        // Original EventHandlerEntity.bowShot: the primal arrow is created with the charge times the arrow damage of the bow
        // and the original EntityPrimalArrow:135 leaves with one and a half times that value, in float.
        assertEquals(2.5F * 1.5F, EquipmentRules.boneBowPrimalArrowSpeed(EquipmentRules.boneBowArrowSpeed(1.0F)));
        assertEquals(1.25F * 1.5F, EquipmentRules.boneBowPrimalArrowSpeed(EquipmentRules.boneBowArrowSpeed(0.5F)));
        // The vanilla bow already leaves with three times its charge, which is the original's two times one and a half.
        assertEquals(2.0F * 1.5F, EquipmentRules.boneBowPrimalArrowSpeed(2.0F));
    }

    @Test void theShotUsesTheFirstPrimalArrowOfTheInventory() {
        // Original EventHandlerEntity.bowShot: the element is the metadata of the first primal arrow of the player's inventory.
        assertEquals(EquipmentRules.ARROW_FIRE, EquipmentRules.firstPrimalArrowType(new int[]{-1, -1, EquipmentRules.ARROW_FIRE, -1}));
        assertEquals(EquipmentRules.ARROW_AIR, EquipmentRules.firstPrimalArrowType(new int[]{-1, -1, -1}));
        // Without a primal arrow the air arrow is the plain default of the original.
        assertEquals(EquipmentRules.ARROW_AIR, EquipmentRules.firstPrimalArrowType(new int[0]));
        // The order of the inventory decides, the hotbar comes first.
        assertEquals(EquipmentRules.ARROW_ENTROPY,
                EquipmentRules.firstPrimalArrowType(new int[]{EquipmentRules.ARROW_ENTROPY, EquipmentRules.ARROW_ORDER}));
    }

    @Test void theKeyWritesTheAccessEntryOfItsKind() {
        // Original ItemKey.onItemUseFirst: the entry is the metadata of the key followed by the name of the player.
        assertEquals("0Steve", EquipmentRules.keyAccessEntry(EquipmentRules.KEY_IRON, "Steve"));
        assertEquals("1Steve", EquipmentRules.keyAccessEntry(EquipmentRules.KEY_GOLD, "Steve"));
        // The gold key grants the access of the owner, the doors know both entries.
        assertTrue(EquipmentRules.keyHasGrantAccess(List.of("1Steve"), "Steve"));
        assertFalse(EquipmentRules.keyHasGrantAccess(List.of("0Steve"), "Steve"));
        assertTrue(EquipmentRules.keyHasAccess(List.of("0Steve"), EquipmentRules.KEY_IRON, "Steve"));
        assertFalse(EquipmentRules.keyHasAccess(List.of("0Steve"), EquipmentRules.KEY_GOLD, "Steve"));
        assertFalse(EquipmentRules.keyHasAccess(List.of("1Steve"), EquipmentRules.KEY_IRON, "Steve"));
    }

    @Test void onlyTheOwnerOrTheGrantedPlayerMayLinkAKey() {
        // Original ItemKey.onItemUseFirst: the owner may always link, a player with the access of the gold key only with the iron key.
        assertTrue(EquipmentRules.keyMayLink(true, false, EquipmentRules.KEY_IRON));
        assertTrue(EquipmentRules.keyMayLink(true, false, EquipmentRules.KEY_GOLD));
        assertTrue(EquipmentRules.keyMayLink(false, true, EquipmentRules.KEY_IRON));
        assertFalse(EquipmentRules.keyMayLink(false, true, EquipmentRules.KEY_GOLD));
        assertFalse(EquipmentRules.keyMayLink(false, false, EquipmentRules.KEY_IRON));
        assertFalse(EquipmentRules.keyMayLink(false, false, EquipmentRules.KEY_GOLD));
    }

    @Test void theBucketOfDeathPoursIntoEverySpotThatIsNotSolid() {
        // Original ItemBucketDeath.tryPlaceContainedLiquid: the spot has to be empty or not solid.
        assertTrue(EquipmentRules.bucketDeathCanPour(true, false, false));
        assertTrue(EquipmentRules.bucketDeathCanPour(false, false, false));
        assertFalse(EquipmentRules.bucketDeathCanPour(false, true, false));
        // A spot that is neither solid nor a liquid is emptied first.
        assertTrue(EquipmentRules.bucketDeathClearsSpot(false, false, false));
        assertFalse(EquipmentRules.bucketDeathClearsSpot(true, false, false));
        assertFalse(EquipmentRules.bucketDeathClearsSpot(false, true, false));
        assertFalse(EquipmentRules.bucketDeathClearsSpot(false, false, true));
        // The bucket of death is the death fluid of the original.
        assertEquals("death", EquipmentRules.BUCKET_DEATH_FLUID);
        assertEquals("thaumcraft:death_fluid", EquipmentRules.BUCKET_DEATH_BLOCK_PATH);
        assertEquals(1, EquipmentRules.BUCKET_DEATH_MAX_STACK_SIZE);
    }

    @Test void theObeliskPlacerPutsUpTheAltarTheObeliskAndItsMiddle() {
        // Original ItemEldritchObject.onItemUseFirst: the altar at one, the obelisk at three and the middle at four to seven.
        assertEquals(EldritchBlockRules.ELDRITCH_ALTAR, EquipmentRules.eldritchPlacerVariant(1));
        assertEquals(EldritchBlockRules.ELDRITCH_OBELISK, EquipmentRules.eldritchPlacerVariant(3));
        assertEquals(EldritchBlockRules.ELDRITCH_OBELISK_MIDDLE, EquipmentRules.eldritchPlacerVariant(4));
        assertEquals(EldritchBlockRules.ELDRITCH_OBELISK_MIDDLE, EquipmentRules.eldritchPlacerVariant(7));
        // The six blocks above the ground have to be free.
        assertEquals(6, EquipmentRules.ELDRITCH_PLACER_FREE_HEIGHT);
    }

    @Test void theBottleOfTaintKeepsTheOriginalNumbers() {
        // Original ItemBottleTaint and EntityBottleTaint.
        assertEquals(8, EquipmentRules.BOTTLE_TAINT_MAX_STACK_SIZE);
        assertEquals(0.05F, EquipmentRules.BOTTLE_TAINT_GRAVITY);
        assertEquals(0.5F, EquipmentRules.BOTTLE_TAINT_SPEED);
        assertEquals(-20.0F, EquipmentRules.BOTTLE_TAINT_PITCH);
        assertEquals(5.0D, EquipmentRules.BOTTLE_TAINT_SPREAD);
        assertEquals(10, EquipmentRules.BOTTLE_TAINT_PATCHES);
        assertEquals(100, EquipmentRules.BOTTLE_TAINT_POISON_TICKS);
    }
}
