package thaumcraft.machines;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import thaumcraft.common.lib.crafting.DeviceContainerRules;
import thaumcraft.common.lib.crafting.DeviceContainerRules.Range;

class DeviceContainerRulesTest {

    // ------------------------------------------------------------- magic box
    @Test void magicBoxBindsThreeRowsAndUsesTheWholeLinkedSizeForTheQuickMove() {
        assertEquals(3, DeviceContainerRules.MAGIC_BOX_ROWS);
        assertEquals(27, DeviceContainerRules.MAGIC_BOX_SLOTS);
        assertEquals(3, DeviceContainerRules.magicBoxRows(27));
        assertEquals(27, DeviceContainerRules.magicBoxQuickMoveBoundary(27));
        // A linked box owns 27 slots per box, but only three rows are bound.
        assertEquals(6, DeviceContainerRules.magicBoxRows(54));
        assertEquals(54, DeviceContainerRules.magicBoxQuickMoveBoundary(54));
        assertEquals(27, DeviceContainerRules.MAGIC_BOX_SLOTS);
        // The original boundary therefore reaches past the bound box slots.
        assertTrue(DeviceContainerRules.magicBoxQuickMoveBoundary(54) > DeviceContainerRules.MAGIC_BOX_SLOTS);
    }
    @Test void magicBoxKeepsTheOriginalSlotPositions() {
        assertEquals(8, DeviceContainerRules.magicBoxSlotX(0));
        assertEquals(80, DeviceContainerRules.magicBoxSlotX(4));
        assertEquals(18, DeviceContainerRules.magicBoxSlotY(0));
        assertEquals(54, DeviceContainerRules.magicBoxSlotY(2));
        assertEquals(103, DeviceContainerRules.magicBoxPlayerY(0));
        assertEquals(139, DeviceContainerRules.magicBoxPlayerY(2));
        assertEquals(161, DeviceContainerRules.magicBoxHotbarY());
    }
    @Test void magicBoxQuickMoveSendsBoxItemsUpAndPlayerItemsDown() {
        var fromBox = DeviceContainerRules.magicBoxQuickMove(3, 27);
        assertEquals(1, fromBox.size());
        assertEquals(new Range(27, 63), fromBox.get(0));
        var fromPlayer = DeviceContainerRules.magicBoxQuickMove(30, 27);
        assertEquals(new Range(0, 27), fromPlayer.get(0));
        // A linked box shifts the boundary, exactly as the original did.
        var linked = DeviceContainerRules.magicBoxQuickMove(3, 54);
        assertEquals(new Range(54, 63), linked.get(0));
    }

    // ------------------------------------------------------------ hand mirror
    @Test void handMirrorKeepsItsSingleSlotAndItsStackLimit() {
        assertEquals(1, DeviceContainerRules.HAND_MIRROR_INPUT_SLOTS);
        assertEquals(64, DeviceContainerRules.HAND_MIRROR_STACK_LIMIT);
        assertEquals(80, DeviceContainerRules.HAND_MIRROR_INPUT_X);
        assertEquals(24, DeviceContainerRules.HAND_MIRROR_INPUT_Y);
        assertEquals(84, DeviceContainerRules.HAND_MIRROR_PLAYER_Y);
        assertEquals(142, DeviceContainerRules.HAND_MIRROR_HOTBAR_Y);
    }
    @Test void handMirrorAcceptsAnyItemButNeverAMirrorOnAQuickMove() {
        assertTrue(DeviceContainerRules.handMirrorSlotAcceptsAnyItem());
        assertFalse(DeviceContainerRules.handMirrorQuickMoveAllowed(true));
        assertTrue(DeviceContainerRules.handMirrorQuickMoveAllowed(false));
    }
    @Test void handMirrorTransportsOnlyOnTheServerAndOnlyWithALinkedMirror() {
        assertFalse(DeviceContainerRules.handMirrorCanTransport(false, true));
        assertFalse(DeviceContainerRules.handMirrorCanTransport(true, false));
        assertTrue(DeviceContainerRules.handMirrorCanTransport(true, true));
    }
    @Test void handMirrorShowsTheLinkedInventoryForAnEqualStackOnly() {
        assertFalse(DeviceContainerRules.handMirrorShowsLinkedInventory(true, true));
        assertFalse(DeviceContainerRules.handMirrorShowsLinkedInventory(false, false));
        assertTrue(DeviceContainerRules.handMirrorShowsLinkedInventory(false, true));
    }
    @Test void handMirrorQuickMoveUsesTheInputSlotAndThePlayerInventory() {
        assertEquals(new Range(1, 37), DeviceContainerRules.handMirrorQuickMove(0).get(0));
        assertEquals(new Range(0, 1), DeviceContainerRules.handMirrorQuickMove(5).get(0));
    }

