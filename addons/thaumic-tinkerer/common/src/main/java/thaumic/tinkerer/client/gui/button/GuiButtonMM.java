/**
 * This class was created by <Vazkii>. It's distributed as part of the ThaumicTinkerer Mod.
 *
 * ThaumicTinkerer is Open Source and distributed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0
 * License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 *
 * File Created @ [12 Sep 2013, 18:39:55 (GMT)]
 *
 * Ported to Minecraft 1.21.1 for Thaumcraft 4 Reload (see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.client.gui.button;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import thaumic.tinkerer.client.lib.LibResources;

/** A button of the Corporeal Attractor screen: invisible, it draws a marker over its box while it is switched on. */
public class GuiButtonMM extends AbstractButton {

    private static final ResourceLocation gui = ResourceLocation.parse(LibResources.GUI_MOB_MAGNET);

    public boolean enabled = false;
    private final Runnable onClick;

    public GuiButtonMM(int x, int y, boolean enabled, Component message, Runnable onClick) {
        super(x, y, 13, 13, message);
        this.enabled = enabled;
        this.onClick = onClick;
    }

    @Override
    public void onPress() {
        onClick.run();
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (enabled) {
            int v = enabled ? 13 : 0;
            graphics.setColor(1F, 1F, 1F, 1F);
            graphics.blit(gui, getX(), getY(), 176, v, width, height);
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
