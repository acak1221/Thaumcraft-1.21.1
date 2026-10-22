package thaumcraft.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

/**
 * The base of the native container screens.
 *
 * <p>The original draws the aspect row of the hovered slot in the render tick of the
 * client ({@code thaumcraft/client/lib/ClientTickEventsFML.java:282-329}), i.e. after the
 * whole container screen, including its tooltip, has been drawn. The native screens call
 * {@code renderTooltip} at the end of their own render, so the row is drawn from the
 * tooltip hook to keep that order: the row of the original covers the tooltip of the
 * container.</p>
 *
 * @param <T> the menu of the screen
 */
public abstract class ThaumcraftContainerScreen<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> {
    protected ThaumcraftContainerScreen(T container, Inventory inventory, Component title) {
        super(container, inventory, title);
    }

    @Override
    protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderTooltip(graphics, mouseX, mouseY);
        // Original ClientTickEventsFML.renderAspectsInGui:660-729.
        HoveredSlotAspectRow.render(graphics, hoveredSlot, mouseX, mouseY);
    }
}
