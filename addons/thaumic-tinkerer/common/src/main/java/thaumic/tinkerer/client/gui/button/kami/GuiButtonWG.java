/**
 * This class was created by <Vazkii>. It's distributed as part of the ThaumicTinkerer Mod.
 *
 * ThaumicTinkerer is Open Source and distributed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0
 * License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 *
 * File Created @ [Jan 10, 2014, 5:58:46 PM (GMT)]
 * Ported to Minecraft 1.21.1 for Thaumcraft 4 Reload (see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.client.gui.button.kami;

import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import thaumic.tinkerer.client.lib.LibResources;

/** The 13 by 13 button of the Celestial Gateway's screen, drawn from the gateway's texture (176, 0) when on, (176, 13) when off. */
public class GuiButtonWG extends AbstractButton {

    private static final ResourceLocation gui = ResourceLocation.parse(LibResources.GUI_WARP_GATE);

    public boolean enabled = false;
    private final Consumer<GuiButtonWG> onClick;

    public GuiButtonWG(int x, int y, boolean enabled, Consumer<GuiButtonWG> onClick) {
        super(x, y, 13, 13, Component.translatable("ttmisc.lockedGate"));
        this.enabled = enabled;
        this.onClick = onClick;
    }

    @Override
    public void onPress() {
        onClick.accept(this);
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int y = !enabled ? 13 : 0;
        graphics.blit(gui, getX(), getY(), 176, y, width, height);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, getMessage());
    }
}
