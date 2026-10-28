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
package thaumic.tinkerer.client.gui.kami;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import thaumic.tinkerer.client.lib.LibResources;
import thaumic.tinkerer.common.block.tile.container.kami.ContainerIchorPouch;

/**
 * The screen of the Bottomless Pouch: a 256 x 256 texture with 13 x 9 slots. The hotbar-key dupe that 1.7.10's
 * {@code checkHotbarKeys} override closed is closed by the menu ({@link ContainerIchorPouch#clicked}).
 */
public class GuiIchorPouch extends AbstractContainerScreen<ContainerIchorPouch> {
    private static final ResourceLocation TEXTURE = ResourceLocation.parse(LibResources.GUI_ICHOR_POUCH);

    public GuiIchorPouch(ContainerIchorPouch menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 256;
        this.imageHeight = 256;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
    }

    /** The 1.7.10 screen drew no titles. */
    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {}
}
