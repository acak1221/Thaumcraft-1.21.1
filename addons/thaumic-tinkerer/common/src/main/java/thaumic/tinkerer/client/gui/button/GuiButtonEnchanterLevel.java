/**
 * This class was created by <Vazkii>. It's distributed as part of the ThaumicTinkerer Mod.
 *
 * ThaumicTinkerer is Open Source and distributed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0
 * License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 *
 * File Created @ [16 Sep 2013, 16:13:50 (GMT)]
 *
 * Ported to Minecraft 1.21.1 for Thaumcraft 4 Reload (see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.client.gui.button;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import thaumic.tinkerer.client.lib.LibResources;

/** The small "-" and "+" next to a chosen enchantment (7 by 7). */
public class GuiButtonEnchanterLevel extends AbstractButton {

    private static final ResourceLocation gui = ResourceLocation.parse(LibResources.GUI_ENCHANTER);

    private final boolean plus;
    private final Runnable onClick;

    public GuiButtonEnchanterLevel(int x, int y, boolean plus, Runnable onClick) {
        super(x, y, 7, 7, Component.literal(plus ? "+" : "-"));
        this.plus = plus;
        this.onClick = onClick;
    }

    @Override
    public void onPress() {
        onClick.run();
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (!active) return;

        int x = 218 + (plus ? 7 : 0);
        graphics.blit(gui, getX(), getY(), x, 0, 7, 7);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, getMessage());
    }
}
