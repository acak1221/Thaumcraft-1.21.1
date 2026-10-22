package thaumcraft.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import thaumcraft.common.container.ContainerGhostSlots;
import thaumcraft.common.container.SlotGhost;

/**
 * The generic ghost slot preview screen.
 *
 * <p>The original ContainerGhostSlots was a base class whose only screen was
 * the golem configuration GUI, so there is no original screen of its own. This
 * screen is the native adaptation: it draws the original ghost slot frames
 * (three columns of two, exactly as the original golem configuration laid them
 * out, oracle ContainerGolem:56-64) over the vanilla player inventory art and
 * lets the menu own every ghost click.</p>
 *
 * <p>A ghost slot only ever shows a preview. The preview items are ordinary
 * slots of the menu, so the vanilla screen draws them, and the menu refuses
 * every click that would turn a preview into real stock.</p>
 */
public final class GuiGhostSlots extends ThaumcraftContainerScreen<ContainerGhostSlots> {
    /** The vanilla player inventory art of the container. */
    private static final ResourceLocation CONTAINER_ART = ResourceLocation.withDefaultNamespace("textures/gui/container/generic_54.png");
    /** The original ghost slot frame, the material frame of the golem configuration. */
    private static final ResourceLocation GHOST_FRAME = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/gui/guigolem.png");
    private static final int GHOST_FRAME_U = 184, GHOST_FRAME_V = 0;

    public static final int IMAGE_WIDTH = 176;
    public static final int IMAGE_HEIGHT = 166;
    /** The ghost frame art is twenty four pixels wide and sits four pixels above and left of the slot. */
    public static final int GHOST_FRAME_SIZE = 24;
    public static final int GHOST_FRAME_OFFSET = 4;
    /** The player inventory art of the vanilla container starts here, so the frames land on eighty four. */
    public static final int PLAYER_ART_TOP = 70;
    public static final int PLAYER_ART_HEIGHT = 96;

    public GuiGhostSlots(ContainerGhostSlots menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = IMAGE_WIDTH;
        imageHeight = IMAGE_HEIGHT;
    }

    /** Original ContainerGhostSlots:35 - three columns of two ghost previews. */
    public static int ghostSlotX(int index) { return ContainerGhostSlots.GHOST_X + index / 2 * ContainerGhostSlots.GHOST_COLUMN_WIDTH; }
    public static int ghostSlotY(int index) { return ContainerGhostSlots.GHOST_Y + index % 2 * ContainerGhostSlots.GHOST_ROW_HEIGHT; }
    public static int ghostFrameX(int index) { return ghostSlotX(index) - GHOST_FRAME_OFFSET; }
    public static int ghostFrameY(int index) { return ghostSlotY(index) - GHOST_FRAME_OFFSET; }
    /** Original ContainerGhostSlots:44 - the player inventory of the ghost container. */
    public static int playerSlotX(int column) { return 8 + column * 18; }
    public static int playerSlotY(int row) { return 84 + row * 18; }
    public static int hotbarY() { return 142; }
    /** The label sits eleven pixels above the first player frame. */
    public static int inventoryLabelY() { return 73; }

    /** The number of ghost previews this menu really owns. */
    public int ghostSlotCount() {
        int count = 0;
        for (Slot slot : menu.slots) if (slot instanceof SlotGhost) count++;
        return count;
    }

    @Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(CONTAINER_ART, leftPos, topPos + PLAYER_ART_TOP, 0, 126, IMAGE_WIDTH, PLAYER_ART_HEIGHT, 256, 256);
        int ghosts = ghostSlotCount();
        for (int index = 0; index < ghosts; index++) {
            graphics.blit(GHOST_FRAME, leftPos + ghostFrameX(index), topPos + ghostFrameY(index),
                    GHOST_FRAME_U, GHOST_FRAME_V, GHOST_FRAME_SIZE, GHOST_FRAME_SIZE, 256, 256);
        }
    }

    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, playerInventoryTitle, 8, inventoryLabelY(), 0x404040, false);
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
