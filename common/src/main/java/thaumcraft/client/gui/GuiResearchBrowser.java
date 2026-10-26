package thaumcraft.client.gui;

import dev.architectury.networking.NetworkManager;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.items.ItemInkwell;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.network.ResearchPurchasePayload;
import thaumcraft.common.lib.network.ResearchNoteRequestPayload;
import thaumcraft.common.lib.research.ResearchBrowserData;
import thaumcraft.common.lib.research.ResearchBrowserConnectors;
import thaumcraft.common.lib.research.ResearchDefinition;
import thaumcraft.common.lib.research.ResearchPurchase;
import thaumcraft.common.lib.research.ResearchNoteIssuance;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/** Native research map: original coordinates, clue gates and completed entries. */
public final class GuiResearchBrowser extends Screen {
    private static final Pattern TEXTURE = Pattern.compile("new ResourceLocation\\(\"thaumcraft\",\\s*\"([^\"]+)\"\\)");
    private static final ResourceLocation FRAME = texture("textures/gui/gui_research.png");
    /** Original GuiResearchBrowser:290 - the background is a 112x98 window of the sheet. */
    private static final int ORIGINAL_BACKGROUND_WINDOW_WIDTH = 112;
    private static final int ORIGINAL_BACKGROUND_WINDOW_HEIGHT = 98;
    private final List<Node> nodes = new ArrayList<>();
    private final List<Tab> tabs = new ArrayList<>();
    private String category = thaumcraft.client.lib.ResearchNotifications.selectedCategory();
    private static final ResourceLocation HIGHLIGHT = texture("textures/misc/particles.png");
    /** The original forbidden marker is drawn from the node sheet. */
    private static final ResourceLocation NODES = texture("textures/misc/nodes.png");
    /** The original node sheet is 2048x2048 with thirty two frames and thirty two strips. */
    private static final int NODE_CELL = 64;
    private int left, top, panelWidth, panelHeight, graphLeft, graphTop, graphWidth, graphHeight;
    // Offset from the pane center. The oracle's guiMapX/Y are -(pan + half-pane).
    // Constructor :100-106 places column/row zero at (82,70) inside the 224x196 pane.
    private double panX = 82 - 112, panY = 70 - 98;
    private record Node(ResearchDefinition research, int x, int y) {}
    private record Tab(String key, int x, int y, ResourceLocation icon) {}
    // Settings adopted from TC4Tweaks (ThaumcraftTweaks): a search box over the completed research
    // and a counter of the completed research of the open category. TC4 has neither.
    private static final int RESULT_ROW = 11, RESULT_WIDTH = 150;
    private net.minecraft.client.gui.components.EditBox search;
    private String query = "";
    private ResearchSearch index;
    private List<ResearchSearch.Entry> results = List.of();
    private int resultScroll;

    public GuiResearchBrowser() {
        super(Component.translatable("item.thaumcraft.thaumonomicon"));
        // Start a fresh native book at remembered column/row (0,0), so its basics
        // entries are on-screen. The oracle's startup (-5,-6) is a different camera
        // view; queued notices and subsequent opens still restore their real center.
        if (thaumcraft.client.lib.ResearchNotifications.hasQueuedResearch()) {
            panX -= thaumcraft.client.lib.ResearchNotifications.lastColumn() * 24;
            panY -= thaumcraft.client.lib.ResearchNotifications.lastRow() * 24;
        }
    }
    /**
     * Opens the Thaumonomicon: on the research page it was closed on when the reopen setting is on and
     * that research is still completed (ResearchBookState), on the map otherwise.
     */
    public static Screen open() {
        var minecraft = net.minecraft.client.Minecraft.getInstance();
        String remembered = thaumcraft.client.lib.ResearchBookState.rememberedResearch();
        if (remembered != null && thaumcraft.common.config.ThaumcraftTweaks.rememberPage() && minecraft.player != null) {
            var research = thaumcraft.common.lib.research.ResearchCatalog.get(remembered);
            if (research != null && KnowledgeAccess.get(minecraft.player).isResearchComplete(remembered)) {
                return new GuiResearchRecipe(new GuiResearchBrowser(), research, thaumcraft.client.lib.ResearchBookState.rememberedPage());
            }
        }
        thaumcraft.client.lib.ResearchBookState.forgetPage();
        return new GuiResearchBrowser();
    }

    private Set<String> completed() {
        return minecraft.player == null ? Set.of() : KnowledgeAccess.get(minecraft.player).getResearchComplete();
    }
    private static ResourceLocation texture(String path) { return ResourceLocation.fromNamespaceAndPath("thaumcraft", path); }

