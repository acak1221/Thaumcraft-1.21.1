/**
 * This class was created by <Vazkii>. It's distributed as part of the ThaumicTinkerer Mod.
 *
 * ThaumicTinkerer is Open Source and distributed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0
 * License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 *
 * File Created @ [15 Sep 2013, 00:13:10 (GMT)]
 *
 * Ported to Minecraft 1.21.1 for Thaumcraft 4 Reload (see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.client.gui.button;

import dev.architectury.networking.NetworkManager;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.enchantment.Enchantment;
import thaumcraft.api.aspects.Aspect;
import thaumic.tinkerer.client.gui.EnchantIcons;
import thaumic.tinkerer.client.gui.GuiEnchanting;
import thaumic.tinkerer.common.enchantment.ModEnchantments;
import thaumic.tinkerer.common.enchantment.core.EnchantmentData;
import thaumic.tinkerer.common.enchantment.core.EnchantmentManager;
import thaumic.tinkerer.common.network.packet.PacketEnchanterAddEnchant;

/**
 * The button of one enchantment of the Enchanter's screen: its icon, and its name and base cost when the mouse is on it.
 * Clicking it chooses the enchantment for the tool.
 */
public class GuiButtonEnchantment extends AbstractButton {

    public final ResourceKey<Enchantment> enchant;
    protected final GuiEnchanting parent;

    public GuiButtonEnchantment(GuiEnchanting parent, ResourceKey<Enchantment> enchant, int x, int y) {
        super(x, y, 16, 16, Component.empty());
        this.parent = parent;
        this.enchant = enchant;
    }

    boolean dontRender() {
        return enchant == null || !active || !EnchantmentManager.isKnown(enchant);
    }

    @Override
    public void onPress() {
        NetworkManager.sendToServer(new PacketEnchanterAddEnchant(parent.enchanter, enchant, 0));
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (dontRender()) return;

        EnchantmentData data = EnchantmentManager.data(enchant, 1);
        if (data == null) return;
        EnchantIcons.draw(graphics, data.texture, getX(), getY());

        if (isHovered()) {
            List<Component> tooltip = new ArrayList<>();
            tooltip.add(name().withStyle(ChatFormatting.AQUA));
            for (Aspect aspect : data.aspects.getAspectsSorted()) {
                tooltip.add(Component.literal(" §" + aspect.getChatcolor() + aspect.getName() + '§' + "r x "
                        + data.aspects.getAmount(aspect) + " ")
                        .append(Component.translatable("ttmisc.baseCost")));
            }
            if (this instanceof GuiButtonFramedEnchantment && !parent.enchanter.working) {
                tooltip.add(Component.literal(" ").append(Component.translatable("ttmisc.clickToRemove"))
                        .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
            }
            parent.setTooltipLines(tooltip);
        }
    }

    /** The name of the enchantment: the description of its data, or its id when the world does not have it. */
    protected net.minecraft.network.chat.MutableComponent name() {
        var access = parent.enchanter.getLevel() != null ? parent.enchanter.getLevel().registryAccess() : null;
        return ModEnchantments.holder(access, enchant).map(holder -> holder.value().description().copy())
                .orElseGet(() -> Component.literal(enchant.location().toString()));
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, name());
    }
}
