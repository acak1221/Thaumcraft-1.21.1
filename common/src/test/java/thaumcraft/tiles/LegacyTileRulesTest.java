package thaumcraft.tiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import thaumcraft.common.lib.tiles.LegacyTileRules;

/**
 * The numbers of the remaining TC4 blocks and tiles, checked against the TC4 4.2.3.5 oracle without a game
 * runtime. Every assertion names the original behaviour it protects.
 */
class LegacyTileRulesTest {
    // ----------------------------------------------------------------------------------------------
    // Arcane furnace
    // ----------------------------------------------------------------------------------------------
    @Test void theFurnaceHoldsThirtyTwoSlotsOfSixtyFour() {
        assertEquals(32, LegacyTileRules.FURNACE_SLOTS);
        assertEquals(64, LegacyTileRules.FURNACE_STACK_LIMIT);
    }

    @Test void anUnspeedyFurnaceCooksInOneHundredAndFortyTicksAndEveryBellowsTakesTwentyOff() {
        assertEquals(140, LegacyTileRules.cookTime(false, 0));
        assertEquals(120, LegacyTileRules.cookTime(false, 1));
        assertEquals(100, LegacyTileRules.cookTime(false, 2));
        assertEquals(80, LegacyTileRules.cookTime(false, 3));
    }

    @Test void aSpeedyFurnaceCooksInEightyTicks() {
        assertEquals(80, LegacyTileRules.cookTime(true, 0));
        assertEquals(60, LegacyTileRules.cookTime(true, 1));
        assertEquals(40, LegacyTileRules.cookTime(true, 2));
        assertEquals(20, LegacyTileRules.cookTime(true, 3));
    }

    @Test void noMoreThanThreeBellowsAreCounted() {
        assertEquals(3, LegacyTileRules.MAX_BELLOWS);
        assertEquals(3, LegacyTileRules.clampBellows(3));
        assertEquals(3, LegacyTileRules.clampBellows(9));
        assertEquals(0, LegacyTileRules.clampBellows(-1));
        assertEquals(20, LegacyTileRules.cookTime(true, 9), "a fourth bellows cannot shorten the cook time");
    }

    @Test void theFurnaceEjectsTowardsItsOutputNozzle() {
        assertEquals(LegacyTileRules.Facing.WEST, LegacyTileRules.furnaceFacing(true, true, true), "west wins");
        assertEquals(LegacyTileRules.Facing.EAST, LegacyTileRules.furnaceFacing(false, true, true), "then east");
        assertEquals(LegacyTileRules.Facing.NORTH, LegacyTileRules.furnaceFacing(false, false, true));
        assertEquals(LegacyTileRules.Facing.SOUTH, LegacyTileRules.furnaceFacing(false, false, false),
                "without a nozzle the items leave to the south");
    }

    @Test void theShellLevelIsZeroNineteenOrEighteen() {
        // A block with the same metadata above and below it is fully enclosed: level nine.
        assertEquals(9, LegacyTileRules.furnaceLevel(2, 2, 2, true, true));
        // The same metadata above but nothing below: the bottom of the furnace, level eighteen.
        assertEquals(18, LegacyTileRules.furnaceLevel(2, 2, 2, true, false));
        // Nothing above: the top of the furnace, level zero.
        assertEquals(0, LegacyTileRules.furnaceLevel(2, 0, 2, false, true));
        // The core itself: the hole above it is air, so it stays level zero.
        assertEquals(0, LegacyTileRules.furnaceLevel(0, 0, 5, false, true));
        // A nozzle above or below counts as "the same as this block".
        assertEquals(18, LegacyTileRules.furnaceLevel(2, 10, 2, true, false));
    }

    @Test void aBrokenShellBlockIsRestoredToTheOriginalBlocks() {
        assertEquals(LegacyTileRules.Restored.AIR, LegacyTileRules.restoredBlock(0), "the core leaves nothing");
        assertEquals(LegacyTileRules.Restored.IRON_BARS, LegacyTileRules.restoredBlock(10), "the nozzle leaves bars");
        assertEquals(LegacyTileRules.Restored.OBSIDIAN, LegacyTileRules.restoredBlock(2));
        assertEquals(LegacyTileRules.Restored.OBSIDIAN, LegacyTileRules.restoredBlock(4));
        assertEquals(LegacyTileRules.Restored.OBSIDIAN, LegacyTileRules.restoredBlock(6));
        assertEquals(LegacyTileRules.Restored.OBSIDIAN, LegacyTileRules.restoredBlock(8));
        assertEquals(LegacyTileRules.Restored.OBSIDIAN, LegacyTileRules.restoredBlock(5), "the bottom centre is obsidian");
        assertEquals(LegacyTileRules.Restored.NETHER_BRICK, LegacyTileRules.restoredBlock(1));
        assertEquals(LegacyTileRules.Restored.NETHER_BRICK, LegacyTileRules.restoredBlock(3));
        assertEquals(LegacyTileRules.Restored.NETHER_BRICK, LegacyTileRules.restoredBlock(7));
        assertEquals(LegacyTileRules.Restored.NETHER_BRICK, LegacyTileRules.restoredBlock(9));
    }

