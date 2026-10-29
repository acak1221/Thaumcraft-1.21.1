/**
 * This class was created by <Vazkii>. It's distributed as part of the ThaumicTinkerer Mod.
 *
 * ThaumicTinkerer is Open Source and distributed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0
 * License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 *
 * File Created @ [16 Sep 2013, 15:31:55 (GMT)]
 *
 * Ported to Minecraft 1.21.1 for Thaumcraft 4 Reload (see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.client.gui.button;

import dev.architectury.networking.NetworkManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.enchantment.Enchantment;
import thaumic.tinkerer.client.gui.GuiEnchanting;
import thaumic.tinkerer.client.lib.LibResources;
import thaumic.tinkerer.common.network.packet.PacketEnchanterAddEnchant;

/** A chosen enchantment: in a frame next to the screen with its level; clicking it takes it off again. */
public class GuiButtonFramedEnchantment extends GuiButtonEnchantment {

    private static final ResourceLocation gui = ResourceLocation.parse(LibResources.GUI_ENCHANTER);

    public GuiButtonFramedEnchantment(GuiEnchanting parent, ResourceKey<Enchantment> enchant, int x, int y) {
        super(parent, enchant, x, y);
    }

    @Override
    public void onPress() {
        NetworkManager.sendToServer(new PacketEnchanterAddEnchant(parent.enchanter, enchant, -1));
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (dontRender() || parent.enchanter.enchantments.isEmpty() || parent.enchanter.levels.isEmpty()) return;

        graphics.blit(gui, getX() - 4, getY() - 4, 176, 0, 24, 24);

        int index = parent.enchanter.enchantments.indexOf(enchant);
        if (index != -1 && index < parent.enchanter.levels.size()) {
            int level = parent.enchanter.levels.get(index);
            graphics.drawString(parent.getFont(), Component.translatable("enchantment.level." + level), getX() + 26,
                    getY() + 8, 0xFFFFFF, true);
        }

        super.renderWidget(graphics, mouseX, mouseY, partialTick);
    }
}
