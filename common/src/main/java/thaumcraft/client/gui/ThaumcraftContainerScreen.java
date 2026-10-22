package thaumcraft.client.gui;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

/**
 * The base of the native container screens.
 *
 * <p>The original draws the aspect row of the hovered slot in the render tick of the client
 * ({@code thaumcraft/client/lib/ClientTickEventsFML.java:282-331,660-729}), i.e. after the whole
 * container screen, including its tooltip, has been drawn. The row is not drawn by this class:
 * the client mixin {@code thaumcraft.common.mixin.client.ContainerScreenAspectRowMixin} injects
 * into the TAIL of {@code Screen#renderWithTooltip} for <b>every</b>
 * {@code AbstractContainerScreen}, so a vanilla chest or furnace shows the row too, exactly like
 * the original {@code GuiContainer}.</p>
 *
 * <p>This class remains the shared base (title, inventory label, tooltip defaults) of the
 * Thaumcraft screens; it must not draw the row a second time.</p>
 *
 * @param <T> the menu of the screen
 */
public abstract class ThaumcraftContainerScreen<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> {
    protected ThaumcraftContainerScreen(T container, Inventory inventory, Component title) {
        super(container, inventory, title);
    }
}
