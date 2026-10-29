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
package thaumic.tinkerer.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.architectury.networking.NetworkManager;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import thaumcraft.api.aspects.Aspect;
import thaumic.tinkerer.client.gui.button.GuiButtonEnchant;
import thaumic.tinkerer.client.gui.button.GuiButtonEnchanterLevel;
import thaumic.tinkerer.client.gui.button.GuiButtonEnchantment;
import thaumic.tinkerer.client.gui.button.GuiButtonFramedEnchantment;
import thaumic.tinkerer.client.lib.LibResources;
import thaumic.tinkerer.common.block.tile.TileEnchanter;
import thaumic.tinkerer.common.block.tile.container.ContainerEnchanter;
import thaumic.tinkerer.common.enchantment.core.EnchantmentManager;
import thaumic.tinkerer.common.network.packet.PacketEnchanterAddEnchant;

/**
 * The screen of the Osmotic Enchanter: the tool and the wand on the left, up to sixteen enchantments to choose from in one
 * or two rows, the chosen ones in frames to the right of the screen with their levels, the six bars of vis, and the button
 * that starts the work. The layout and the artwork are those of 1.7.10; the screen builds its buttons again whenever the tool,
 * the choice or the list of offered enchantments changes (1.7.10 did it every tick).
 */
public class GuiEnchanting extends AbstractContainerScreen<ContainerEnchanter> {

    /** The most enchantments the screen offers at a time (two rows of eight). */
    public static final int MAX_OFFERED = 16;

    private static final ResourceLocation gui = ResourceLocation.parse(LibResources.GUI_ENCHANTER);

    public final TileEnchanter enchanter;
    private List<Component> tooltip = new ArrayList<>();

    private List<ResourceKey<Enchantment>> offered = List.of();
    private ItemStack lastTool = ItemStack.EMPTY;
    private List<ResourceKey<Enchantment>> lastChosen = List.of();
    private List<Integer> lastLevels = List.of();
    private boolean lastWorking;

    public GuiEnchanting(ContainerEnchanter menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.enchanter = menu.enchanter;
    }

    @Override
    protected void init() {
        super.init();
        buildButtonList();
    }

    /** Remembers the state the buttons were built for. */
    private void remember(List<ResourceKey<Enchantment>> newOffered) {
        offered = newOffered;
        lastTool = enchanter.getItem(TileEnchanter.SLOT_TOOL).copy();
        lastChosen = new ArrayList<>(enchanter.enchantments);
        lastLevels = new ArrayList<>(enchanter.levels);
        lastWorking = enchanter.working;
    }

    private boolean stateChanged(List<ResourceKey<Enchantment>> newOffered) {
        return !ItemStack.matches(lastTool, enchanter.getItem(TileEnchanter.SLOT_TOOL))
                || !lastChosen.equals(enchanter.enchantments) || !lastLevels.equals(enchanter.levels)
                || lastWorking != enchanter.working || !offered.equals(newOffered);
    }

    private List<ResourceKey<Enchantment>> currentOffer() {
        if (minecraft == null || minecraft.player == null) return List.of();
        return EnchantmentManager.offered(minecraft.player, enchanter.getItem(TileEnchanter.SLOT_TOOL), enchanter.enchantments,
                MAX_OFFERED);
    }

    public void buildButtonList() {
        clearWidgets();
        List<ResourceKey<Enchantment>> newOffered = currentOffer();
        remember(newOffered);

        GuiButtonEnchant enchantButton = new GuiButtonEnchant(this, enchanter, leftPos + 151, topPos + 33);
        enchantButton.active = !enchanter.enchantments.isEmpty();
        addRenderableWidget(enchantButton);

        boolean twoRows = newOffered.size() > 8;
        for (int i = 0; i < newOffered.size(); i++) {
            int z = i > 7 || !twoRows ? 0 : -24;
            addRenderableWidget(new GuiButtonEnchantment(this, newOffered.get(i), leftPos + 34 + (i % 8) * 16, topPos + 54 + z));
        }

        int i = 0;
        for (ResourceKey<Enchantment> enchant : enchanter.enchantments) {
            if (i >= enchanter.levels.size()) break;
            final int index = i;
            addRenderableWidget(new GuiButtonFramedEnchantment(this, enchant, leftPos + imageWidth + 4, topPos + i * 26));
            addRenderableWidget(new GuiButtonEnchanterLevel(leftPos + imageWidth + 24, topPos + i * 26 - 4, false, () -> {
                int level = levelAt(index);
                send(enchant, level == 1 ? -1 : level - 1);
            }));
            addRenderableWidget(new GuiButtonEnchanterLevel(leftPos + imageWidth + 31, topPos + i * 26 - 4, true,
                    () -> send(enchant, levelAt(index) + 1)));
            ++i;
        }
    }

