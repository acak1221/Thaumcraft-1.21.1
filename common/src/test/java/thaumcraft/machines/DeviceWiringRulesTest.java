package thaumcraft.machines;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import thaumcraft.common.lib.crafting.DeviceWiringRules;
import thaumcraft.common.lib.crafting.DeviceWiringRules.HandMirrorTarget;
import thaumcraft.common.lib.crafting.DeviceWiringRules.MirrorLinkAction;

/**
 * The pure decisions of the device glue: the magic box menu, the hand mirror
 * link and the item filter block.
 */
class DeviceWiringRulesTest {

    // ------------------------------------------------------------------ magic box
    @Test void magicBoxMenuCarriesTheSizeOfTheWholeChain() {
        assertEquals(27, DeviceWiringRules.magicBoxMenuSize(27));
        // A linked box owns twenty seven slots per box.
        assertEquals(54, DeviceWiringRules.magicBoxMenuSize(54));
        assertEquals(0, DeviceWiringRules.magicBoxMenuSize(-1));
    }
    @Test void magicBoxMenuBindsThreeRowsWhateverTheChainSizeIs() {
        assertEquals(27, DeviceWiringRules.magicBoxBoundSlots());
        // The original binds three rows of the chain, never more.
        assertTrue(DeviceWiringRules.magicBoxBoundSlots() <= 27);
    }

    // --------------------------------------------------------------- hand mirror
    @Test void handMirrorWithoutALinkHasNoTarget() {
        assertEquals(HandMirrorTarget.NO_LINK, DeviceWiringRules.handMirrorTarget(false, false, false));
        assertEquals(HandMirrorTarget.NO_LINK, DeviceWiringRules.handMirrorTarget(false, true, true));
    }
    @Test void handMirrorAsksForTheWorldBeforeTheMirror() {
        assertEquals(HandMirrorTarget.NO_WORLD, DeviceWiringRules.handMirrorTarget(true, false, false));
        assertEquals(HandMirrorTarget.NO_WORLD, DeviceWiringRules.handMirrorTarget(true, false, true));
        assertEquals(HandMirrorTarget.NO_MIRROR, DeviceWiringRules.handMirrorTarget(true, true, false));
        assertEquals(HandMirrorTarget.MIRROR, DeviceWiringRules.handMirrorTarget(true, true, true));
    }
    @Test void handMirrorScreenOpensOnlyForALiveMirror() {
        assertTrue(DeviceWiringRules.handMirrorOpensScreen(HandMirrorTarget.MIRROR));
        assertFalse(DeviceWiringRules.handMirrorOpensScreen(HandMirrorTarget.NO_LINK));
        assertFalse(DeviceWiringRules.handMirrorOpensScreen(HandMirrorTarget.NO_WORLD));
        assertFalse(DeviceWiringRules.handMirrorOpensScreen(HandMirrorTarget.NO_MIRROR));
    }
    @Test void handMirrorClearsItsLinkOnlyWhenItsMirrorIsGone() {
        assertTrue(DeviceWiringRules.handMirrorClearsLink(HandMirrorTarget.NO_MIRROR));
        assertFalse(DeviceWiringRules.handMirrorClearsLink(HandMirrorTarget.NO_LINK));
        assertFalse(DeviceWiringRules.handMirrorClearsLink(HandMirrorTarget.NO_WORLD));
        assertFalse(DeviceWiringRules.handMirrorClearsLink(HandMirrorTarget.MIRROR));
    }
    @Test void handMirrorCarriesAnItemOnlyToALiveMirror() {
        assertTrue(DeviceWiringRules.handMirrorTransports(HandMirrorTarget.MIRROR));
        assertFalse(DeviceWiringRules.handMirrorTransports(HandMirrorTarget.NO_LINK));
        assertFalse(DeviceWiringRules.handMirrorTransports(HandMirrorTarget.NO_WORLD));
        assertFalse(DeviceWiringRules.handMirrorTransports(HandMirrorTarget.NO_MIRROR));
    }

    // -------------------------------------------------------------- item filter
    @Test void itemFilterIsTheDedicatedGrateAndTheTwoMetalDeviceVariants() {
        assertTrue(DeviceWiringRules.itemFilterBlock(true, false, 0));
        assertTrue(DeviceWiringRules.itemFilterBlock(false, true, DeviceWiringRules.ITEM_FILTER_OPEN));
        assertTrue(DeviceWiringRules.itemFilterBlock(false, true, DeviceWiringRules.ITEM_FILTER_CLOSED));
        // The original metal device metadata five is the open filter and six the closed one.
        assertEquals(5, DeviceWiringRules.ITEM_FILTER_OPEN);
        assertEquals(6, DeviceWiringRules.ITEM_FILTER_CLOSED);
    }
    @Test void itemFilterIsNoOtherBlock() {
        assertFalse(DeviceWiringRules.itemFilterBlock(false, false, 0));
        assertFalse(DeviceWiringRules.itemFilterBlock(false, true, 0));
        assertFalse(DeviceWiringRules.itemFilterBlock(false, true, 4));
        assertFalse(DeviceWiringRules.itemFilterBlock(false, true, 7));
        assertFalse(DeviceWiringRules.itemFilterBlock(false, true, 12));
    }

    // -------------------------------------------------------------- mirror link
    @Test void mirrorItemBindsItselfToAMirrorWithoutAPartner() {
        assertEquals(MirrorLinkAction.BIND_ITEM, DeviceWiringRules.mirrorItemLink(true, true, false));
        // A mirror with a valid partner refuses a second one.
        assertEquals(MirrorLinkAction.ALREADY_LINKED, DeviceWiringRules.mirrorItemLink(true, true, true));
    }
    @Test void mirrorItemBindsItselfToNoOtherBlock() {
        assertEquals(MirrorLinkAction.UNSUPPORTED, DeviceWiringRules.mirrorItemLink(false, true, false));
        // The essentia mirror has no item form of its own in the native tree.
        assertEquals(MirrorLinkAction.UNSUPPORTED, DeviceWiringRules.mirrorItemLink(true, false, false));
        assertEquals(MirrorLinkAction.UNSUPPORTED, DeviceWiringRules.mirrorItemLink(true, false, true));
    }
}
