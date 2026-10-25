package thaumcraft.fabric.compat.emi;

import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.widget.WidgetHolder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;

/** Shared layout of the Thaumcraft EMI pages: aspect cost rows and the research line. */
abstract class ThaumcraftEmiRecipe extends BasicEmiRecipe {
    static final int TEXT = 0xFF404040;
    private final String research;

    ThaumcraftEmiRecipe(EmiRecipeCategory category, ResourceLocation id, int width, int height, String research) {
        super(category, id, width, height);
        this.research = research == null ? "" : research;
    }

    /** Aspects in the Thaumonomicon order of a recipe page: alphabetical by tag. */
    static List<AspectEmiStack> aspects(Map<String, Integer> costs) {
        List<AspectEmiStack> result = new ArrayList<>();
        costs.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            Aspect aspect = Aspect.getAspect(entry.getKey());
            if (aspect != null && entry.getValue() > 0) result.add(AspectEmiStack.cost(aspect, entry.getValue()));
        });
        return result;
    }

    /** The arcane workbench lists the primal vis by descending amount, as the book does. */
    static List<AspectEmiStack> vis(Map<String, Integer> costs) {
        List<AspectEmiStack> result = new ArrayList<>(aspects(costs));
        result.sort(Comparator.comparingLong(AspectEmiStack::getAmount).reversed());
        return result;
    }

    static int rows(int count, int perRow) { return count == 0 ? 0 : (count + perRow - 1) / perRow; }

    /** One slot per aspect, {@code perRow} to a row; returns the y below the last row. */
    static int addAspects(WidgetHolder widgets, List<AspectEmiStack> aspects, int x, int y, int perRow) {
        for (int i = 0; i < aspects.size(); i++) {
            widgets.addSlot(aspects.get(i), x + i % perRow * 18, y + i / perRow * 18);
        }
        return y + rows(aspects.size(), perRow) * 18;
    }

    /**
     * Essentia and vis never come from the inventory, so they are catalysts: EMI still lists the
     * recipe under the uses of the aspect, while the craftable filter and the workbench fill
     * handler only look at the items.
     */
    void setAspects(List<AspectEmiStack> aspects) { this.catalysts = List.copyOf(aspects); }

    boolean hasResearch() { return !research.isEmpty(); }

    /**
     * The research that unlocks the recipe: a small Thaumonomicon and the research name, green once
     * the viewing player knows it. The tooltip says whether it is known.
     */
    void addResearch(WidgetHolder widgets, int y) {
        if (research.isEmpty()) return;
        Component name = researchName(research);
        int width = getDisplayWidth();
        ItemStack book = new ItemStack(ThaumcraftContent.THAUMONOMICON.get());
        widgets.addDrawable(0, y, width, 9, (graphics, mouseX, mouseY, delta) -> {
            var font = Minecraft.getInstance().font;
            graphics.pose().pushPose();
            graphics.pose().scale(0.5F, 0.5F, 1);
            graphics.renderFakeItem(book, 0, 0);
            graphics.pose().popPose();
            graphics.drawString(font, trim(name.getString(), width - 10), 10, 0, known(research) ? 0xFF1F6B1F : 0xFF7A2E2E, false);
        });
        widgets.addTooltip((mouseX, mouseY) -> List.of(
                ClientTooltipComponent.create(Component.translatable("tc.port.emi.research", name).getVisualOrderText()),
                ClientTooltipComponent.create(Component.translatable(
                        known(research) ? "tc.port.emi.research_known" : "tc.port.emi.research_unknown")
                        .withStyle(known(research) ? ChatFormatting.GREEN : ChatFormatting.RED).getVisualOrderText())), 0, y, width, 9);
    }

    /** Cuts a line to a width with an ellipsis. */
    static String trim(String text, int width) {
        var font = Minecraft.getInstance().font;
        if (font.width(text) <= width) return text;
        return font.plainSubstrByWidth(text, width - font.width("...")) + "...";
    }

    static Component researchName(String key) {
        var item = ResearchCategories.getResearch(key);
        return item != null ? Component.literal(item.getName()) : Component.translatable("tc.research_name." + key);
    }

    private static boolean known(String key) {
        var player = Minecraft.getInstance().player;
        return player != null && KnowledgeAccess.get(player).isResearchComplete(key);
    }
}