    private int levelAt(int index) {
        return index < enchanter.levels.size() ? enchanter.levels.get(index) : 1;
    }

    private void send(ResourceKey<Enchantment> enchant, int level) {
        NetworkManager.sendToServer(new PacketEnchanterAddEnchant(enchanter, enchant, level));
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (stateChanged(currentOffer())) buildButtonList();
    }

    /** What the widgets want shown as the tooltip of this frame. */
    public void setTooltipLines(List<Component> lines) {
        this.tooltip = lines;
    }

    public Font getFont() {
        return font;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        tooltip = new ArrayList<>();
        super.render(graphics, mouseX, mouseY, partialTick);
        if (!tooltip.isEmpty()) {
            graphics.renderComponentTooltip(font, tooltip, mouseX, mouseY);
        } else {
            renderTooltip(graphics, mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.setColor(1F, 1F, 1F, 1F);
        graphics.blit(gui, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        ItemStack itemToEnchant = enchanter.getItem(TileEnchanter.SLOT_TOOL);
        boolean twoRows = offered.size() > 8;
        if (twoRows && !itemToEnchant.isEmpty() && !itemToEnchant.isEnchanted()) {
            graphics.blit(gui, leftPos + 30, topPos + 26, 0, imageHeight, 147, 24);
        }

        if (!itemToEnchant.isEmpty() && !itemToEnchant.isEnchanted()) {
            graphics.blit(gui, leftPos + 30, topPos + 50, 0, imageHeight, 147, 24);
        }

        if (!enchanter.enchantments.isEmpty()) {
            int x = leftPos + 40;
            RenderSystem.enableBlend();
            int xo = 15;
            int z = 50;
            if (twoRows) z = 26;
            for (Aspect aspect : TileEnchanter.PRIMAL_ASPECTS) {
                drawAspectBar(graphics, aspect, x + xo, topPos + z, mouseX, mouseY);
                xo += 15;
            }
            RenderSystem.disableBlend();
        }
    }

    /** The title and the label of the inventory are not drawn, as in 1.7.10. */
    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {}

    private void drawAspectBar(GuiGraphics graphics, Aspect aspect, int x, int y, int mx, int my) {
        int totalCost = enchanter.totalAspects.getAmount(aspect);
        int current = enchanter.currentAspects.getAmount(aspect);

        int size = totalCost == 0 ? 11 : 59;

        if (totalCost == 0) {
            graphics.blit(gui, x, y - size, 200, 0, 10, 4);
            graphics.blit(gui, x, y - size + 4, 200, 52, 10, 10);
        } else {
            int pixels = (int) (48D * ((double) Math.min(current, totalCost) / (double) totalCost));
            int color = aspect.getColor();
            graphics.setColor((color >> 16 & 255) / 255F, (color >> 8 & 255) / 255F, (color & 255) / 255F, 1F);
            graphics.blit(gui, x + 1, y - size + 4 + 48 - pixels, 210, 48 - pixels, 8, pixels);
            graphics.setColor(1F, 1F, 1F, 1F);
            graphics.blit(gui, x, y - size, 200, 0, 10, size);
        }

        if (mx > x && mx <= x + 10 && my > y - size && my <= y) {
            List<Component> lines = new ArrayList<>();
            lines.add(Component.literal('§' + aspect.getChatcolor() + aspect.getName()));
            lines.add(Component.literal(current + "/" + totalCost));
            setTooltipLines(lines);
        }
    }
}
