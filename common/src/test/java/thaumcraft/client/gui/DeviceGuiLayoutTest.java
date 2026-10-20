package thaumcraft.client.gui;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import thaumcraft.common.container.ContainerGhostSlots;
import thaumcraft.common.lib.crafting.DeviceContainerRules;

/**
 * The device screens draw exactly the slots their containers bind.
 *
 * <p>Every number is the original one: oracle ContainerMagicBox:38-56,
 * ContainerHandMirror:43-58 and ContainerGolem:56-64 (the only original screen
 * of the ghost slot base class).</p>
 */
class DeviceGuiLayoutTest {

    // ------------------------------------------------------------- magic box
    @Test void magicBoxDrawsTheThreeBoundBoxRows() {
        assertEquals(3, GuiMagicBox.boxRows());
        assertEquals(DeviceContainerRules.MAGIC_BOX_ROWS, GuiMagicBox.boxRows());
        assertEquals(27, GuiMagicBox.boxRows() * 9);
        assertEquals(8, GuiMagicBox.boxSlotX(0));
        assertEquals(152, GuiMagicBox.boxSlotX(8));
        assertEquals(18, GuiMagicBox.boxSlotY(0));
        assertEquals(54, GuiMagicBox.boxSlotY(2));
    }
    @Test void magicBoxPlayerInventoryMatchesItsContainer() {
        assertEquals(DeviceContainerRules.magicBoxPlayerX(0), GuiMagicBox.playerSlotX(0));
        assertEquals(8, GuiMagicBox.playerSlotX(0));
        assertEquals(152, GuiMagicBox.playerSlotX(8));
        assertEquals(103, GuiMagicBox.playerSlotY(0));
        assertEquals(139, GuiMagicBox.playerSlotY(2));
        assertEquals(161, GuiMagicBox.hotbarY());
    }
    @Test void magicBoxArtPlacesItsFramesOnTheBoundSlots() {
        // The vanilla art carries the first player frame fourteen pixels below
        // its own top edge, so the art has to be drawn on the bound slots.
        assertEquals(GuiMagicBox.playerSlotY(0), GuiMagicBox.BOX_ART_HEIGHT
                + GuiMagicBox.PLAYER_ART_OFFSET + GuiMagicBox.PLAYER_ART_FRAME_OFFSET);
        assertEquals(GuiMagicBox.hotbarY(), GuiMagicBox.playerSlotY(0) + 58);
        assertEquals(3 * 18 + 17, GuiMagicBox.BOX_ART_HEIGHT);
    }
    @Test void magicBoxKeepsTheLinkedSizeForTheQuickMoveOnly() {
        // A linked box owns more rows than the container binds, exactly as in
        // the original, so only the three bound rows are ever drawn.
        assertEquals(3, GuiMagicBox.linkedRows(27));
        assertEquals(6, GuiMagicBox.linkedRows(54));
        assertEquals(3, GuiMagicBox.boxRows());
    }
    @Test void magicBoxBindsTwentySevenBoxSlotsAndThirtySixPlayerSlots() {
        assertEquals(63, GuiMagicBox.slotCount());
        assertEquals(27 + 36, GuiMagicBox.slotCount());
    }

    // ------------------------------------------------------------ hand mirror
    @Test void handMirrorKeepsItsSingleInputSlot() {
        assertEquals(80, GuiHandMirror.inputSlotX());
        assertEquals(24, GuiHandMirror.inputSlotY());
        assertEquals(DeviceContainerRules.HAND_MIRROR_INPUT_X, GuiHandMirror.inputSlotX());
        assertEquals(DeviceContainerRules.HAND_MIRROR_INPUT_Y, GuiHandMirror.inputSlotY());
        assertEquals(1, GuiHandMirror.slotCount() - 36);
        assertEquals(37, GuiHandMirror.slotCount());
    }
    @Test void handMirrorPlayerInventoryMatchesItsContainer() {
        assertEquals(8, GuiHandMirror.playerSlotX(0));
        assertEquals(152, GuiHandMirror.playerSlotX(8));
        assertEquals(84, GuiHandMirror.playerSlotY(0));
        assertEquals(120, GuiHandMirror.playerSlotY(2));
        assertEquals(142, GuiHandMirror.hotbarY());
    }
    @Test void handMirrorShowsTheLinkedInventoryForAnEqualStackOnly() {
        assertTrue(GuiHandMirror.linkedInventoryRequested(false, true));
        assertFalse(GuiHandMirror.linkedInventoryRequested(true, true));
        assertFalse(GuiHandMirror.linkedInventoryRequested(false, false));
    }

    // ------------------------------------------------------------ ghost slots
    @Test void ghostSlotsKeepTheOriginalPreviewGrid() {
        assertEquals(100, GuiGhostSlots.ghostSlotX(0));
        assertEquals(16, GuiGhostSlots.ghostSlotY(0));
        assertEquals(ContainerGhostSlots.GHOST_X, GuiGhostSlots.ghostSlotX(0));
        assertEquals(ContainerGhostSlots.GHOST_Y, GuiGhostSlots.ghostSlotY(0));
        assertEquals(ContainerGhostSlots.GHOST_COLUMN_WIDTH, GuiGhostSlots.ghostSlotX(2) - GuiGhostSlots.ghostSlotX(0));
        assertEquals(ContainerGhostSlots.GHOST_ROW_HEIGHT, GuiGhostSlots.ghostSlotY(1) - GuiGhostSlots.ghostSlotY(0));
        // Three columns of two previews, exactly as the original golem screen.
        assertEquals(156, GuiGhostSlots.ghostSlotX(4));
        assertEquals(47, GuiGhostSlots.ghostSlotY(5));
    }
    @Test void ghostSlotsDrawTheirFrameOnThePreviewSlot() {
        assertEquals(GuiGhostSlots.ghostSlotX(0), GuiGhostSlots.ghostFrameX(0) + GuiGhostSlots.GHOST_FRAME_OFFSET);
        assertEquals(GuiGhostSlots.ghostSlotY(0), GuiGhostSlots.ghostFrameY(0) + GuiGhostSlots.GHOST_FRAME_OFFSET);
        assertEquals(24, GuiGhostSlots.GHOST_FRAME_SIZE);
    }
    @Test void ghostSlotsPlayerInventoryMatchesItsContainer() {
        assertEquals(8, GuiGhostSlots.playerSlotX(0));
        assertEquals(152, GuiGhostSlots.playerSlotX(8));
        assertEquals(84, GuiGhostSlots.playerSlotY(0));
        assertEquals(120, GuiGhostSlots.playerSlotY(2));
        assertEquals(142, GuiGhostSlots.hotbarY());
        // The vanilla art has to place its frames on the bound slots.
        assertEquals(GuiGhostSlots.playerSlotY(0), GuiGhostSlots.PLAYER_ART_TOP + 14);
        assertEquals(GuiGhostSlots.hotbarY(), GuiGhostSlots.playerSlotY(0) + 58);
    }
}