    @Test void theShellIsIntactWhileAllTwentyFiveCellsAroundTheCoreBelongToIt() {
        assertTrue(LegacyTileRules.structureIntact(cell -> true));
        assertTrue(LegacyTileRules.structureIntact(cell -> cell != LegacyTileRules.offset(0, 1, 0)),
                "the hole above the core is allowed to be empty");
        assertTrue(LegacyTileRules.structureIntact(cell -> cell != LegacyTileRules.offset(0, 0, 0)),
                "the core cell itself is allowed to be missing");
        assertFalse(LegacyTileRules.structureIntact(cell -> cell != LegacyTileRules.offset(1, 0, 1)),
                "a missing corner breaks the furnace");
    }

    @Test void theFurnaceDrainsFiveFireVisAndTheNozzleGrantsSixHundredSpeedyTicks() {
        assertEquals(5, LegacyTileRules.FURNACE_VIS_DRAIN);
        assertEquals(600, LegacyTileRules.NOZZLE_SPEEDY_GRANT);
    }

    @Test void theNozzleOnlyOffersSuctionWhileTheFurnaceIsHungry() {
        assertEquals(128, LegacyTileRules.nozzleSuction(0));
        assertEquals(128, LegacyTileRules.nozzleSuction(39));
        assertEquals(0, LegacyTileRules.nozzleSuction(40), "at forty speedy ticks the nozzle stops");
        assertEquals(0, LegacyTileRules.nozzleSuction(600));
    }

    @Test void theNozzleDrawsOnEveryFifthTickOnly() {
        assertFalse(LegacyTileRules.nozzleDrawTick(0));
        assertFalse(LegacyTileRules.nozzleDrawTick(1));
        assertFalse(LegacyTileRules.nozzleDrawTick(2));
        assertFalse(LegacyTileRules.nozzleDrawTick(3));
        assertTrue(LegacyTileRules.nozzleDrawTick(4));
        assertTrue(LegacyTileRules.nozzleDrawTick(9));
    }

    @Test void theNozzleFacesAwayFromItsCore() {
        // Direction indices are the original ForgeDirection order: down, up, north, south, west, east.
        assertEquals(1, LegacyTileRules.nozzleFacing(0), "down becomes up");
        assertEquals(4, LegacyTileRules.nozzleFacing(5), "east becomes west");
        assertEquals(3, LegacyTileRules.nozzleFacing(2), "north becomes south");
    }

    @Test void theNozzleOnlyDrawsFromAWeakerNeighbour() {
        assertTrue(LegacyTileRules.nozzleOutdraws(64, 128));
        assertFalse(LegacyTileRules.nozzleOutdraws(128, 128));
        assertFalse(LegacyTileRules.nozzleOutdraws(256, 128));
    }

    @Test void theFurnaceLightIsThirteen() {
        assertEquals(13, LegacyTileRules.FURNACE_LIGHT);
    }

    @Test void theEjectionUsesTheOriginalOffsets() {
        assertEquals(1.2F, LegacyTileRules.EJECT_OFFSET);
        assertEquals(0.13F, LegacyTileRules.EJECT_PUSH);
        assertEquals(0.03F, LegacyTileRules.EJECT_SCATTER);
        assertEquals(0.025F, LegacyTileRules.EJECT_XP_SCATTER);
    }

    @Test void theSmeltingBonusIsOneInFourWithoutBellowsAndFortyFourPercentPerBellows() {
        assertEquals(1, LegacyTileRules.bonusRolls(0));
        assertEquals(3, LegacyTileRules.bonusRolls(3));
        assertEquals(0.25F, LegacyTileRules.bonusChance(0));
        assertEquals(0.44F, LegacyTileRules.bonusChance(1));
        assertEquals(0.44F, LegacyTileRules.bonusChance(3));
    }

    // ----------------------------------------------------------------------------------------------
    // Arcane lamp light and death fluid
    // ----------------------------------------------------------------------------------------------
    @Test void theLampLightChecksItsSourceOnEveryHundredthTick() {
        assertEquals(100, LegacyTileRules.LAMP_LIGHT_CHECK_PERIOD);
        assertFalse(LegacyTileRules.lampLightExpires(99, false));
        assertTrue(LegacyTileRules.lampLightExpires(100, false), "without a lamp the light removes itself");
        assertFalse(LegacyTileRules.lampLightExpires(100, true), "with a lamp the light stays");
        assertFalse(LegacyTileRules.lampLightExpires(200, true));
    }

