/**
 * This class was created by <Vazkii>. It's distributed as part of the ThaumicTinkerer Mod.
 *
 * ThaumicTinkerer is Open Source and distributed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0
 * License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 *
 * Ported to Minecraft 1.21.1 for Thaumcraft 4 Reload (see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.client.gui.button;

import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import thaumic.tinkerer.client.lib.LibResources;

/** A 13 by 13 button of the Remote Placer's screen: drawn (from the placer's texture) only while it is on. */
public class GuiButtonRP extends AbstractButton {

    private static final ResourceLocation gui = ResourceLocation.parse(LibResources.GUI_REMOTE_PLACER);

    public boolean buttonEnabled = false;
    private final Consumer<GuiButtonRP> onClick;

    public GuiButtonRP(int x, int y, boolean enabled, Consumer<GuiButtonRP> onClick) {
        super(x, y, 13, 13, Component.empty());
        this.buttonEnabled = enabled;
        this.onClick = onClick;
    }

    @Override
    public void onPress() {
        onClick.accept(this);
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (buttonEnabled) {
            int y = buttonEnabled ? 13 : 0;
            graphics.blit(gui, getX(), getY(), 176, y, width, height);
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, Component.empty());
    }
}