    @Override protected void init() {
        // Oracle GuiResearchBrowser:75-76,272-275: no stretched replacement panel.
        panelWidth = 256; panelHeight = 230;
        left = (width - panelWidth) / 2; top = (height - panelHeight) / 2;
        graphLeft = left + 16; graphTop = top + 17;
        graphWidth = 224; graphHeight = 196;
        tabs.clear();
        var categories = ResearchBrowserData.categories(completed());
        if (!categories.contains(category)) category = "BASICS";
        int count = 0;
        for (String key : categories) {
            // Original GuiResearchBrowser:460-509 draws one tab per visible
            // category, 24 pixels apart, each with the icon of its category.
            tabs.add(new Tab(key, count < 9 ? left - 24 : left + 256,
                    top + (count % 9) * 24, categoryIcon(key)));
            count++;
        }
        search = null;
        if (thaumcraft.common.config.ThaumcraftTweaks.researchSearch()) {
            // On the carved top border, right half, like TC4Tweaks; it survives a category change.
            search = new net.minecraft.client.gui.components.EditBox(font, left + 134, top + 5, 104, 10,
                    Component.translatable("tc.port.search.hint"));
            search.setBordered(false);
            search.setMaxLength(40);
            search.setHint(Component.translatable("tc.port.search.hint").withStyle(ChatFormatting.DARK_GRAY));
            search.setValue(query);
            search.setResponder(this::searchFor);
            addRenderableWidget(search);
        }
    }

    private void searchFor(String text) {
        query = text;
        resultScroll = 0;
        if (text.isBlank()) { results = List.of(); return; }
        if (index == null) index = ResearchSearch.index(completed(), minecraft.level);
        results = index.find(text);
    }

    private int resultRows() { return Math.min(results.size() - resultScroll, (graphHeight - 4) / RESULT_ROW); }
    private int resultLeft() { return graphLeft + graphWidth - RESULT_WIDTH - 2; }
    private boolean inResults(double x, double y) {
        return !results.isEmpty() && x >= resultLeft() && x < resultLeft() + RESULT_WIDTH
                && y >= graphTop + 2 && y < graphTop + 2 + Math.max(1, resultRows()) * RESULT_ROW;
    }
    private ResearchSearch.Entry resultAt(double x, double y) {
        if (!inResults(x, y)) return null;
        int row = (int) ((y - graphTop - 2) / RESULT_ROW) + resultScroll;
        return row >= 0 && row < results.size() ? results.get(row) : null;
    }

