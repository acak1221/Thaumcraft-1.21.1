/**
 * This class was created by <Vazkii>. It's distributed as part of the ThaumicTinkerer Mod.
 *
 * ThaumicTinkerer is Open Source and distributed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0
 * License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 *
 * File Created @ [9 Sep 2013, 16:33:24 (GMT)]
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

/** A 13 by 13 toggle of the Animation Tablet's screen, drawn from the tablet's texture (off at v 0, on at v 13). */
public class GuiButtonAT extends AbstractButton {

    private static final ResourceLocation gui = ResourceLocation.parse(LibResources.GUI_ANIMATION_TABLET);

    public boolean buttonEnabled = false;
    private final Consumer<GuiButtonAT> onClick;

    public GuiButtonAT(int x, int y, boolean buttonEnabled, Consumer<GuiButtonAT> onClick) {
        super(x, y, 13, 13, Component.empty());
        this.buttonEnabled = buttonEnabled;
        this.onClick = onClick;
    }

    @Override
    public void onPress() {
        onClick.accept(this);
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (active) {
            int y = buttonEnabled ? 13 : 0;
            graphics.blit(gui, getX(), getY(), 176, y, width, height);
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, Component.empty());
    }
}
