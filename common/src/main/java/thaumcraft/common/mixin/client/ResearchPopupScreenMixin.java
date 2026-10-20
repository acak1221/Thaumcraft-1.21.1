package thaumcraft.common.mixin.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Original RenderTick END placed research popups above open book/inventory screens, not underneath them. */
@Mixin(Screen.class)
public abstract class ResearchPopupScreenMixin {
    @Inject(method="renderWithTooltip",at=@At("TAIL"))
    private void thaumcraft$researchPopup(GuiGraphics graphics,int mouseX,int mouseY,float partial,CallbackInfo callback) {
        thaumcraft.client.gui.GuiResearchPopup.renderHud(graphics);
    }
}