    /** The search results over the map: the research name, or the item and its research. */
    private void renderResults(GuiGraphics graphics, int mouseX, int mouseY) {
        if (search == null || query.isBlank()) return;
        // Research icons are items drawn with depth; the list must sit above them.
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 400);
        try { renderResultRows(graphics, mouseX, mouseY); } finally { graphics.pose().popPose(); }
    }

    private void renderResultRows(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = resultLeft(), y = graphTop + 2, rows = Math.max(1, resultRows());
        graphics.fill(x - 1, y - 1, x + RESULT_WIDTH + 1, y + rows * RESULT_ROW + 1, 0xFF5A3A1E);
        graphics.fill(x, y, x + RESULT_WIDTH, y + rows * RESULT_ROW, 0xE0140C06);
        if (results.isEmpty()) {
            graphics.drawString(font, Component.translatable("tc.port.search.none"), x + 3, y + 2, 0x909090, false);
            return;
        }
        for (int row = 0; row < resultRows(); row++) {
            var entry = results.get(row + resultScroll);
            int rowY = y + row * RESULT_ROW;
            if (mouseX >= x && mouseX < x + RESULT_WIDTH && mouseY >= rowY && mouseY < rowY + RESULT_ROW)
                graphics.fill(x, rowY, x + RESULT_WIDTH, rowY + RESULT_ROW, 0x60FFFFFF);
            int textX = x + 3;
            if (!entry.item().isEmpty()) {
                graphics.pose().pushPose();
                graphics.pose().translate(x + 2, rowY + 1, 0);
                graphics.pose().scale(.5625F, .5625F, 1);
                graphics.renderItem(entry.item(), 0, 0);
                graphics.pose().popPose();
                textX += 11;
            }
            int color = entry.item().isEmpty() ? 0xFFE0C080 : 0xFFD0D0D0;
            graphics.drawString(font, font.plainSubstrByWidth(entry.text(), x + RESULT_WIDTH - textX - 2), textX, rowY + 2, color, false);
        }
    }

    /**
     * The completed research of the open category. While research of it is still hidden the count
     * is of the research the player can see, marked with a red question mark, so nothing is revealed.
     */
    private void renderCounter(GuiGraphics graphics, Set<String> completed, int shown, int mouseX, int mouseY) {
        if (!thaumcraft.common.config.ThaumcraftTweaks.researchCounter()) return;
        var all = ResearchBrowserData.categoryEntries(category).stream()
                .filter(research -> !research.isVirtual() && research.isEnabled()).toList();
        long done = all.stream().filter(research -> completed.contains(research.key())).count();
        boolean partial = shown < all.size();
        Component text = Component.literal(done + "/" + (partial ? shown : all.size()))
                .append(partial ? Component.literal("?").withStyle(ChatFormatting.DARK_RED) : Component.empty());
        int x = left + 18, y = top + 5;
        graphics.drawString(font, text, x, y, 0xC8B89A, true);
        if (mouseX >= x && mouseX < x + font.width(text) && mouseY >= y && mouseY < y + font.lineHeight)
            graphics.renderTooltip(font, Component.translatable(partial ? "tc.port.counter.partial" : "tc.port.counter.all"), mouseX, mouseY);
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.renderBackground(graphics, mouseX, mouseY, delta);
        var completed = completed();
        var entries = ResearchBrowserData.entries(category, completed);
        nodes.clear();
        int[] bounds = mapBounds();
        int mapX = Math.clamp((int)(-panX - graphWidth / 2), bounds[0], bounds[2] - 1);
        int mapY = Math.clamp((int)(-panY - graphHeight / 2), bounds[1], bounds[3] - 1);
        panX = -mapX - graphWidth / 2;
        panY = -mapY - graphHeight / 2;
        int originX = graphLeft - mapX;
        int originY = graphTop - mapY;
        for (var research : entries) nodes.add(new Node(research, originX + research.column() * 24, originY + research.row() * 24));
        graphics.enableScissor(graphLeft, graphTop, graphLeft + graphWidth, graphTop + graphHeight);
        // Oracle :277-292: one normalized, panning window, under scale(2,2,1).
        drawCategoryBackground(graphics, mapX, mapY, bounds);
        drawConnectors(graphics, completed, originX, originY, delta);
        for (Node node : nodes) {
            boolean unlocked = completed.contains(node.research.key());
            boolean available = node.research.hasPrerequisites(completed);
            int frameX = badgeFrame(node.research, purchases(node.research));
            // Original GuiResearchBrowser:366,789-802 - a research with warp above zero shows the
            // forbidden marker: the animated tainted strip of the node sheet, eighty pixels wide,
            // in the original dark violet 0x440055 with the alpha 0.66.
            forbidden(graphics, node.research, node.x + 11, node.y + 11);
            blend();
            graphics.blit(FRAME, node.x - 2, node.y - 2, frameX, 230, 26, 26, 256, 256);
            // Original GuiResearchBrowser:400-402 draws the corner brackets of a
            // special research over the cell of the research.
            if (node.research.flags().contains("Special")) {
                graphics.blit(FRAME, node.x - 2, node.y - 2, 26, 230, 26, 26, 256, 256);
            }
            if (thaumcraft.client.lib.ResearchNotifications.highlighted(node.research.key())) highlight(graphics, node.x - 5, node.y - 5);
            drawIcon(graphics, node.research, node.x + 3, node.y + 3);
            if (!available && !unlocked) graphics.fill(node.x, node.y, node.x + 22, node.y + 22, 0x99000000);
        }
        graphics.disableScissor();
        renderTabs(graphics);
        // Oracle GuiResearchBrowser:511-513 draws the original carved frame LAST,
        // covering the cropped graph and the inner ends of the category tabs.
        blend();
        graphics.blit(FRAME, left, top, 0, 0, 256, 230, 256, 256);
        if (search != null) graphics.fill(search.getX() - 3, search.getY() - 2, search.getX() + search.getWidth() + 1,
                search.getY() + search.getHeight(), 0x90000000);
        renderResults(graphics, mouseX, mouseY);
        renderBookmarks(graphics, mouseX, mouseY);
        super.render(graphics, mouseX, mouseY, delta);
        renderCounter(graphics, completed, entries.size(), mouseX, mouseY);
        Node hover = nodeAt(mouseX, mouseY);
        Tab hovered = tabAt(mouseX, mouseY);
        if (hovered != null) {
            graphics.renderTooltip(font, Component.translatable("tc.research_category." + hovered.key()), mouseX, mouseY);
        } else if (hover != null) {
            int tooltipWidth = Math.max(120, Math.min(300, width - 24));
            var lines = tooltip(hover.research).stream()
                    .flatMap(line -> font.split(line, tooltipWidth).stream()).toList();
            graphics.renderTooltip(font, lines, mouseX, mouseY);
        }
    }

    /**
     * Original GuiResearchBrowser:460-509: the tab of a category is its frame, the
     * highlight, the icon of the category and, for every tab but the selected one,
     * the dim overlay. The unselected shape and its icon sit eight pixels to the
     * right, exactly as the original draws them.
     */
    private void renderTabs(GuiGraphics graphics) {
        for (Tab tab : tabs) {
            blend();
            boolean selected = tab.key().equals(category);
            boolean right = tab.x() > left;
            int shift = right ? (selected ? 14 : 6) : (selected ? 0 : 8);
            int iconX = right ? left + 245 + shift : tab.x() + 5 + shift;
            // Oracle :481-485 mirrors the right-hand tab. PoseStack replaces GL11.
            graphics.pose().pushPose();
            graphics.pose().translate(tab.x() + (right ? 24 : 0), tab.y(), 0);
            graphics.pose().scale(right ? -1 : 1, 1, 1);
            graphics.blit(FRAME, 0, 0, selected ? 152 : 176, 232, 24, 24, 256, 256);
            graphics.pose().popPose();
            if (thaumcraft.client.lib.ResearchNotifications.highlighted(tab.key())) {
                highlight(graphics, iconX - 8, tab.y() - 4);
            }
            ResourceLocation icon = tab.icon();
            if (icon != null) graphics.blit(icon, iconX, tab.y() + 4, 16, 16, 0, 0, 32, 32, 32, 32);
            if (!selected) {
                graphics.pose().pushPose();
                graphics.pose().translate(tab.x() + (right ? 24 : 0), tab.y(), 0);
                graphics.pose().scale(right ? -1 : 1, 1, 1);
                graphics.blit(FRAME, 0, 0, 200, 232, 24, 24, 256, 256);
                graphics.pose().popPose();
            }
        }
        com.mojang.blaze3d.systems.RenderSystem.disableBlend();
    }

    /**
     * Original GuiResearchBrowser:789-802 ({@code drawForbidden}): the animated quad strip of the
     * tainted node strip, eighty pixels wide, in the original colour 0x440055 with the alpha
     * {@code 0.66}. The frame runs backwards over the thirty two frames of the sheet.
     */
    private void forbidden(GuiGraphics graphics, ResearchDefinition research, int centreX, int centreY) {
        if (research.currentWarp() <= 0) return;
        int frames = 32, strip = 5, size = 80;
        int part = minecraft.player == null ? 0 : minecraft.player.tickCount % frames;
        int frame = frames - 1 - part;
        blend();
        graphics.setColor(0x44 / 255F, 0x00 / 255F, 0x55 / 255F, 168F / 255F);
        graphics.blit(NODES, centreX - size / 2, centreY - size / 2,
                frame * NODE_CELL, strip * NODE_CELL, size, size, 2048, 2048);
        graphics.setColor(1, 1, 1, 1);
    }

    /** Oracle GuiResearchBrowser:139-142: bounds over all entries, not only visible ones. */
    private int[] mapBounds() {
        var entries = ResearchBrowserData.categoryEntries(category);
        // ResearchCategoryList starts its extrema at zero; oracle browser:139-142.
        int minX = 0, minY = 0, maxX = 0, maxY = 0;
        for (var entry : entries) {
            minX = Math.min(minX, entry.column()); maxX = Math.max(maxX, entry.column());
            minY = Math.min(minY, entry.row()); maxY = Math.max(maxY, entry.row());
        }
        return new int[] {minX * 24 - 85, minY * 24 - 112, maxX * 24 - 112, maxY * 24 - 61};
    }

    private void drawCategoryBackground(GuiGraphics graphics, int mapX, int mapY, int[] bounds) {
        ResourceLocation sheet = texture("textures/gui/"
                + (category.equals("ELDRITCH") ? "gui_researchbackeldritch.png" : "gui_researchback.png"));
        // Oracle :287-291: normalized pan, NOT raw pixel pan and NOT wrapping.
        int vx = backgroundOffset(mapX, bounds[0], bounds[2], 288);
        int vy = backgroundOffset(mapY, bounds[1], bounds[3], 316);
        // 1.7's drawTexturedModalRect always used 256 as the UV denominator, even
        // for this 512px sheet. Preserve that geometry rather than correcting it.
        graphics.pose().pushPose();
        graphics.pose().scale(2, 2, 1);
        graphics.blit(sheet, graphLeft / 2, graphTop / 2, vx / 2, vy / 2,
                ORIGINAL_BACKGROUND_WINDOW_WIDTH, ORIGINAL_BACKGROUND_WINDOW_HEIGHT, 256, 256);
        graphics.pose().popPose();
    }

    static int backgroundOffset(int map, int min, int max, int extent) {
        return (int)((float)(map - min) / Math.abs(min - max) * extent);
    }

    private void drawConnectors(GuiGraphics graphics, Set<String> completed, int originX, int originY, float partialTick) {        Map<String, int[]> positions = new LinkedHashMap<>();
        for (var research : ResearchBrowserData.categoryEntries(category)) {
            positions.put(research.key(), new int[] {originX + research.column() * 24, originY + research.row() * 24});
        }
        blend();
        for (var connector : ResearchBrowserConnectors.connectors(category, completed)) {
            int[] from = positions.get(connector.from());
            int[] to = positions.get(connector.to());
            if (from == null || to == null) continue;
            connectorLine(graphics, from[0] + 11, from[1] + 11, to[0] + 11, to[1] + 11, connector, partialTick);
        }
    }

    /**
     * Original GuiResearchBrowser:732-787 ({@code drawLine}): a polyline of {@code dist / 2}
     * segments whose width is three pixels, whose colour fades to black along the line and whose
     * vertices wobble with the ticks of the player.
     */
    private void connectorLine(GuiGraphics graphics, float x, float y, float x2, float y2,
                                ResearchBrowserConnectors.Connector connector, float partialTick) {
        float dx = x - x2, dy = y - y2;
        int segments = ResearchBrowserConnectors.segments(dx, dy);
        if (segments <= 0) return;
        // Oracle GuiResearchBrowser:733 - the phase of the wobble is the age of the
        // player in ticks plus the partial tick of the frame.
        float count = (minecraft.player == null ? 0 : minecraft.player.tickCount) + partialTick;
        float stepX = dx / segments, stepY = dy / segments;
        boolean horizontal = Math.abs(dx) > Math.abs(dy);
        if (horizontal) stepX *= 2.0F; else stepY *= 2.0F;
        // Oracle :751-754: GL line width is THREE PHYSICAL pixels, not three GUI
        // units. Native DEBUG_LINE_STRIP preserves the original strip and vertex
        // gradients; filled staircase rectangles made these connectors enormous.
        graphics.flush();
        blend();
        com.mojang.blaze3d.systems.RenderSystem.setShader(net.minecraft.client.renderer.GameRenderer::getPositionColorShader);
        com.mojang.blaze3d.systems.RenderSystem.lineWidth(3);
        org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL11.GL_LINE_SMOOTH);
        var buffer = com.mojang.blaze3d.vertex.Tesselator.getInstance().begin(
                com.mojang.blaze3d.vertex.VertexFormat.Mode.DEBUG_LINE_STRIP,
                com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION_COLOR);
        for (int step = 0; step <= segments; step++) {
            float phase = ResearchBrowserConnectors.phase(step, segments);
            float red = connector.red(), green = connector.green(), blue = connector.blue();
            float alpha = ResearchBrowserConnectors.VERTEX_ALPHA;
            float vertexX = x, vertexY = y;
            if (connector.wiggle()) {
                float wobbleX = ResearchBrowserConnectors.wobbleX(count, step, segments);
                float wobbleY = ResearchBrowserConnectors.wobbleY(count, step, segments);
                float fade = ResearchBrowserConnectors.fade(step, segments);
                red *= fade; green *= fade; blue *= fade;
                alpha *= phase;
                vertexX = x - stepX * step + wobbleX;
                vertexY = y - stepY * step + wobbleY;
            } else {
                vertexX = x - stepX * step;
                vertexY = y - stepY * step;
            }
            buffer.addVertex(graphics.pose().last().pose(), vertexX, vertexY, 0).setColor(red, green, blue, alpha);
            if (horizontal) stepX *= 1.0F - 1.0F / (segments * 3.0F / 2.0F);
            else stepY *= 1.0F - 1.0F / (segments * 3.0F / 2.0F);
        }
        com.mojang.blaze3d.vertex.BufferUploader.drawWithShader(buffer.buildOrThrow());
        org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_LINE_SMOOTH);
        com.mojang.blaze3d.systems.RenderSystem.lineWidth(1);
    }

    /**
     * Original GuiResearchBrowser:456-458 and :385-386 switch blending on with the source alpha before
     * the tabs and the cell frames are drawn. {@code GuiGraphics#blit} does not, and a {@code fill} ends
     * with blending off, so without this the soft shadow of a cell frame and the translucent dim layer of
     * an unselected tab (texture 200,232: black, alpha 137 of 255 at most) are drawn as solid black.
     */
    private static void blend() {
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
    }

    /** The original category icon; the corpus keeps it apart from the entry icons. */
    private static ResourceLocation categoryIcon(String key) {
        var list = thaumcraft.api.research.ResearchCategories.getResearchList(key);
        return list == null ? null : list.icon;
    }

    /**
     * Original GuiResearchBrowser:387-399: the four frames of a research cell.
     * A round cell always uses 54, a hidden cell uses 230 when the research is
     * bought instead of written on a note and 86 otherwise, and a normal cell
     * uses 110 when it is bought and 0 otherwise.
     */
    public static int badgeFrame(ResearchDefinition research, boolean purchases) {
        if (research.flags().contains("Round")) return 54;
        if (research.isHidden()) return purchases ? 230 : 86;
        return purchases ? 110 : 0;
    }

    private static boolean purchases(ResearchDefinition research) {
        return thaumcraft.common.config.ThaumcraftSettings.current().purchases(research.isSecondary());
    }

    private List<Component> tooltip(ResearchDefinition research) {
        var lines = new ArrayList<Component>();
        lines.add(Component.translatable(research.nameKey()));
        lines.add(Component.translatable(research.textKey()).withStyle(ChatFormatting.GRAY));
        if (minecraft.player == null) return lines;
        var knowledge = KnowledgeAccess.get(minecraft.player);
        if (knowledge.isResearchComplete(research.key())) {
            lines.add(Component.translatable("tc.port.research.read").withStyle(ChatFormatting.GREEN));
            return lines;
        }
        if (!purchases(research)) {
            var preview = ResearchNoteIssuance.preview(minecraft.player, research.key());
            if (preview.success() && preview.reused()) {
                lines.add(Component.translatable("tc.port.note.action_existing").withStyle(ChatFormatting.YELLOW));
                return lines;
            }
            int paper = minecraft.player.getInventory().items.stream()
                    .filter(stack -> stack.is(Items.PAPER)).mapToInt(stack -> stack.getCount()).sum();
            long ink = minecraft.player.getInventory().items.stream().filter(ItemInkwell::isUsable).count();
            lines.add(Component.translatable("tc.port.note.materials").withStyle(ChatFormatting.GRAY));
            lines.add(Component.translatable("tc.port.note.paper", paper)
                    .withStyle(paper >= 1 ? ChatFormatting.GREEN : ChatFormatting.RED));
            lines.add(Component.translatable("tc.port.note.ink", ink)
                    .withStyle(ink >= 1 ? ChatFormatting.GREEN : ChatFormatting.RED));
            lines.add(preview.success()
                    ? Component.translatable("tc.port.note.request").withStyle(ChatFormatting.GREEN)
                    : noteFailureReason(preview.failure()).withStyle(ChatFormatting.RED));
            return lines;
        }
        var preview = ResearchPurchase.preview(knowledge, research.key());
        if (purchases(research) && !preview.costs().isEmpty()) {
            lines.add(Component.translatable("tc.port.research.cost_header").withStyle(ChatFormatting.GRAY));
            // Original GuiResearchBrowser:584 draws the price tags in the order of
            // AspectList.getAspectsSortedAmount: by decreasing amount, and in the
            // declared order for equal amounts. The name of the aspect is never a
            // tie breaker.
            AspectList tags = new AspectList();
            List<Map.Entry<String, Integer>> unresolved = new ArrayList<>();
            for (var cost : preview.costs().entrySet()) {
                Aspect aspect = Aspect.getAspect(cost.getKey());
                if (aspect == null) unresolved.add(cost);
                else tags.add(aspect, cost.getValue());
            }
            for (Aspect aspect : tags.getAspectsSortedAmount()) {
                String tag = aspect.getTag();
                int price = preview.costs().getOrDefault(tag, 0);
                int pool = knowledge.getAspectPoolFor(tag);
                if (!knowledge.hasDiscoveredAspect(tag)) {
                    // Original TC4 draws an anonymous icon without an amount for
                    // an undiscovered aspect. Do not reveal its name or price.
                    lines.add(Component.translatable("tc.port.research.cost_unknown").withStyle(ChatFormatting.DARK_GRAY));
                } else {
                    lines.add(Component.translatable("tc.port.research.cost", aspect.getName(), pool, price)
                            .withStyle(pool >= price ? ChatFormatting.GREEN : ChatFormatting.RED));
                }
            }
            for (var cost : unresolved) {
                lines.add(Component.translatable("tc.port.research.cost_unknown").withStyle(ChatFormatting.DARK_GRAY));
            }
        }
        lines.add(preview.success()
                ? Component.translatable("tc.port.research.buy").withStyle(ChatFormatting.GREEN)
                : failureReason(preview.failure()).withStyle(ChatFormatting.RED));
        return lines;
    }

    private static net.minecraft.network.chat.MutableComponent failureReason(ResearchPurchase.Failure failure) {
        return Component.translatable("tc.port.research.failure." + failure.name().toLowerCase(java.util.Locale.ROOT));
    }

    private static net.minecraft.network.chat.MutableComponent noteFailureReason(ResearchNoteIssuance.Failure failure) {
        return Component.translatable("tc.port.note.failure." + failure.name().toLowerCase(java.util.Locale.ROOT));
    }

    private void drawIcon(GuiGraphics graphics, ResearchDefinition research, int x, int y) {
        if (ResearchIconDisplay.render(graphics, font, research.iconExpression(), x, y)) return;
        // Original GuiResearchBrowser:404-412 draws the icon of the research only
        // when its expression is a texture of the mod. An expression that names
        // nothing draws NOTHING, it never falls back to another icon.
        var expression = TEXTURE.matcher(research.iconExpression() == null ? "" : research.iconExpression());
        if (expression.matches()) {
            ResourceLocation icon = texture(expression.group(1).toLowerCase(java.util.Locale.ROOT));
            if (thaumcraft.client.lib.MissingResourceDiagnostics.isPresent(minecraft.getResourceManager(), icon)) {
                graphics.blit(icon, x, y, 16, 16, 0, 0, 256, 256, 256, 256);
            }
        }
    }

    // ------------------------------------------------------------------ the port's own bookmarks

    private record Bookmark(ResearchDefinition research, int x, int y) {}

    /** The bookmarked research the player has completed, one column right of the book. */
    private List<Bookmark> bookmarks() {
        if (!thaumcraft.common.config.ThaumcraftTweaks.bookmarks() || minecraft.player == null) return List.of();
        var completed = completed();
        // Right of the book, beyond the second column of category tabs when there is one.
        int x = left + 256 + (tabs.size() > 9 ? 28 : 4), y = top + 4;
        List<Bookmark> shown = new ArrayList<>();
        for (String key : thaumcraft.client.lib.ResearchBookState.bookmarks().keys()) {
            var research = thaumcraft.common.lib.research.ResearchCatalog.get(key);
            if (research == null || !completed.contains(key)) continue;
            shown.add(new Bookmark(research, x, y + shown.size() * 24));
        }
        return shown;
    }

    private Bookmark bookmarkAt(double x, double y) {
        for (Bookmark bookmark : bookmarks())
            if (x >= bookmark.x && x < bookmark.x + 22 && y >= bookmark.y && y < bookmark.y + 22) return bookmark;
        return null;
    }

    private void renderBookmarks(GuiGraphics graphics, int mouseX, int mouseY) {
        Bookmark hovered = null;
        for (Bookmark bookmark : bookmarks()) {
            blend();
            graphics.blit(FRAME, bookmark.x - 2, bookmark.y - 2, badgeFrame(bookmark.research, false), 230, 26, 26, 256, 256);
            drawIcon(graphics, bookmark.research, bookmark.x + 3, bookmark.y + 3);
            if (mouseX >= bookmark.x && mouseX < bookmark.x + 22 && mouseY >= bookmark.y && mouseY < bookmark.y + 22) hovered = bookmark;
        }
        if (hovered != null) graphics.renderComponentTooltip(font, List.of(Component.translatable(hovered.research.nameKey()),
                Component.translatable("tc.port.bookmark.open").withStyle(ChatFormatting.DARK_GRAY)), mouseX, mouseY);
    }

    /** The port's own map keys: the arrows move the map by one cell, Tab changes the category. */
    private boolean mapKey(int key, int modifiers) {
        int step = 24;
        switch (key) {
            case org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT -> pan(step, 0);
            case org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT -> pan(-step, 0);
            case org.lwjgl.glfw.GLFW.GLFW_KEY_UP -> pan(0, step);
            case org.lwjgl.glfw.GLFW.GLFW_KEY_DOWN -> pan(0, -step);
            case org.lwjgl.glfw.GLFW.GLFW_KEY_TAB -> {
                if (tabs.isEmpty()) return true;
                int current = 0;
                for (int index = 0; index < tabs.size(); index++) if (tabs.get(index).key().equals(category)) current = index;
                int next = Math.floorMod(current + ((modifiers & org.lwjgl.glfw.GLFW.GLFW_MOD_SHIFT) != 0 ? -1 : 1), tabs.size());
                selectCategory(tabs.get(next).key());
            }
            default -> { return false; }
        }
        return true;
    }

    private void pan(double dx, double dy) {
        int[] bounds = mapBounds();
        panX = -Math.clamp(-panX - graphWidth / 2 - dx, bounds[0], bounds[2] - 1) - graphWidth / 2;
        panY = -Math.clamp(-panY - graphHeight / 2 - dy, bounds[1], bounds[3] - 1) - graphHeight / 2;
    }

    private void selectCategory(String key) {
        category = key;
        thaumcraft.client.lib.ResearchNotifications.selectCategory(category);
        rebuildWidgets();
        // After a key press 1.21.1 focuses the first widget, the search box, which would take the arrows.
        if (search != null) search.setFocused(false);
        setFocused(null);
    }

    // 1.21.1 Screen.render invokes this again before drawing its widgets. The
    // background was already drawn above; a second blur would blur the map.
    @Override public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float delta) {}

    private boolean inGraph(double x, double y) { return x >= graphLeft && y >= graphTop && x < graphLeft + graphWidth && y < graphTop + graphHeight; }
    private Node nodeAt(double x, double y) {
        if (!inGraph(x, y) || inResults(x, y)) return null;
        for (Node node : nodes) if (x >= node.x - 2 && y >= node.y - 2 && x < node.x + 24 && y < node.y + 24) return node;
        return null;
    }
    /** Original GuiResearchBrowser:668-687 picks the category of the tab under the mouse. */
    private Tab tabAt(double x, double y) {
        for (Tab tab : tabs) if (x >= tab.x() && x < tab.x() + 24 && y >= tab.y() && y < tab.y() + 24) return tab;
        return null;
    }
    @Override public boolean mouseClicked(double x, double y, int button) {
        if (search != null && button == 1 && search.isMouseOver(x, y)) {
            search.setValue("");
            setFocused(search);
            return true;
        }
        var found = resultAt(x, y);
        if (button == 0 && found != null) {
            minecraft.setScreen(new GuiResearchRecipe(this, found.research()));
            return true;
        }
        if (inResults(x, y)) return true;
        if (search != null && search.isFocused() && !search.isMouseOver(x, y)) {
            search.setFocused(false);
            setFocused(null);
        }
        Tab tab = tabAt(x, y);
        if (button == 0 && tab != null) {
            selectCategory(tab.key());
            return true;
        }
        Bookmark bookmark = bookmarkAt(x, y);
        if (bookmark != null) {
            if (button == 1) thaumcraft.client.lib.ResearchBookState.bookmarks().remove(bookmark.research.key());
            else if (button == 0) minecraft.setScreen(new GuiResearchRecipe(this, bookmark.research));
            return true;
        }
        Node node = nodeAt(x, y);
        if (button == 0 && node != null && minecraft.player != null) {
            var knowledge = KnowledgeAccess.get(minecraft.player);
            if (knowledge.isResearchComplete(node.research.key())) {
                minecraft.setScreen(new GuiResearchRecipe(this, node.research));
            } else if (!purchases(node.research)) {
                var preview = ResearchNoteIssuance.preview(minecraft.player, node.research.key());
                if (preview.success() && !preview.reused()) {
                    // The server chooses inventory slots and commits paper/ink.
                    NetworkManager.sendToServer(new ResearchNoteRequestPayload(node.research.key()));
                } else {
                    minecraft.player.displayClientMessage(preview.success()
                            ? Component.translatable("tc.port.note.action_existing") : noteFailureReason(preview.failure()), true);
                }
            } else {
                var preview = ResearchPurchase.preview(knowledge, node.research.key());
                if (preview.success()) {
                    // Only the canonical key crosses the wire. Completion and
                    // pool changes arrive in the authoritative server snapshot.
                    NetworkManager.sendToServer(new ResearchPurchasePayload(node.research.key()));
                } else {
                    minecraft.player.displayClientMessage(failureReason(preview.failure()), true);
                }
            }
            return true;
        }
        return super.mouseClicked(x, y, button);
    }
    @Override public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
        if (button == 0 && inGraph(x, y) && !inResults(x, y)) {
            pan(dx, dy);
            return true;
        }
        return super.mouseDragged(x, y, button, dx, dy);
    }
    private static void highlight(GuiGraphics graphics, int x, int y) {
        float[] old = com.mojang.blaze3d.systems.RenderSystem.getShaderColor().clone();
        try { graphics.setColor(1,1,1,1); graphics.blit(HIGHLIGHT,x,y,(int)(System.nanoTime()/50_000_000L%16)*16,80,16,16); }
        finally { graphics.setColor(old[0],old[1],old[2],old[3]); }
    }
    @Override public void onClose() {
        thaumcraft.client.lib.ResearchNotifications.clearHighlights();
        super.onClose();
    }
    @Override public void removed() {
        thaumcraft.client.lib.ResearchNotifications.rememberCenter((int)((82-graphWidth/2-panX)/24),(int)((70-graphHeight/2-panY)/24));
        super.removed();
    }
    @Override public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (search != null && search.isFocused()) {
            // Typing never closes the book; Escape first leaves the search box.
            if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) { search.setFocused(false); setFocused(null); return true; }
            return search.keyPressed(key, scanCode, modifiers) || search.canConsumeInput() || super.keyPressed(key, scanCode, modifiers);
        }
        if (minecraft.options.keyInventory.matches(key,scanCode)) { onClose(); return true; }
        if (thaumcraft.common.config.ThaumcraftTweaks.bookKeyboard() && mapKey(key, modifiers)) return true;
        return super.keyPressed(key,scanCode,modifiers);
    }
    @Override public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        if (inResults(x, y)) {
            int visible = (graphHeight - 4) / RESULT_ROW;
            resultScroll = Math.clamp(resultScroll - (int) Math.signum(scrollY), 0, Math.max(0, results.size() - visible));
            return true;
        }
        return super.mouseScrolled(x, y, scrollX, scrollY);
    }
    @Override public boolean isPauseScreen() { return false; }
}