    // ------------------------------------------------------------- focus pouch
    @Test void focusPouchHasEighteenSlotsInThreeRowsOfSix() {
        assertEquals(18, DeviceContainerRules.FOCUS_POUCH_SLOTS);
        assertEquals(37, DeviceContainerRules.focusPouchSlotX(0));
        assertEquals(127, DeviceContainerRules.focusPouchSlotX(5));
        assertEquals(51, DeviceContainerRules.focusPouchSlotY(0));
        assertEquals(87, DeviceContainerRules.focusPouchSlotY(17));
        assertEquals(151, DeviceContainerRules.focusPouchPlayerY(0));
        assertEquals(209, DeviceContainerRules.focusPouchHotbarY());
    }
    @Test void focusPouchOnlyAcceptsABasicFocus() {
        assertTrue(DeviceContainerRules.focusPouchAccepts(true));
        assertFalse(DeviceContainerRules.focusPouchAccepts(false));
        assertEquals(new Range(18, 54), DeviceContainerRules.focusPouchQuickMove(0).get(0));
        assertEquals(new Range(0, 18), DeviceContainerRules.focusPouchQuickMove(20).get(0));
    }

    // ---------------------------------------------------------- hover harness
    @Test void hoverHarnessTakesAFilledJarThatStillCarriesEnergy() {
        assertEquals(1, DeviceContainerRules.HOVER_HARNESS_SLOTS);
        assertEquals(80, DeviceContainerRules.hoverHarnessSlotX());
        assertEquals(32, DeviceContainerRules.hoverHarnessSlotY());
        assertFalse(DeviceContainerRules.hoverHarnessAccepts(false, true));
        assertFalse(DeviceContainerRules.hoverHarnessAccepts(true, false));
        assertTrue(DeviceContainerRules.hoverHarnessAccepts(true, true));
        assertEquals(new Range(1, 37), DeviceContainerRules.hoverHarnessQuickMove(0).get(0));
        assertEquals(new Range(0, 1), DeviceContainerRules.hoverHarnessQuickMove(9).get(0));
    }

