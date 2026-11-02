package thaumcraft.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
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
    private final TransferButton[] transferButtons = new TransferButton[2];
    private int requestedDescriptor = -1;
    private List<RecipeItemLinks.Hitbox> itemLinks = List.of();
    private int renderedPage = -1, renderedWidth, renderedHeight;
    private net.minecraft.world.level.Level renderedLevel;
    private record Draw(FormattedCharSequence text, ResearchText.Image image, int y, int width, int height) {}
    private record BookPage(List<Draw> text, ThaumcraftRecipeDisplay recipe, int descriptor) {}
    private record TransferChoice(int pageIndex, long cycle, net.minecraft.world.item.crafting.RecipeHolder<?> holder,
                                  ResourceLocation id) {}

    public GuiResearchRecipe(Screen parent, ResearchDefinition research) { this(parent, research, 0); }
    /** Opens the research on the spread that holds {@code startPage}; the reopened page of the book. */
    public GuiResearchRecipe(Screen parent, ResearchDefinition research, int startPage) {
        super(Component.translatable(research.nameKey())); this.parent = parent; this.research = research;
        page = Math.max(0, startPage) / 2 * 2;
    }
    @Override protected void init() {
        // Oracle GuiResearchRecipe:83-84,216-227: fixed 256x181 sheet at scale 1.3.
        // Text is NOT inside that scaled pose (:231-235,1236).
        bookWidth = Math.round(256 * 1.3F); bookHeight = Math.round(181 * 1.3F);
        left = (width - bookWidth) / 2; top = (height - bookHeight) / 2;
        textWidth = 139; textHeight = 181;
        refreshPages(true);
        int sw = (width - 256) / 2, sh = (height - 181) / 2;
        // Oracle :242-257: original arrow positions and artwork, not a vanilla button row.
        previous = addRenderableWidget(new BookNavigation(sw - 17, sh + 189, 14, 10,
                Component.literal("←"), b -> { page -= 2; updateButtons(); }, 0, 184, 12, 8, 1, 1));
        next = addRenderableWidget(new BookNavigation(sw + 261, sh + 189, 14, 10,
                Component.literal("→"), b -> { page += 2; updateButtons(); }, 12, 184, 12, 8, 1, 1));
        // Native parent-screen Back retains its existing action using the original history arrow.
        addRenderableWidget(new BookNavigation(sw + 118, sh + 189, 20, 12,
                Component.translatable("gui.back"), b -> onClose(), 38, 202, 20, 12, 0, 0));
        // The port's own bookmark ribbon on the top right corner of the book (ThaumcraftTweaks.bookmarks).
        if (thaumcraft.common.config.ThaumcraftTweaks.bookmarks()) addRenderableWidget(new BookmarkRibbon(left + bookWidth - 34, top - 4));
        for (int side = 0; side < transferButtons.length; side++) transferButtons[side] = addRenderableWidget(new TransferButton(side));
        updateButtons();
    }

    private final class TransferButton extends Button {
        private TransferChoice choice;
        private TransferButton(int side) {
            super(0, 0, 12, 12, Component.translatable("tc.recipe_transfer.fill"), button -> transferVisibleRecipe(side), DEFAULT_NARRATION);
            visible = false;
            active = false;
            setTooltip(Tooltip.create(Component.translatable("tc.recipe_transfer.fill").append("\n")
                    .append(Component.translatable("tc.recipe_transfer.max"))));
        }
        private void show(int x, int y, TransferChoice choice, boolean known) {
            setX(x); setY(y); this.choice = choice; visible = true; active = known;
        }
        private void hide() { choice = null; visible = false; active = false; }
        @Override protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
            int x = getX(), y = getY();
            graphics.fill(x, y, x + width, y + height, 0xff6b5130);
            graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1,
                    !active ? 0xffa1957d : isHoveredOrFocused() ? 0xfff0deb7 : 0xffdfc89d);
            graphics.drawCenteredString(font, Component.literal("+"), x + width / 2, y + 2,
                    active ? 0xff3e3728 : 0xff6c6658);
        }
    }

    private void transferVisibleRecipe(int side) {
        TransferChoice choice = transferButtons[side].choice;
        refreshPages(false);
        if (choice == null || minecraft.player == null || minecraft.level == null || minecraft.screen != this
                || renderedLevel != minecraft.level || renderedPage != page || renderedWidth != width || renderedHeight != height
                || choice.pageIndex != page + side || choice.pageIndex >= pages.size()
                || !KnowledgeAccess.get(minecraft.player).getResearchComplete().contains(research.key())) return;
        var display = pages.get(choice.pageIndex).recipe;
        if (display == null || display.selectedRecipe(choice.cycle) != choice.holder) return;
        // Use the exact variant that was actually rendered, not the next second's array entry.
        if (RecipeTransferClient.recipeId(display, choice.cycle).filter(choice.id::equals).isEmpty()) return;
        RecipeTransferClient.request(choice.id, hasShiftDown());
    }

    /**
     * The port's own keys (ThaumcraftTweaks.bookKeyboard): the arrow and page keys turn the spreads,
     * Home and End go to the first and the last one, Backspace goes back to the map. The inventory key
     * goes back to the map as in TC4 (oracle GuiResearchRecipe.keyTyped), or with the reopen setting
     * closes the book on this page.
     */
    @Override public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (thaumcraft.common.config.ThaumcraftTweaks.bookKeyboard()) {
            int last = Math.max(0, (pages.size() - 1) / 2 * 2);
            switch (key) {
                case org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT, org.lwjgl.glfw.GLFW.GLFW_KEY_PAGE_UP -> { turnTo(page - 2, last); return true; }
                case org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT, org.lwjgl.glfw.GLFW.GLFW_KEY_PAGE_DOWN -> { turnTo(page + 2, last); return true; }
                case org.lwjgl.glfw.GLFW.GLFW_KEY_HOME -> { turnTo(0, last); return true; }
                case org.lwjgl.glfw.GLFW.GLFW_KEY_END -> { turnTo(last, last); return true; }
                case org.lwjgl.glfw.GLFW.GLFW_KEY_BACKSPACE -> { onClose(); return true; }
                default -> { }
            }
        }
        if (minecraft.options.keyInventory.matches(key, scanCode)) {
            if (thaumcraft.common.config.ThaumcraftTweaks.rememberPage()) {
                thaumcraft.client.lib.ResearchBookState.rememberPage(research.key(), page);
                minecraft.setScreen(null);
            } else onClose();
            return true;
        }
        return super.keyPressed(key, scanCode, modifiers);
    }

    private void turnTo(int target, int last) {
        if (target < 0 || target > last || target == page) return;
        page = target;
        updateButtons();
    }

    /** A ribbon in the corner of the book: red while the research is bookmarked, a faded outline otherwise. */
    private final class BookmarkRibbon extends Button {
        private BookmarkRibbon(int x, int y) {
            super(x, y, 9, 16, Component.translatable("tc.port.bookmark.add"), button -> {
                thaumcraft.client.lib.ResearchBookState.bookmarks().toggle(research.key());
            }, DEFAULT_NARRATION);
        }
        @Override protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
            var bookmarks = thaumcraft.client.lib.ResearchBookState.bookmarks();
            boolean marked = bookmarks.contains(research.key());
            int body = marked ? 0xFFA0201C : isHovered() ? 0xC06A4A2A : 0x806A4A2A;
            int x = getX(), y = getY();
            graphics.fill(x, y, x + 9, y + 11, body);
            // The forked end of a ribbon.
            for (int row = 0; row < 5; row++) {
                graphics.fill(x, y + 11 + row, x + 4 - row, y + 12 + row, body);
                graphics.fill(x + 5 + row, y + 11 + row, x + 9, y + 12 + row, body);
            }
            if (isHovered()) {
                Component tip = Component.translatable(marked ? "tc.port.bookmark.remove" : "tc.port.bookmark.add");
                if (!marked && bookmarks.full()) tip = tip.copy().append(" (" + thaumcraft.client.lib.ResearchBookState.MAX_BOOKMARKS + ")");
                graphics.renderTooltip(font, tip, mouseX, mouseY);
            }
        }
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
        itemLinks = List.of();
        renderedPage = -1;
        if (requestedDescriptor >= 0) {
            int spread = spreadForDescriptor(pages.stream().map(BookPage::descriptor).toList(), requestedDescriptor);
            if (spread >= 0) page = spread;
            requestedDescriptor = -1;
        }
        page = Math.max(0, Math.min(page, Math.max(0, (pages.size() - 1) / 2 * 2)));
        if (previous != null && next != null) updateButtons();
    }
    private void layoutPages() {
        pages.clear();
        List<Draw> current = new ArrayList<>(); int y = 0, pagesDescriptor = -1;
        for (var descriptor : descriptors) {
            int ordinal = research.pages().indexOf(descriptor);
            if (!current.isEmpty()) { pages.add(new BookPage(List.copyOf(current), null, pagesDescriptor)); current.clear(); y = 0; }
            pagesDescriptor = ordinal;
            var recipe = recipes.get(descriptor);
            if (recipe != null) {
                pages.add(new BookPage(List.of(), recipe, ordinal));
                continue;
            }
            for (var part : ResearchText.parse(Component.translatable(descriptor.textKey()).getString())) {
                if (part instanceof ResearchText.Text text) {
                    for (var line : font.split(ResearchText.formatted(text.value()), textWidth)) {
                        if (y + font.lineHeight > textHeight) { pages.add(new BookPage(List.copyOf(current), null, ordinal)); current.clear(); y = 0; }
                        current.add(new Draw(line, null, y, 0, font.lineHeight)); y += font.lineHeight;
                    }
                } else if (part instanceof ResearchText.Image image) {
                    float scale = Math.min(image.scale(), Math.min((float)textWidth / image.width(), (float)(textHeight - 4) / image.height()));
                    int w = Math.max(1, (int)(image.width() * scale)), h = Math.max(1, (int)(image.height() * scale));
                    if (y + h + 4 > textHeight) { pages.add(new BookPage(List.copyOf(current), null, ordinal)); current.clear(); y = 0; }
                    current.add(new Draw(null, image, y, w, h)); y += h + 4;
                } else {
                    if (y + 8 > textHeight) { pages.add(new BookPage(List.copyOf(current), null, ordinal)); current.clear(); y = 0; }
                    current.add(new Draw(null, null, y, 96, 4)); y += font.lineHeight;
                }
            }
        }
        if (!current.isEmpty()) pages.add(new BookPage(List.copyOf(current), null, pagesDescriptor));
    }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        refreshPages(false); // Also removes unavailable recipes/gated pages while the book stays open.
        for (TransferButton button : transferButtons) if (button != null) button.hide();
        itemLinks = List.of();
        renderedPage = -1;
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
        try (RecipeItemLinks links = RecipeItemLinks.capture()) {
            graphics.pose().pushPose();
            graphics.pose().translate((width - 256 * 1.3F) / 2, (height - 181 * 1.3F) / 2, 0);
            graphics.pose().scale(1.3F, 1.3F, 1);
            graphics.blit(BOOK, 0, 0, 0, 0, 256, 181, 256, 256);
            graphics.pose().popPose();
            int paneLeft = (width - 256) / 2, paneTop = (height - 181) / 2;
            ThaumcraftRecipeDisplay.Hover hover = null;
            long cycle = net.minecraft.Util.getMillis() / 1000L;
            boolean knownResearch = minecraft.player != null
                    && KnowledgeAccess.get(minecraft.player).getResearchComplete().contains(research.key());
            for (int side = 0; side < 2; side++) {
                int x = paneLeft - 15 + side * 152;
                int index = page + side;
                if (index >= pages.size()) continue;
                int bodyTop = paneTop - 10;
                if (page == 0 && side == 0) {
                    // Oracle :272-282: original textured rules and neutral-grey title.
                    rule(graphics, paneLeft + 4, paneTop - 13);
                    rule(graphics, paneLeft + 4, paneTop + 4);
                    int titleWidth = font.width(title);
                    float scale = titleWidth <= 130 ? 1 : 130F / titleWidth;
                    graphics.pose().pushPose();
                    graphics.pose().translate(paneLeft + 52 - titleWidth / 2F * scale, paneTop - 6 * scale, 0);
                    graphics.pose().scale(scale, scale, 1);
                    graphics.drawString(font, title, 0, 0, 0xFF303030, false);
                    graphics.pose().popPose();
                    bodyTop += 25;
                }
                BookPage current = pages.get(index);
                if (current.recipe != null && minecraft.level != null) {
                    var recipeHover = current.recipe.render(graphics, font, minecraft.level, minecraft.player,
                            x, bodyTop, textWidth, textHeight, mouseX, mouseY, cycle);
                    if (recipeHover != null) hover = recipeHover;
                    var transferId = RecipeTransferClient.recipeId(current.recipe, cycle);
                    if (transferId.isPresent()) transferButtons[side].show(x + textWidth - 14, bodyTop + 14,
                            new TransferChoice(index, cycle, current.recipe.selectedRecipe(cycle), transferId.get()), knownResearch);
                }
                for (Draw draw : current.text) {
                    int y = bodyTop + draw.y;
                    if (draw.text != null) graphics.drawString(font, draw.text, x, y, 0xFF505050, false);
                    else if (draw.image != null) {
                        var image = draw.image;
                        RenderSystem.enableBlend();
                        RenderSystem.defaultBlendFunc();
                        graphics.blit(ResourceLocation.parse(image.texture()), x + (textWidth - draw.width) / 2, y,
                                draw.width, draw.height, image.u(), image.v(), image.width(), image.height(), 256, 256);
                    } else rule(graphics, x + textWidth / 2 - 48, y + 2);
                }
            }
            super.render(graphics, mouseX, mouseY, delta);
            itemLinks = links.items();
            renderedPage = page; renderedWidth = width; renderedHeight = height; renderedLevel = minecraft.level;
            if (hover != null) {
                if (!hover.stack().isEmpty() && itemLinks.stream().anyMatch(item -> item.contains(mouseX, mouseY))
                        && reference(hover.stack()) != null) {
                    hover = new ThaumcraftRecipeDisplay.Hover(hover.stack(), List.of(
                            Component.translatable("recipe.clickthrough").withStyle(net.minecraft.ChatFormatting.DARK_GRAY,
                                    net.minecraft.ChatFormatting.ITALIC)));
                }
                hover.render(graphics, font, mouseX, mouseY);
            }
        } finally {
            RenderSystem.disableBlend();
        }
    }

    /** API page indices refer to source descriptors; long text and concealed pages alter the rendered page numbers. */
    static int spreadForDescriptor(List<Integer> layout, int descriptor) {
        int index = layout.indexOf(descriptor);
        return index < 0 ? -1 : index / 2 * 2;
    }

    static GuiResearchRecipe followReference(GuiResearchRecipe current, ResearchDefinition target, int descriptor,
                                              Set<String> completed) {
        if (!mayFollow(current, target, descriptor, completed)) return null;
        GuiResearchRecipe next = new GuiResearchRecipe(current, target);
        next.requestedDescriptor = descriptor;
        return next;
    }

    private static boolean mayFollow(GuiResearchRecipe current, ResearchDefinition target, int descriptor,
                                     Set<String> completed) {
        return target != null && !target.key().equals(current.research.key()) && completed.contains(target.key())
                && ResearchBrowserData.isVisible(target, completed) && descriptor >= 0 && descriptor < target.pages().size()
                && target.pages().get(descriptor).isVisible(completed);
    }

    private Object[] reference(net.minecraft.world.item.ItemStack stack) {
        if (minecraft.player == null || stack.isEmpty()) return null;
        Object[] reference = thaumcraft.api.ThaumcraftApi.getCraftingRecipeKey(minecraft.player, stack);
        if (reference == null || reference.length != 2 || !(reference[0] instanceof String key)
                || !(reference[1] instanceof Integer descriptor)) return null;
        return mayFollow(this, thaumcraft.common.lib.research.ResearchCatalog.get(key), descriptor,
                KnowledgeAccess.get(minecraft.player).getResearchComplete()) ? reference : null;
    }

    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        refreshPages(false);
        if (super.mouseClicked(mouseX, mouseY, button)) return true;
        if (button != 0 || minecraft.player == null || minecraft.screen != this || renderedLevel != minecraft.level
                || renderedPage != page || renderedWidth != width || renderedHeight != height) return false;
        Set<String> complete = KnowledgeAccess.get(minecraft.player).getResearchComplete();
        if (!complete.contains(research.key())) return false;
        for (RecipeItemLinks.Hitbox item : itemLinks) {
            if (!item.contains(mouseX, mouseY)) continue;
            Object[] reference = reference(item.stack());
            if (reference == null) return false;
            var target = thaumcraft.common.lib.research.ResearchCatalog.get((String) reference[0]);
            if (target == null || ThaumcraftRecipeDisplay.resolve(target.pages().get((Integer) reference[1]),
                    target.key(), minecraft.level) == null) return false;
            GuiResearchRecipe next = followReference(this, target, (Integer) reference[1], complete);
            if (next == null) return false;
            minecraft.setScreen(next);
            return true;
        }
        return false;
    }
    /** Original TCFontRenderer.renderSplitString: <LINE/> blits (24,184), 96x4. */
    private static void rule(GuiGraphics graphics, int x, int y) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        graphics.blit(BOOK, x, y, 24, 184, 96, 4, 256, 256);
    }
    private static final class BookNavigation extends Button {
        private final int u, v, artWidth, artHeight, dx, dy;
        private BookNavigation(int x, int y, int width, int height, Component title, OnPress action,
                               int u, int v, int artWidth, int artHeight, int dx, int dy) {
            super(x, y, width, height, title, action, DEFAULT_NARRATION);
            this.u = u; this.v = v; this.artWidth = artWidth; this.artHeight = artHeight;
            this.dx = dx; this.dy = dy;
        }
        @Override protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
            if (!active) return;
            var player = net.minecraft.client.Minecraft.getInstance().player;
            float bob = player == null ? .1F : (float)Math.sin(player.tickCount / 3F) * .2F + .1F;
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            // Oracle :1287-1298: scale around the sprite center, not the hit box.
            graphics.pose().pushPose();
            graphics.pose().translate(getX() + dx + artWidth / 2F, getY() + dy + artHeight / 2F, 0);
            graphics.pose().scale(1 + bob, 1 + bob, 1);
            graphics.blit(BOOK, -artWidth / 2, -artHeight / 2, u, v, artWidth, artHeight, 256, 256);
            graphics.pose().popPose();
        }
    }
    // Screen.render draws widgets after calling this. Do not blur the pages a
    // second time: their explicit background pass is at the start of render.
    @Override public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float delta) {}
    @Override public void onClose() {
        // Back through recipe references, then to the map; the next opening shows the map.
        thaumcraft.client.lib.ResearchBookState.forgetPage();
        minecraft.setScreen(backDestination(minecraft.player == null ? Set.of()
                : KnowledgeAccess.get(minecraft.player).getResearchComplete()));
    }
    Screen backDestination(Set<String> completed) {
        Screen destination = parent;
        while (destination instanceof GuiResearchRecipe previous && !completed.contains(previous.research.key())) {
            destination = previous.parent;
        }
        return destination;
    }
    @Override public boolean isPauseScreen() { return false; }
}