    @Test void theDeathFluidHoldsFourQuantaAndHurtsForQuantaPlusOne() {
        assertEquals(4, LegacyTileRules.DEATH_FLUID_QUANTA);
        assertEquals(1, LegacyTileRules.deathFluidDamage(0));
        assertEquals(4, LegacyTileRules.deathFluidDamage(3));
        assertTrue(LegacyTileRules.deathFluidHurts(false));
        assertFalse(LegacyTileRules.deathFluidHurts(true));
    }

    // ----------------------------------------------------------------------------------------------
    // Essentia tubes
    // ----------------------------------------------------------------------------------------------
    @Test void aPlainTubeOffersOneLessAndARestrictTubeHalves() {
        assertEquals(63, LegacyTileRules.tubeSuction(64));
        assertEquals(32, LegacyTileRules.restrictSuction(64));
        assertEquals(35, LegacyTileRules.restrictSuction(70));
    }

    @Test void aOneWayTubeOnlyPullsFromTheFaceOppositeItsFacing() {
        // Facing north is direction two, so the pull happens on the south face, direction three.
        assertTrue(LegacyTileRules.oneWayPulls(2, 2), "the pressure face never pulls");
        assertFalse(LegacyTileRules.oneWayPulls(2, 3));
        assertTrue(LegacyTileRules.oneWayPulls(2, 4));
        assertTrue(LegacyTileRules.oneWayPulls(2, 5));
    }

    @Test void theValveClosesWhenPoweredAndOpensWhenThePowerIsGone() {
        assertFalse(LegacyTileRules.valveFlowAfterEdge(false, true, true), "power closes the valve");
        assertTrue(LegacyTileRules.valveFlowAfterEdge(true, false, false), "losing power opens the valve");
        assertTrue(LegacyTileRules.valveFlowAfterEdge(true, true, true), "a steady signal changes nothing");
        assertFalse(LegacyTileRules.valveFlowAfterEdge(false, false, false), "an unpowered closed valve stays closed");
    }

    @Test void theValveWheelTurnsTwentyDegreesATick() {
        assertEquals(20.0F, LegacyTileRules.valveRotation(0.0F, false));
        assertEquals(40.0F, LegacyTileRules.valveRotation(20.0F, false));
        assertEquals(360.0F, LegacyTileRules.valveRotation(340.0F, false));
        assertEquals(360.0F, LegacyTileRules.valveRotation(360.0F, false), "it stops at a full turn");
        assertEquals(0.0F, LegacyTileRules.valveRotation(20.0F, true));
        assertEquals(0.0F, LegacyTileRules.valveRotation(0.0F, true));
    }

    // ----------------------------------------------------------------------------------------------
    // Wand pedestal
    // ----------------------------------------------------------------------------------------------
    @Test void theWandPedestalKeepsOneVisUnlessTheWandIsIronCappedOrWooden() {
        assertEquals(0, LegacyTileRules.wandPedestalReserve("iron", "wood"));
        assertEquals(0, LegacyTileRules.wandPedestalReserve("iron", "silverwood"));
        assertEquals(0, LegacyTileRules.wandPedestalReserve("copper", "wood"));
        assertEquals(1, LegacyTileRules.wandPedestalReserve("copper", "silverwood"));
    }

    @Test void theWandPedestalScansSixteenBlocksAndReportsWholeVis() {
        assertEquals(8, LegacyTileRules.WAND_PEDESTAL_SCAN_RADIUS);
        assertEquals(5, LegacyTileRules.WAND_PEDESTAL_DRAW_PERIOD);
        assertEquals(20, LegacyTileRules.WAND_PEDESTAL_REFRESH_PERIOD);
        assertEquals(100, LegacyTileRules.WAND_PEDESTAL_RESCAN_PERIOD);
        assertEquals(3, LegacyTileRules.wandPedestalReadout(350), "the readout is whole vis");
        assertEquals(0, LegacyTileRules.wandPedestalReadout(99));
    }

    // ----------------------------------------------------------------------------------------------
    // Magic workbench
    // ----------------------------------------------------------------------------------------------
    @Test void theWorkbenchHasElevenSlotsAndOnlyTheWandSlotIsAutomated() {
        assertEquals(11, LegacyTileRules.MAGIC_WORKBENCH_SLOTS);
        assertEquals(10, LegacyTileRules.MAGIC_WORKBENCH_WAND_SLOT);
    }

