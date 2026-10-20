package thaumcraft.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import thaumcraft.common.container.ContainerHandMirror;
import thaumcraft.common.lib.crafting.DeviceContainerRules;

/**
 * Original GuiHandMirror: the hand mirror art, one input slot, the player
 * inventory and no label at all.
 *
 * <p>The original screen refused the hotbar number keys (oracle
 * GuiHandMirror:35-37). The modern client only handles those keys while no
 * screen is open, so the original behaviour needs no override.</p>
 *
 * <p>Original ContainerHandMirror:62-63: a stack equal to the mirror in the
 * input slot replaces the mirror GUI with the player inventory. The server
 * closes the mirror menu for that case, so the client owns the player
 * inventory screen and takes the menu over from the mirror menu.</p>
 */
public final class GuiHandMirror extends AbstractContainerScreen<ContainerHandMirror> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/gui/guihandmirror.png");

    public static final int IMAGE_WIDTH = 176;
    public static final int IMAGE_HEIGHT = 166;

    private boolean linkedInventoryShown;

    public GuiHandMirror(ContainerHandMirror menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = IMAGE_WIDTH;
        imageHeight = IMAGE_HEIGHT;
    }

    /** The single input slot of the original ContainerHandMirror:44. */
    public static int inputSlotX() { return DeviceContainerRules.HAND_MIRROR_INPUT_X; }
    public static int inputSlotY() { return DeviceContainerRules.HAND_MIRROR_INPUT_Y; }
    public static int playerSlotX(int column) { return 8 + column * 18; }
    public static int playerSlotY(int row) { return DeviceContainerRules.HAND_MIRROR_PLAYER_Y + row * 18; }
    public static int hotbarY() { return DeviceContainerRules.HAND_MIRROR_HOTBAR_Y; }
    /** The input slot plus the thirty six player slots. */
    public static int slotCount() { return DeviceContainerRules.HAND_MIRROR_INPUT_SLOTS + 36; }

    /**
     * Original ContainerHandMirror:62 - a stack equal to the mirror in the
     * input slot replaces the mirror GUI with the player inventory.
     */
    public static boolean linkedInventoryRequested(boolean slotEmpty, boolean slotEqualsMirror) {
        return DeviceContainerRules.handMirrorShowsLinkedInventory(slotEmpty, slotEqualsMirror);
    }

    @Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, IMAGE_WIDTH, IMAGE_HEIGHT, 256, 256);
    }

    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // Original GuiHandMirror:32-33 - the original screen draws no label at all.
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // The mirror in the slot replaces the whole GUI with the player inventory.
        if (!linkedInventoryShown && linkedInventoryRequested()) openLinkedInventory();
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    private boolean linkedInventoryRequested() {
        ItemStack mirror = menu.mirror();
        ItemStack input = menu.inputStack();
        boolean equalsMirror = !mirror.isEmpty() && ItemStack.matches(input, mirror);
        return linkedInventoryRequested(input.isEmpty(), equalsMirror);
    }

    private void openLinkedInventory() {
        if (linkedInventoryShown || minecraft == null || minecraft.player == null) return;
        linkedInventoryShown = true;
        // Original ContainerHandMirror:63 - the player inventory replaces the
        // mirror GUI, so the mirror menu hands the player inventory over.
        minecraft.player.containerMenu = minecraft.player.inventoryMenu;
        minecraft.setScreen(new InventoryScreen(minecraft.player));
    }
}
