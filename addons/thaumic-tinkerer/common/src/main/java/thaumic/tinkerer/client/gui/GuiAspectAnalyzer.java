/**
 * This class was created by <Vazkii>. It's distributed as part of the ThaumicTinkerer Mod.
 *
 * ThaumicTinkerer is Open Source and distributed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0
 * License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 *
 * File Created @ [Dec 11, 2013, 10:46:14 PM (GMT)]
 *
 * Ported to Minecraft 1.21.1 for Thaumcraft 4 Reload (see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.client.gui;

import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.client.AspectRenderer;
import thaumic.tinkerer.client.lib.LibResources;
import thaumic.tinkerer.common.block.tile.container.ContainerAspectAnalyzer;
import thaumic.tinkerer.common.core.helper.AnalyzerKnowledge;

public class GuiAspectAnalyzer extends AbstractContainerScreen<ContainerAspectAnalyzer> {

    private static final ResourceLocation gui = ResourceLocation.parse(LibResources.GUI_ASPECT_ANALYZER);

    private Aspect aspectHovered = null;

    public GuiAspectAnalyzer(ContainerAspectAnalyzer menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mx, int my) {
        aspectHovered = null;

        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        graphics.blit(gui, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        ItemStack stack = menu.getSlot(0).getItem();
        if (!stack.isEmpty() && minecraft != null && minecraft.player != null) {
            AspectList tags = ThaumcraftApiHelper.getObjectAspects(stack, minecraft.player.level());
            if (tags != null && AnalyzerKnowledge.hasScanned(minecraft.player, stack)) {
                int i = 0;
                for (Aspect aspect : tags.getAspectsSortedAmount()) {
                    int x = leftPos + 20 + i * 18;
                    int y = topPos + 58;
                    AspectRenderer.draw(graphics, aspect, x, y, tags.getAmount(aspect), 0);

                    if (mx > x && mx < x + 16 && my > y && my < y + 16) aspectHovered = aspect;

                    i++;
                }
            }
        }
    }

    /** 1.7.10 drew no title and no "Inventory" label in this screen. */
    @Override
    protected void renderLabels(GuiGraphics graphics, int mx, int my) {}

    @Override
    public void render(GuiGraphics graphics, int mx, int my, float partialTick) {
        super.render(graphics, mx, my, partialTick);

        if (aspectHovered != null) {
            graphics.renderTooltip(font,
                    List.of(Component.literal(aspectHovered.getName()).withStyle(ChatFormatting.AQUA),
                            Component.literal(aspectHovered.getLocalizedDescription()).withStyle(ChatFormatting.GRAY)),
                    Optional.empty(), mx, my);
        } else {
            renderTooltip(graphics, mx, my);
        }
    }
}
