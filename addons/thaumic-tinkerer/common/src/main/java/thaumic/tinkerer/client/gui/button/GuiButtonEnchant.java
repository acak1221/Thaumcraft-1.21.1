/**
 * This class was created by <Vazkii>. It's distributed as part of the ThaumicTinkerer Mod.
 *
 * ThaumicTinkerer is Open Source and distributed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0
 * License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 *
 * File Created @ [14 Sep 2013, 21:56:21 (GMT)]
 *
 * Ported to Minecraft 1.21.1 for Thaumcraft 4 Reload (see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.client.gui.button;

import dev.architectury.networking.NetworkManager;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import thaumic.tinkerer.client.gui.GuiEnchanting;
import thaumic.tinkerer.client.lib.LibResources;
import thaumic.tinkerer.common.block.tile.TileEnchanter;
import thaumic.tinkerer.common.network.packet.PacketEnchanterStartWorking;

/** The button of the Enchanter's screen that starts the work (15 by 15, drawn only while enchantments are chosen). */
public class GuiButtonEnchant extends AbstractButton {

    private static final ResourceLocation gui = ResourceLocation.parse(LibResources.GUI_ENCHANTER);
    private final TileEnchanter enchanter;
    private final GuiEnchanting parent;

    public GuiButtonEnchant(GuiEnchanting parent, TileEnchanter enchanter, int x, int y) {
        super(x, y, 15, 15, Component.translatable("ttmisc.startEnchant"));
        this.enchanter = enchanter;
        this.parent = parent;
    }

    @Override
    public void onPress() {
        NetworkManager.sendToServer(new PacketEnchanterStartWorking(enchanter));
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (!active) return;

        final int x = 176;
        final int y = enchanter.working ? 39 : 24;

        graphics.blit(gui, getX(), getY(), x, y, 15, 15);

        if (isHovered() && !enchanter.working) {
            parent.setTooltipLines(List.of(Component.translatable("ttmisc.startEnchant").withStyle(ChatFormatting.AQUA)));
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, getMessage());
    }
}
