package thaumcraft.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.research.ResearchBrowserData;
import thaumcraft.common.lib.research.ResearchDefinition;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Gated original text/illustrations and resolved native crafting/arcane recipe pages. */
public final class GuiResearchRecipe extends Screen {
    private static final ResourceLocation BOOK = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/gui/gui_researchbook.png");
    private final Screen parent;
    private final ResearchDefinition research;
    private final List<BookPage> pages = new ArrayList<>();
    private List<ResearchDefinition.Page> descriptors = List.of();
    private Map<ResearchDefinition.Page, ThaumcraftRecipeDisplay> recipes = Map.of();
    private int page, left, top, bookWidth, bookHeight, textWidth, textHeight;
    private Button previous, next;
    private record Draw(FormattedCharSequence text, ResearchText.Image image, int y, int width, int height) {}
    private record BookPage(List<Draw> text, ThaumcraftRecipeDisplay recipe) {}

    public GuiResearchRecipe(Screen parent, ResearchDefinition research) {
        super(Component.translatable(research.nameKey())); this.parent = parent; this.research = research;
    }
    @Override protected void init() {
        bookWidth = Math.min(480, Math.min(width - 16, (height - 42) * 512 / 360));
        bookHeight = bookWidth * 360 / 512;
        left = (width - bookWidth) / 2; top = (height - bookHeight - 24) / 2;
        textWidth = bookWidth * 215 / 512; textHeight = bookHeight - 54;
        refreshPages(true);
        previous = addRenderableWidget(Button.builder(Component.literal("←"), b -> { page -= 2; updateButtons(); }).bounds(left, top + bookHeight + 4, 36, 20).build());
        next = addRenderableWidget(Button.builder(Component.literal("→"), b -> { page += 2; updateButtons(); }).bounds(left + bookWidth - 36, top + bookHeight + 4, 36, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.back"), b -> onClose()).bounds(left + bookWidth / 2 - 40, top + bookHeight + 4, 80, 20).build());
        updateButtons();
    }
    private void updateButtons() { previous.active = page > 0; next.active = page + 2 < pages.size(); }
    private void refreshPages(boolean force) {
        var complete = minecraft.player == null ? Set.<String>of() : KnowledgeAccess.get(minecraft.player).getResearchComplete();
        Map<ResearchDefinition.Page, ThaumcraftRecipeDisplay> resolved = new LinkedHashMap<>();
        var visible = ResearchBrowserData.displayablePages(research, complete, descriptor -> {
            var recipe = ThaumcraftRecipeDisplay.resolve(descriptor, research.key(), minecraft.level);
            if (recipe == null) return false;
            resolved.put(descriptor, recipe);
            return true;
        });
        // RecipeManager itself can survive a reload. Compare holder identity,
        // not just manager/ID/value equality, so replaced recipes never stay cached.
        boolean changed = resolved.size() != recipes.size() || resolved.entrySet().stream().anyMatch(entry ->
                !recipes.containsKey(entry.getKey()) || !recipes.get(entry.getKey()).sameRecipes(entry.getValue()));
        if (!force && visible.equals(descriptors) && !changed) return;
        descriptors = visible;
        recipes = Map.copyOf(resolved);
        layoutPages();
        page = Math.max(0, Math.min(page, Math.max(0, (pages.size() - 1) / 2 * 2)));
        if (previous != null && next != null) updateButtons();
    }
    private void layoutPages() {
        pages.clear();
        List<Draw> current = new ArrayList<>(); int y = 0;
        for (var descriptor : descriptors) {
            if (!current.isEmpty()) { pages.add(new BookPage(List.copyOf(current), null)); current.clear(); y = 0; }
            var recipe = recipes.get(descriptor);
            if (recipe != null) {
                pages.add(new BookPage(List.of(), recipe));
                continue;
            }
            for (var part : ResearchText.parse(Component.translatable(descriptor.textKey()).getString())) {
                if (part instanceof ResearchText.Text text) {
                    for (var line : font.split(Component.literal(text.value()), textWidth)) {
                        if (y + 10 > textHeight) { pages.add(new BookPage(List.copyOf(current), null)); current.clear(); y = 0; }
                        current.add(new Draw(line, null, y, 0, 10)); y += 10;
                    }
                } else if (part instanceof ResearchText.Image image) {
                    float scale = Math.min(image.scale(), Math.min((float)textWidth / image.width(), (float)(textHeight - 4) / image.height()));
                    int w = Math.max(1, (int)(image.width() * scale)), h = Math.max(1, (int)(image.height() * scale));
                    if (y + h + 4 > textHeight) { pages.add(new BookPage(List.copyOf(current), null)); current.clear(); y = 0; }
                    current.add(new Draw(null, image, y, w, h)); y += h + 4;
                } else {
                    if (y + 8 > textHeight) { pages.add(new BookPage(List.copyOf(current), null)); current.clear(); y = 0; }
                    current.add(new Draw(null, null, y + 3, textWidth, 8)); y += 8;
                }
            }
        }
        if (!current.isEmpty()) pages.add(new BookPage(List.copyOf(current), null));
    }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        refreshPages(false); // Also removes unavailable recipes/gated pages while the book stays open.
        super.renderBackground(graphics, mouseX, mouseY, delta);
        // Original GuiResearchRecipe draws the book sheet and the page icons
        // with the ordinary alpha blend of the 1.7.10 GUI. The plain 1.21.1
        // texture blit neither enables nor disables blending, and the book
        // sheet has a translucent edge (11419 of 184320 pixels of the drawn
        // region) while the page icons are the translucent aspect glyphs, so
        // without the blend both are drawn as solid black. The blend is
        // switched on again before every icon, because the text of the page
        // runs through the font render type, whose teardown leaves the blend
        // off.
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        try {
            graphics.blit(BOOK, left, top, bookWidth, bookHeight, 0, 0, 512, 360, 512, 512);
            ThaumcraftRecipeDisplay.Hover hover = null;
            for (int side = 0; side < 2; side++) {
                int x = left + bookWidth * (side == 0 ? 26 : 273) / 512;
                int index = page + side;
                if (index >= pages.size()) continue;
                graphics.drawString(font, font.substrByWidth(title, textWidth).getString(), x, top + 14, 0xFF43301F, false);
                BookPage current = pages.get(index);
                if (current.recipe != null && minecraft.level != null) {
                    var recipeHover = current.recipe.render(graphics, font, minecraft.level, minecraft.player,
                            x, top + 32, textWidth, textHeight, mouseX, mouseY, net.minecraft.Util.getMillis() / 1000L);
                    if (recipeHover != null) hover = recipeHover;
                }
                for (Draw draw : current.text) {
                    int y = top + 32 + draw.y;
                    if (draw.text != null) graphics.drawString(font, draw.text, x, y, 0xFF30251B, false);
                    else if (draw.image != null) {
                        var image = draw.image;
                        RenderSystem.enableBlend();
                        RenderSystem.defaultBlendFunc();
                        graphics.blit(ResourceLocation.parse(image.texture()), x + (textWidth - draw.width) / 2, y,
                                draw.width, draw.height, image.u(), image.v(), image.width(), image.height(), 256, 256);
                    } else graphics.hLine(x + 6, x + textWidth - 6, y, 0xFF927A54);
                }
            }
            super.render(graphics, mouseX, mouseY, delta);
            if (hover != null) hover.render(graphics, font, mouseX, mouseY);
        } finally {
            RenderSystem.disableBlend();
        }
    }
    // Screen.render draws widgets after calling this. Do not blur the pages a
    // second time: their explicit background pass is at the start of render.
    @Override public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float delta) {}
    @Override public void onClose() { minecraft.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }
}
