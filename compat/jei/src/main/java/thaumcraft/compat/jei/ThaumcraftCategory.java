package thaumcraft.compat.jei;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;

/** Shared layout of the Thaumcraft JEI pages, as the EMI plugin draws them: aspect rows and the research line. */
abstract class ThaumcraftCategory<T> implements IRecipeCategory<T> {
    static final int TEXT = 0xFF404040;
    private final RecipeType<T> type;
    private final Component title;
    private final IDrawable icon;
    final IGuiHelper gui;
    private final int width, height;

    ThaumcraftCategory(RecipeType<T> type, String key, ItemLike icon, IGuiHelper gui, int width, int height) {
        this.type = type;
        this.title = Component.translatable("emi.category.thaumcraft." + key);
        this.icon = gui.createDrawableItemLike(icon);
        this.gui = gui;
        this.width = width;
        this.height = height;
    }

    @Override public RecipeType<T> getRecipeType() { return type; }
    @Override public Component getTitle() { return title; }
    @Override public IDrawable getIcon() { return icon; }
    @Override public int getWidth() { return width; }
    @Override public int getHeight() { return height; }

    /** The research of a page, or "" when it needs none. */
    abstract String research(T recipe);

    /** The y of the research line, under the page's last row. */
    abstract int researchY(T recipe);

    /** Aspects in the Thaumonomicon order of a recipe page: alphabetical by tag. */
    static List<AspectIngredient.Stack> aspects(Map<String, Integer> costs) {
        List<AspectIngredient.Stack> result = new ArrayList<>();
        costs.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            Aspect aspect = Aspect.getAspect(entry.getKey());
            if (aspect != null && entry.getValue() > 0) result.add(new AspectIngredient.Stack(aspect, entry.getValue()));
        });
        return result;
    }

    /** The arcane workbench lists the primal vis by descending amount, as the book does. */
    static List<AspectIngredient.Stack> vis(Map<String, Integer> costs) {
        List<AspectIngredient.Stack> result = new ArrayList<>(aspects(costs));
        result.sort(Comparator.comparingLong(AspectIngredient.Stack::amount).reversed());
        return result;
    }

    static int rows(int count, int perRow) { return count == 0 ? 0 : (count + perRow - 1) / perRow; }

    /** One slot per aspect, {@code perRow} to a row; returns the y below the last row. */
    static int addAspects(IRecipeLayoutBuilder builder, List<AspectIngredient.Stack> aspects, int x, int y, int perRow) {
        for (int i = 0; i < aspects.size(); i++) {
            builder.addSlot(RecipeIngredientRole.INPUT, x + 1 + i % perRow * 18, y + 1 + i / perRow * 18)
                    .setStandardSlotBackground().addIngredient(AspectIngredient.TYPE, aspects.get(i));
        }
        return y + rows(aspects.size(), perRow) * 18;
    }

    /** A small Thaumonomicon and the research name, green once the viewing player knows it. */
    @Override public void draw(T recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        String research = research(recipe);
        if (research.isEmpty()) return;
        int y = researchY(recipe);
        var font = Minecraft.getInstance().font;
        graphics.pose().pushPose();
        graphics.pose().translate(0, y, 0);
        graphics.pose().scale(0.5F, 0.5F, 1);
        graphics.renderFakeItem(new ItemStack(ThaumcraftContent.THAUMONOMICON.get()), 0, 0);
        graphics.pose().popPose();
        graphics.drawString(font, trim(researchName(research).getString(), width - 10), 10, y, known(research) ? 0xFF1F6B1F : 0xFF7A2E2E, false);
    }

    @Override public void getTooltip(ITooltipBuilder tooltip, T recipe, IRecipeSlotsView slots, double mouseX, double mouseY) {
        String research = research(recipe);
        int y = researchY(recipe);
        if (research.isEmpty() || mouseY < y || mouseY >= y + 9 || mouseX < 0 || mouseX >= width) return;
        tooltip.add(Component.translatable("tc.port.emi.research", researchName(research)));
        tooltip.add(Component.translatable(known(research) ? "tc.port.emi.research_known" : "tc.port.emi.research_unknown")
                .withStyle(known(research) ? ChatFormatting.GREEN : ChatFormatting.RED));
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
