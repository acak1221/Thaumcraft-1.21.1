package thaumcraft.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import thaumcraft.common.container.ContainerMagicBox;
import thaumcraft.common.lib.crafting.DeviceContainerRules;

/**
 * Original GuiMagicBox: the vanilla container art over the magic box.
 *
 * <p>The original screen was a vanilla chest GUI over the box inventory, so it
 * drew the generic container art, the box name in the top left corner and the
 * player inventory name above the player inventory, and nothing else.</p>
 *
 * <p>The original client container was a vanilla ContainerChest, whose player
 * inventory sat eighteen pixels higher than the original TC4 server container
 * binds it (oracle ContainerMagicBox:50,54). The native container keeps the
 * original server positions, so the art is drawn eighteen pixels lower here
 * and every frame lines up with the slot that owns it.</p>
 */
public final class GuiMagicBox extends ThaumcraftContainerScreen<ContainerMagicBox> {
    /** Original GuiMagicBox:30 - the vanilla generic container art. */
    private static final ResourceLocation CONTAINER_ART = ResourceLocation.withDefaultNamespace("textures/gui/container/generic_54.png");
    /** Original TileMagicBox:111 - the box is called "Magic Box" and the name is never translated. */
    public static final Component BOX_NAME = Component.literal("Magic Box");

    public static final int IMAGE_WIDTH = 176;
    /** The box art: three rows of eighteen plus the closing border. */
    public static final int BOX_ART_HEIGHT = DeviceContainerRules.MAGIC_BOX_ROWS * 18 + 17;
    /** The player inventory art of the vanilla container. */
    public static final int PLAYER_ART_HEIGHT = 96;
    /** The original TC4 player inventory sits eighteen pixels below the vanilla three row chest. */
    public static final int PLAYER_ART_OFFSET = 18;
    /** The first player frame of the vanilla art, below the top edge of the art. */
    public static final int PLAYER_ART_FRAME_OFFSET = 14;
    public static final int IMAGE_HEIGHT = BOX_ART_HEIGHT + PLAYER_ART_OFFSET + PLAYER_ART_HEIGHT + 1;

    public GuiMagicBox(ContainerMagicBox menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = IMAGE_WIDTH;
        imageHeight = IMAGE_HEIGHT;
    }

    /** The container binds exactly three box rows, whatever the linked size is. */
    public static int boxRows() { return DeviceContainerRules.MAGIC_BOX_ROWS; }
    /** The linked size owns the quick move boundary, exactly as in the original. */
    public static int linkedRows(int boxSize) { return DeviceContainerRules.magicBoxRows(boxSize); }
    public static int boxSlotX(int column) { return DeviceContainerRules.magicBoxSlotX(column); }
    public static int boxSlotY(int row) { return DeviceContainerRules.magicBoxSlotY(row); }
    public static int playerSlotX(int column) { return DeviceContainerRules.magicBoxPlayerX(column); }
    public static int playerSlotY(int row) { return DeviceContainerRules.magicBoxPlayerY(row); }
    public static int hotbarY() { return DeviceContainerRules.magicBoxHotbarY(); }
    /** The twenty seven box slots plus the thirty six player slots. */
    public static int slotCount() { return DeviceContainerRules.MAGIC_BOX_SLOTS + 36; }
    /** The original chest label sits eleven pixels above the first player frame. */
    public static int inventoryLabelY() { return IMAGE_HEIGHT - 94; }

    @Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(CONTAINER_ART, leftPos, topPos, 0, 0, IMAGE_WIDTH, BOX_ART_HEIGHT, 256, 256);
        graphics.blit(CONTAINER_ART, leftPos, topPos + BOX_ART_HEIGHT + PLAYER_ART_OFFSET, 0, 126, IMAGE_WIDTH, PLAYER_ART_HEIGHT, 256, 256);
    }

    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // Original GuiMagicBox:47-48 - the box name and the player inventory name, never a title.
        graphics.drawString(font, BOX_NAME, 8, 6, 0x404040, false);
        graphics.drawString(font, playerInventoryTitle, 8, inventoryLabelY(), 0x404040, false);
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