    // ------------------------------------------------------------ ghost slots
    @Test void ghostSlotsKeepTheOriginalLimits() {
        assertEquals(256, DeviceContainerRules.GHOST_SLOT_LIMIT);
        assertEquals(1, DeviceContainerRules.GHOST_FLUID_SLOT_LIMIT);
        assertEquals(256, DeviceContainerRules.ghostSlotLimit(false));
        assertEquals(1, DeviceContainerRules.ghostSlotLimit(true));
        assertTrue(DeviceContainerRules.ghostSlotAcceptsFluidContainer(true));
        assertFalse(DeviceContainerRules.ghostSlotAcceptsFluidContainer(false));
    }
    @Test void ghostFillPutsTheWholeStackOnALeftClickAndOneItemOnARightClick() {
        assertEquals(64, DeviceContainerRules.ghostFillCount(256, 64, false));
        assertEquals(1, DeviceContainerRules.ghostFillCount(256, 64, true));
        assertEquals(16, DeviceContainerRules.ghostFillCount(16, 64, false));
    }
    @Test void ghostAdjustRemovesOneOnALeftClickAndAddsOneOnARightClick() {
        assertEquals(9, DeviceContainerRules.ghostAdjustCount(10, 256, false));
        assertEquals(11, DeviceContainerRules.ghostAdjustCount(10, 256, true));
        assertEquals(256, DeviceContainerRules.ghostAdjustCount(256, 256, true));
    }
    @Test void ghostMergeIsBoundedByTheSlotLimitAndTheHeldMaximum() {
        // The original mixes the held maximum with the slot count, so a merge
        // grows the preview to the held maximum and can even shrink it.
        assertEquals(64, DeviceContainerRules.ghostMergeCount(10, 64, 64, 256, false));
        assertEquals(11, DeviceContainerRules.ghostMergeCount(10, 64, 64, 256, true));
        assertEquals(64, DeviceContainerRules.ghostMergeCount(250, 64, 64, 256, false));
        assertEquals(1, DeviceContainerRules.ghostMergeCount(10, 64, 1, 256, false));
    }
    @Test void ghostQuickMoveClearsTheGhostOrAddsSixteen() {
        assertEquals(0, DeviceContainerRules.ghostQuickMoveCount(40, 256, false));
        assertEquals(56, DeviceContainerRules.ghostQuickMoveCount(40, 256, true));
        assertEquals(256, DeviceContainerRules.ghostQuickMoveCount(250, 256, true));
    }
    @Test void ghostThrowRemovesOneItemOrTheWholePreview() {
        assertEquals(9, DeviceContainerRules.ghostThrowCount(10, false));
        assertEquals(0, DeviceContainerRules.ghostThrowCount(10, true));
        assertEquals(0, DeviceContainerRules.ghostThrowCount(0, false));
    }
    @Test void ghostClickCoversFillAdjustMergeAndReplacement() {
        // An empty ghost takes the held stack.
        assertEquals(32, DeviceContainerRules.ghostClickCount(0, 32, 64, 256, false, false));
        assertEquals(1, DeviceContainerRules.ghostClickCount(0, 32, 64, 256, false, true));
        // An empty cursor adjusts the preview.
        assertEquals(4, DeviceContainerRules.ghostClickCount(5, 0, 64, 256, false, false));
        assertEquals(6, DeviceContainerRules.ghostClickCount(5, 0, 64, 256, false, true));
        // The same item grows the preview.
        assertEquals(37, DeviceContainerRules.ghostClickCount(5, 32, 64, 256, true, false));
        // A different item replaces the preview with the whole held stack.
        assertEquals(32, DeviceContainerRules.ghostClickCount(5, 32, 64, 256, false, false));
        // Nothing at all leaves the ghost empty.
        assertEquals(0, DeviceContainerRules.ghostClickCount(0, 0, 64, 256, false, false));
    }

    // -------------------------------------------------------- limited by rules
    @Test void limitedByClassAcceptsEveryAssignableItemClass() {
        assertTrue(DeviceContainerRules.limitedByClass(Number.class, Integer.class));
        assertTrue(DeviceContainerRules.limitedByClass(Number.class, Number.class));
        assertFalse(DeviceContainerRules.limitedByClass(Number.class, String.class));
        assertFalse(DeviceContainerRules.limitedByClass(null, String.class));
        assertFalse(DeviceContainerRules.limitedByClass(Number.class, null));
    }
    @Test void limitedByWandRefusesAStaff() {
        assertTrue(DeviceContainerRules.limitedByWand(true, false));
        assertFalse(DeviceContainerRules.limitedByWand(true, true));
        assertFalse(DeviceContainerRules.limitedByWand(false, false));
    }
    @Test void limitedHasAspectsNeedsAtLeastOneAspect() {
        assertTrue(DeviceContainerRules.limitedHasAspects(1));
        assertFalse(DeviceContainerRules.limitedHasAspects(0));
        assertFalse(DeviceContainerRules.limitedHasAspects(-3));
    }
    @Test void researchTableOnlyTakesScribeToolsAndResearchNotes() {
        assertEquals(2, DeviceContainerRules.RESEARCH_TABLE_SLOTS);
        assertTrue(DeviceContainerRules.researchTableScribeSlot(true));
        assertFalse(DeviceContainerRules.researchTableScribeSlot(false));
        assertTrue(DeviceContainerRules.researchTableNotesSlot(true));
        assertFalse(DeviceContainerRules.researchTableNotesSlot(false));
        assertEquals(14, DeviceContainerRules.researchTableScribeX());
        assertEquals(70, DeviceContainerRules.researchTableNotesX());
        assertEquals(175, DeviceContainerRules.researchTablePlayerY(0));
        assertEquals(233, DeviceContainerRules.researchTableHotbarY());
    }

