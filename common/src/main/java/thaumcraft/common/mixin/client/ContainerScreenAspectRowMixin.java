package thaumcraft.common.mixin.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thaumcraft.client.gui.HoveredSlotAspectRow;

/**
 * The aspect row of the hovered container slot, for every container screen.
 *
 * <p>Original {@code thaumcraft/client/lib/ClientTickEventsFML.java:282-331,660-729}: the row is
 * drawn during the render tick of the client, i.e. after the whole container screen, including
 * its tooltip, has been drawn. The native screens of the port draw it from their own
 * {@code renderTooltip} hook, which only covers the Thaumcraft screens; a vanilla chest or
 * furnace had no row.</p>
 *
 * <p>1.21.1 draws a screen in two steps: {@code Screen#render} and then the final
 * {@code Screen#renderWithTooltip} ({@code net.minecraft.client.gui.screens.Screen}), which
 * first calls {@code renderTooltip} and then draws the deferred tooltip. The TAIL of
 * {@code renderWithTooltip} is therefore exactly the point of the original: after the screen and
 * after its tooltip. The method is {@code final}, so the injection is the only legal hook, and
 * it runs once per frame for every screen. The guard restricts it to
 * {@link AbstractContainerScreen}, the {@code GuiContainer} of the original.</p>
 *
 * <p>{@code HoveredSlotAspectRow} itself owns the whole rule, including the shift/configuration
 * decision and the scanned-item test, so this mixin only supplies the hovered slot.</p>
 */
@Mixin(Screen.class)
public abstract class ContainerScreenAspectRowMixin {
    @Inject(method = "renderWithTooltip", at = @At("TAIL"))
    private void thaumcraft$renderHoveredSlotAspects(GuiGraphics graphics, int mouseX, int mouseY,
                                                     float partialTick, CallbackInfo callback) {
        if (!((Object) this instanceof AbstractContainerScreen<?> screen)) return;
        HoveredSlotAspectRow.render(graphics,
                ((AbstractContainerScreenAccess) screen).thaumcraft$hoveredSlot(), mouseX, mouseY);
    }
}
