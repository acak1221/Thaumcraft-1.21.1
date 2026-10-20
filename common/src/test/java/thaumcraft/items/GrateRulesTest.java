package thaumcraft.items;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;
import thaumcraft.common.lib.GrateRules;

/** The decisions of the Item Grate (original BlockMetalDevice metadata 5 / 6 and TileGrate) without a world. */
class GrateRulesTest {
    @Test void thePlateSitsInTheTopThreeSixteenthsAndAFedItemReappearsBelowIt() {
        assertEquals(0.8125D, GrateRules.PLATE_BOTTOM, 0.0D);
        assertEquals(13.0D, GrateRules.PLATE_BOTTOM * 16.0D, 0.0D, "13/16 of the block");
        assertTrue(GrateRules.SPAWN_HEIGHT < GrateRules.PLATE_BOTTOM, "the new item starts under the plate");
        assertEquals(0.6D, GrateRules.SPAWN_HEIGHT, 0.0D);
        assertEquals(-0.1D, GrateRules.FALL_SPEED, 0.0D);
    }

    @Test void aPoweredGrateClosesAndAnUnpoweredOneOpens() {
        assertTrue(GrateRules.closedAfterNeighbourChange(false, true, false), "open + powered closes");
        assertTrue(GrateRules.closedAfterNeighbourChange(true, true, false), "closed + powered stays closed");
        assertFalse(GrateRules.closedAfterNeighbourChange(true, false, true), "closed + unpowered after a power source changed opens");
        assertFalse(GrateRules.closedAfterNeighbourChange(false, false, true), "open + unpowered stays open");
    }

    @Test void aChangeOfABlockThatCannotGivePowerLeavesAnUnpoweredGrateAlone() {
        // BlockMetalDevice:490 - without power and without a power-giving neighbour the handler does not run, so a hand-closed grate stays shut.
        assertTrue(GrateRules.closedAfterNeighbourChange(true, false, false), "a closed grate opened by hand is not reopened by stone changing next to it");
        assertFalse(GrateRules.closedAfterNeighbourChange(false, false, false));
    }

    @Test void aPoweredGrateClosesEvenWhenTheChangedBlockGivesNoPower() {
        assertTrue(GrateRules.closedAfterNeighbourChange(false, true, true));
        assertTrue(GrateRules.closedAfterNeighbourChange(false, true, false));
    }

    @Test void aClosedGrateStopsEverythingAndAnOpenOneStopsEverythingButDroppedItems() {
        assertTrue(GrateRules.collides(true, true, false), "closed vs creature");
        assertTrue(GrateRules.collides(true, true, true), "closed vs dropped item");
        assertTrue(GrateRules.collides(true, false, false), "closed with no entity at all");
        assertTrue(GrateRules.collides(false, true, false), "open vs creature");
        assertFalse(GrateRules.collides(false, true, true), "open vs dropped item: it falls through");
    }

    @Test void anOpenGrateAskedWithoutAnEntityHasNoCollisionBoxQuirkKept() {
        assertFalse(GrateRules.collides(false, false, false), "BlockMetalDevice.addCollisionBoxesToList adds nothing for entity == null");
    }

    @Test void onlyAnOpenGrateTakesItemsAndOnlyFromAbove() {
        for (Direction side : Direction.values()) {
            assertEquals(side == Direction.UP, GrateRules.acceptsFrom(false, side), "open from " + side);
            assertFalse(GrateRules.acceptsFrom(true, side), "closed from " + side);
        }
    }

    @Test void theClosedGrateDropsTheSameItemAsTheOpenOne() {
        assertTrue(GrateRules.dropsTheSameItemWhenClosed());
    }
}
