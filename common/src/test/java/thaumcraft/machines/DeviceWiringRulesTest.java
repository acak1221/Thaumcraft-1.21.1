package thaumcraft.machines;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import thaumcraft.common.lib.crafting.DeviceContainerRules;
import thaumcraft.common.lib.crafting.DeviceContainerRules.HandMirrorTarget;
import thaumcraft.common.lib.crafting.DeviceContainerRules.MirrorLinkAction;
import thaumcraft.common.lib.world.EldritchBlockRules;

/**
 * The pure decisions of the device glue: the magic box menu, the hand mirror
 * link and the item filter block.
 */
class DeviceWiringRulesTest {

    // ------------------------------------------------------------------ magic box
    @Test void magicBoxMenuCarriesTheSizeOfTheWholeChain() {
        assertEquals(27, DeviceContainerRules.magicBoxMenuSize(27));
        // A linked box owns twenty seven slots per box.
        assertEquals(54, DeviceContainerRules.magicBoxMenuSize(54));
        assertEquals(0, DeviceContainerRules.magicBoxMenuSize(-1));
    }
    @Test void magicBoxMenuBindsThreeRowsWhateverTheChainSizeIs() {
        assertEquals(27, DeviceContainerRules.magicBoxBoundSlots());
        // The original binds three rows of the chain, never more.
        assertTrue(DeviceContainerRules.magicBoxBoundSlots() <= 27);
    }

    // --------------------------------------------------------------- hand mirror
    @Test void handMirrorWithoutALinkHasNoTarget() {
        assertEquals(HandMirrorTarget.NO_LINK, DeviceContainerRules.handMirrorTarget(false, false, false));
        assertEquals(HandMirrorTarget.NO_LINK, DeviceContainerRules.handMirrorTarget(false, true, true));
    }
    @Test void handMirrorAsksForTheWorldBeforeTheMirror() {
        assertEquals(HandMirrorTarget.NO_WORLD, DeviceContainerRules.handMirrorTarget(true, false, false));
        assertEquals(HandMirrorTarget.NO_WORLD, DeviceContainerRules.handMirrorTarget(true, false, true));
        assertEquals(HandMirrorTarget.NO_MIRROR, DeviceContainerRules.handMirrorTarget(true, true, false));
        assertEquals(HandMirrorTarget.MIRROR, DeviceContainerRules.handMirrorTarget(true, true, true));
    }
    @Test void handMirrorScreenOpensOnlyForALiveMirror() {
        assertTrue(DeviceContainerRules.handMirrorOpensView(HandMirrorTarget.MIRROR));
        assertFalse(DeviceContainerRules.handMirrorOpensView(HandMirrorTarget.NO_LINK));
        assertFalse(DeviceContainerRules.handMirrorOpensView(HandMirrorTarget.NO_WORLD));
        assertFalse(DeviceContainerRules.handMirrorOpensView(HandMirrorTarget.NO_MIRROR));
    }
    @Test void handMirrorClearsItsLinkOnlyWhenItsMirrorIsGone() {
        assertTrue(DeviceContainerRules.handMirrorClearsLink(HandMirrorTarget.NO_MIRROR));
        assertFalse(DeviceContainerRules.handMirrorClearsLink(HandMirrorTarget.NO_LINK));
        assertFalse(DeviceContainerRules.handMirrorClearsLink(HandMirrorTarget.NO_WORLD));
        assertFalse(DeviceContainerRules.handMirrorClearsLink(HandMirrorTarget.MIRROR));
    }
    @Test void handMirrorCarriesAnItemOnlyToALiveMirror() {
        assertTrue(DeviceContainerRules.handMirrorTransports(HandMirrorTarget.MIRROR));
        assertFalse(DeviceContainerRules.handMirrorTransports(HandMirrorTarget.NO_LINK));
        assertFalse(DeviceContainerRules.handMirrorTransports(HandMirrorTarget.NO_WORLD));
        assertFalse(DeviceContainerRules.handMirrorTransports(HandMirrorTarget.NO_MIRROR));
    }

    // -------------------------------------------------------------- item filter
    @Test void itemFilterIsTheDedicatedGrateAndTheTwoMetalDeviceVariants() {
        assertTrue(EldritchBlockRules.itemFilterBlock(true, false, 0));
        assertTrue(EldritchBlockRules.itemFilterBlock(false, true, EldritchBlockRules.DEVICE_METAL_GRATE_OPEN));
        assertTrue(EldritchBlockRules.itemFilterBlock(false, true, EldritchBlockRules.DEVICE_METAL_GRATE_CLOSED));
        // Oracle TileGrate:65-67 - the metal device metadata five is the OPEN filter: it takes an
        // item only while it is five and it gives its slot to the top side only. The metadata six
        // is the closed one, a powered filter becomes six and an unpowered one five
        // (oracle BlockMetalDevice:711-720), and damageDropped (oracle BlockMetalDevice:388-396)
        // turns a dropped metadata six into a metadata five.
        assertEquals(5, EldritchBlockRules.DEVICE_METAL_GRATE_OPEN);
        assertEquals(6, EldritchBlockRules.DEVICE_METAL_GRATE_CLOSED);
    }
    @Test void itemFilterIsNoOtherBlock() {
        assertFalse(EldritchBlockRules.itemFilterBlock(false, false, 0));
        assertFalse(EldritchBlockRules.itemFilterBlock(false, true, 0));
        assertFalse(EldritchBlockRules.itemFilterBlock(false, true, 4));
        assertFalse(EldritchBlockRules.itemFilterBlock(false, true, 7));
        assertFalse(EldritchBlockRules.itemFilterBlock(false, true, 12));
    }

    // -------------------------------------------------------------- mirror link
    @Test void mirrorItemBindsItselfToAMirrorWithoutAPartner() {
        assertEquals(MirrorLinkAction.BIND_ITEM, DeviceContainerRules.mirrorItemLink(true, true, false));
        // A mirror with a valid partner refuses a second one.
        assertEquals(MirrorLinkAction.ALREADY_LINKED, DeviceContainerRules.mirrorItemLink(true, true, true));
    }
    @Test void mirrorItemBindsItselfToNoOtherBlock() {
        assertEquals(MirrorLinkAction.UNSUPPORTED, DeviceContainerRules.mirrorItemLink(false, true, false));
        // The essentia mirror has no item form of its own in the native tree.
        assertEquals(MirrorLinkAction.UNSUPPORTED, DeviceContainerRules.mirrorItemLink(true, false, false));
        assertEquals(MirrorLinkAction.UNSUPPORTED, DeviceContainerRules.mirrorItemLink(true, false, true));
    }
}