    // ------------------------------------------------------- quick move routes
    @Test void alchemyFurnaceRoutesFuelAndAspectsToTheMachineSlots() {
        assertEquals(List.of(new Range(2, 38)), DeviceContainerRules.alchemyFurnaceQuickMove(0, false, false));
        assertEquals(List.of(new Range(2, 38)), DeviceContainerRules.alchemyFurnaceQuickMove(1, false, false));
        assertEquals(List.of(new Range(1, 2), new Range(0, 1)), DeviceContainerRules.alchemyFurnaceQuickMove(5, true, false));
        assertEquals(List.of(new Range(0, 1)), DeviceContainerRules.alchemyFurnaceQuickMove(5, false, true));
        assertEquals(List.of(new Range(29, 38)), DeviceContainerRules.alchemyFurnaceQuickMove(5, false, false));
        assertEquals(List.of(new Range(2, 29)), DeviceContainerRules.alchemyFurnaceQuickMove(30, false, false));
    }
    @Test void deconstructionAndFocalManipulatorKeepTheirOriginalRoutes() {
        assertEquals(List.of(new Range(1, 37)), DeviceContainerRules.deconstructionQuickMove(0, false));
        assertEquals(List.of(new Range(0, 1)), DeviceContainerRules.deconstructionQuickMove(5, true));
        assertEquals(List.of(new Range(28, 37)), DeviceContainerRules.deconstructionQuickMove(5, false));
        assertEquals(List.of(new Range(1, 28)), DeviceContainerRules.deconstructionQuickMove(30, false));
        assertEquals(List.of(new Range(1, 37)), DeviceContainerRules.focalManipulatorQuickMove(0, false));
        assertEquals(List.of(new Range(0, 1)), DeviceContainerRules.focalManipulatorQuickMove(5, true));
        assertEquals(List.of(new Range(28, 37)), DeviceContainerRules.focalManipulatorQuickMove(5, false));
        assertEquals(List.of(new Range(1, 28)), DeviceContainerRules.focalManipulatorQuickMove(30, false));
    }
    @Test void researchTableSpaAndThaumatoriumKeepTheirOriginalRoutes() {
        assertEquals(List.of(new Range(2, 38)), DeviceContainerRules.researchTableQuickMove(0));
        assertEquals(List.of(new Range(0, 2)), DeviceContainerRules.researchTableQuickMove(20));
        assertEquals(List.of(new Range(1, 37)), DeviceContainerRules.spaQuickMove(0));
        assertEquals(List.of(new Range(0, 1)), DeviceContainerRules.spaQuickMove(4));
        assertEquals(List.of(new Range(1, 37)), DeviceContainerRules.thaumatoriumQuickMove(0));
        assertEquals(List.of(new Range(0, 1)), DeviceContainerRules.thaumatoriumQuickMove(4));
    }
    @Test void arcaneBoreAndWorkbenchKeepTheirOriginalRoutes() {
        assertEquals(List.of(new Range(2, 38)), DeviceContainerRules.arcaneBoreQuickMove(0, false, false));
        assertEquals(List.of(new Range(0, 1)), DeviceContainerRules.arcaneBoreQuickMove(5, true, false));
        assertEquals(List.of(new Range(1, 2)), DeviceContainerRules.arcaneBoreQuickMove(5, false, true));
        assertTrue(DeviceContainerRules.arcaneBoreQuickMove(5, false, false).isEmpty());
        assertEquals(List.of(new Range(11, 47)), DeviceContainerRules.arcaneWorkbenchQuickMove(0, false));
        assertEquals(List.of(new Range(1, 2)), DeviceContainerRules.arcaneWorkbenchQuickMove(20, true));
        assertEquals(List.of(new Range(38, 47)), DeviceContainerRules.arcaneWorkbenchQuickMove(20, false));
        assertEquals(List.of(new Range(1, 2)), DeviceContainerRules.arcaneWorkbenchQuickMove(40, true));
        assertEquals(List.of(new Range(11, 38)), DeviceContainerRules.arcaneWorkbenchQuickMove(40, false));
        assertEquals(List.of(new Range(11, 47)), DeviceContainerRules.arcaneWorkbenchQuickMove(2, false));
    }
}