    @Test void theWorkbenchMatrixIsThreeByThree() {
        assertEquals(0, LegacyTileRules.magicWorkbenchSlot(0, 0));
        assertEquals(2, LegacyTileRules.magicWorkbenchSlot(0, 2));
        assertEquals(3, LegacyTileRules.magicWorkbenchSlot(1, 0));
        assertEquals(8, LegacyTileRules.magicWorkbenchSlot(2, 2));
        assertEquals(-1, LegacyTileRules.magicWorkbenchSlot(3, 0));
        assertEquals(-1, LegacyTileRules.magicWorkbenchSlot(0, 3));
        assertEquals(-1, LegacyTileRules.magicWorkbenchSlot(-1, 0));
    }

    @Test void theWorkbenchWandSlotRefusesAStaff() {
        assertTrue(LegacyTileRules.magicWorkbenchAcceptsWand(true, false));
        assertFalse(LegacyTileRules.magicWorkbenchAcceptsWand(true, true), "a staff cannot be used");
        assertFalse(LegacyTileRules.magicWorkbenchAcceptsWand(false, false));
    }

    // ----------------------------------------------------------------------------------------------
    // Ownership
    // ----------------------------------------------------------------------------------------------
    @Test void theOwnerAndTheAccessListKeepAccess() {
        assertTrue(LegacyTileRules.ownedAccess("Steve", List.of("Alex"), "Steve"));
        assertTrue(LegacyTileRules.ownedAccess("Steve", List.of("Alex"), "Alex"));
        assertFalse(LegacyTileRules.ownedAccess("Steve", List.of("Alex"), "Herobrine"));
        assertFalse(LegacyTileRules.ownedAccess("Steve", List.of(), "Alex"));
    }

    @Test void anOwnedBlockIsOnlySafeToRemoveWithAnOwner() {
        assertFalse(LegacyTileRules.ownedSafeToRemove(""));
        assertFalse(LegacyTileRules.ownedSafeToRemove(null));
        assertTrue(LegacyTileRules.ownedSafeToRemove("Steve"));
    }

    // ----------------------------------------------------------------------------------------------
    // Cosmetic blocks
    // ----------------------------------------------------------------------------------------------
    @Test void theCosmeticSolidKeepsItsOriginalLight() {
        assertEquals(9, LegacyTileRules.cosmeticSolidLight(2), "the paving stone lights up");
        assertEquals(4, LegacyTileRules.cosmeticSolidLight(14), "the taint crust glows faintly");
        assertEquals(0, LegacyTileRules.cosmeticSolidLight(0));
    }

    @Test void theCosmeticSolidKeepsItsOriginalResistance() {
        assertEquals(999.0F, LegacyTileRules.cosmeticSolidResistance(0));
        assertEquals(999.0F, LegacyTileRules.cosmeticSolidResistance(8));
        assertEquals(20.0F, LegacyTileRules.cosmeticSolidResistance(4));
        assertEquals(20.0F, LegacyTileRules.cosmeticSolidResistance(7));
        assertEquals(6.0F, LegacyTileRules.cosmeticSolidResistance(9),
                "the original setResistance(10) was stored as thirty and exposed as six");
    }

    @Test void theCosmeticSolidKeepsItsOriginalHardness() {
        assertEquals(30.0F, LegacyTileRules.cosmeticSolidHardness(0));
        assertEquals(30.0F, LegacyTileRules.cosmeticSolidHardness(1));
        assertEquals(30.0F, LegacyTileRules.cosmeticSolidHardness(8));
        assertEquals(4.0F, LegacyTileRules.cosmeticSolidHardness(4));
        assertEquals(4.0F, LegacyTileRules.cosmeticSolidHardness(6));
        assertEquals(4.0F, LegacyTileRules.cosmeticSolidHardness(7));
        assertEquals(2.0F, LegacyTileRules.cosmeticSolidHardness(9), "the golem stone keeps the block hardness");
    }

    @Test void theWardedGlassIsUnbreakableAndSeeThrough() {
        assertTrue(LegacyTileRules.cosmeticOpaqueWarded(2));
        assertFalse(LegacyTileRules.cosmeticOpaqueWarded(0));
        assertEquals(-1.0F, LegacyTileRules.cosmeticOpaqueHardness(2));
        assertEquals(999.0F, LegacyTileRules.cosmeticOpaqueResistance(2));
        assertEquals(3.0F, LegacyTileRules.cosmeticOpaqueResistance(0),
                "the original setResistance(5) was stored as fifteen and exposed as three");
        assertEquals(0, LegacyTileRules.cosmeticOpaqueOpacity(2), "the warded glass does not block light");
        assertEquals(3, LegacyTileRules.cosmeticOpaqueOpacity(0), "the amber block dims the light");
    }
}
