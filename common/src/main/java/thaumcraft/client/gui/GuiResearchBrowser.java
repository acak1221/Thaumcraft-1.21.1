package thaumcraft.client.gui;

import dev.architectury.networking.NetworkManager;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
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
    private final List<Node> nodes = new ArrayList<>();
    private final List<Tab> tabs = new ArrayList<>();
    private String category = thaumcraft.client.lib.ResearchNotifications.selectedCategory();
    private static final ResourceLocation HIGHLIGHT = texture("textures/misc/particles.png");
    /** The original forbidden marker is drawn from the node sheet. */
    private static final ResourceLocation NODES = texture("textures/misc/nodes.png");
    /** The original node sheet is 2048x2048 with thirty two frames and thirty two strips. */
    private static final int NODE_CELL = 64;
    private int left, top, panelWidth, panelHeight, graphLeft, graphTop, graphWidth, graphHeight;
    private double panX, panY;
    private record Node(ResearchDefinition research, int x, int y) {}
    private record Tab(String key, int x, int y, ResourceLocation icon) {}

    public GuiResearchBrowser() {
        super(Component.translatable("item.thaumcraft.thaumonomicon"));
        if (thaumcraft.client.lib.ResearchNotifications.hasQueuedResearch()) {
            panX = -thaumcraft.client.lib.ResearchNotifications.lastColumn() * 24;
            panY = -thaumcraft.client.lib.ResearchNotifications.lastRow() * 24;
        }
    }
    private Set<String> completed() {
        return minecraft.player == null ? Set.of() : KnowledgeAccess.get(minecraft.player).getResearchComplete();
    }
    private static ResourceLocation texture(String path) { return ResourceLocation.fromNamespaceAndPath("thaumcraft", path); }

    @Override protected void init() {
        panelWidth = Math.min(510, width - 16); panelHeight = Math.min(300, height - 16);
        left = (width - panelWidth) / 2; top = (height - panelHeight) / 2;
        graphLeft = left + 116; graphTop = top + 28;
        graphWidth = panelWidth - 126; graphHeight = panelHeight - 58;
        tabs.clear();
        var categories = ResearchBrowserData.categories(completed());
        if (!categories.contains(category)) category = "BASICS";
        int y = top + 30;
        for (String key : categories) {
            // Original GuiResearchBrowser:460-509 draws one tab per visible
            // category, 24 pixels apart, each with the icon of its category.
            tabs.add(new Tab(key, left + 8, y, categoryIcon(key)));
            y += 24;
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(left + panelWidth - 88, top + panelHeight - 24, 80, 20).build());
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.renderBackground(graphics, mouseX, mouseY, delta);
        graphics.fill(left, top, left + panelWidth, top + panelHeight, 0xF0181513);
        graphics.drawString(font, title, left + 10, top + 10, 0xFFE8D8B4, false);
        graphics.drawString(font, Component.translatable("tc.book.drag"), graphLeft, top + panelHeight - 18, 0xFFBCAA89, false);
        var completed = completed();
        var entries = ResearchBrowserData.entries(category, completed);
        nodes.clear();
        int originX = graphLeft + graphWidth / 2 + (int)panX;
        int originY = graphTop + graphHeight / 2 + (int)panY;
        for (var research : entries) nodes.add(new Node(research, originX + research.column() * 24, originY + research.row() * 24));
        graphics.enableScissor(graphLeft, graphTop, graphLeft + graphWidth, graphTop + graphHeight);
        graphics.blit(texture("textures/gui/" + (category.equals("ELDRITCH") ? "gui_researchbackeldritch.png" : "gui_researchback.png")),
                graphLeft, graphTop, 0, 0, graphWidth, graphHeight, 512, 512);
        drawConnectors(graphics, completed, originX, originY);
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
            if (!ResearchIconDisplay.render(graphics, font, node.research.iconExpression(), node.x + 3, node.y + 3)) {
                // Original GuiResearchBrowser:404-412 draws the icon of the research only
                // when its expression is a texture of the mod. An expression that names
                // nothing draws NOTHING, it never falls back to another icon.
                var expression = TEXTURE.matcher(node.research.iconExpression() == null ? "" : node.research.iconExpression());
                if (expression.matches()) {
                    ResourceLocation icon = texture(expression.group(1).toLowerCase(java.util.Locale.ROOT));
                    if (!minecraft.getResourceManager().getResource(icon).isEmpty()) {
                        graphics.blit(icon, node.x + 3, node.y + 3, 16, 16, 0, 0, 256, 256, 256, 256);
                    }
                }
            }
            if (!available && !unlocked) graphics.fill(node.x, node.y, node.x + 22, node.y + 22, 0x99000000);
            if (unlocked) graphics.fill(node.x + 17, node.y + 17, node.x + 22, node.y + 22, 0xFF75AD45);
        }
        graphics.disableScissor();
        renderTabs(graphics);
        super.render(graphics, mouseX, mouseY, delta);
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
            int shift = selected ? 0 : 8;
            graphics.blit(FRAME, tab.x(), tab.y(), selected ? 152 : 176, 232, 24, 24, 256, 256);
            if (thaumcraft.client.lib.ResearchNotifications.highlighted(tab.key())) {
                highlight(graphics, tab.x() - 3 + shift, tab.y() - 4);
            }
            ResourceLocation icon = tab.icon();
            if (icon != null) graphics.blit(icon, tab.x() + 5 + shift, tab.y() + 4, 16, 16, 0, 0, 32, 32, 32, 32);
            if (!selected) graphics.blit(FRAME, tab.x(), tab.y(), 200, 232, 24, 24, 256, 256);
        }
        com.mojang.blaze3d.systems.RenderSystem.disableBlend();
    }

    /**
     * Original GuiResearchBrowser:789-802 ({@code drawForbidden}): the animated quad strip of the
     * tainted node strip, eighty pixels wide, in the original colour 0x440055 with the alpha
     * {@code 0.66}. The frame runs backwards over the thirty two frames of the sheet.
     */
    private void forbidden(GuiGraphics graphics, ResearchDefinition research, int centreX, int centreY) {
        if (research.warp() <= 0) return;
        int frames = 32, strip = 5, size = 80;
        int part = minecraft.player == null ? 0 : minecraft.player.tickCount % frames;
        int frame = frames - 1 - part;
        blend();
        graphics.setColor(0x44 / 255F, 0x00 / 255F, 0x55 / 255F, 168F / 255F);
        graphics.blit(NODES, centreX - size / 2, centreY - size / 2,
                frame * NODE_CELL, strip * NODE_CELL, size, size, 2048, 2048);
        graphics.setColor(1, 1, 1, 1);
    }

    /**
     * Original GuiResearchBrowser:299-348 - the parent and the sibling connectors of the map.
     *
     * <p>The original draws them over <em>every</em> research of the selected category, not only
     * over the visible cells, so the line of a hidden research is drawn as well. The line itself
     * is the three pixel wide gradient polyline of {@code :732-787}: the colour of a vertex fades
     * to black along the line, the alpha grows from zero to 0.6 and the whole line wobbles with
     * the sine of the ticks of the player.</p>
     */
    private void drawConnectors(GuiGraphics graphics, Set<String> completed, int originX, int originY) {
        Map<String, int[]> positions = new LinkedHashMap<>();
        for (var research : ResearchBrowserData.categoryEntries(category)) {
            positions.put(research.key(), new int[] {originX + research.column() * 24, originY + research.row() * 24});
        }
        blend();
        for (var connector : ResearchBrowserConnectors.connectors(category, completed)) {
            int[] from = positions.get(connector.from());
            int[] to = positions.get(connector.to());
            if (from == null || to == null) continue;
            connectorLine(graphics, from[0] + 11, from[1] + 11, to[0] + 11, to[1] + 11, connector);
        }
    }

    /**
     * Original GuiResearchBrowser:732-787 ({@code drawLine}): a polyline of {@code dist / 2}
     * segments whose width is three pixels, whose colour fades to black along the line and whose
     * vertices wobble with the ticks of the player.
     */
    private void connectorLine(GuiGraphics graphics, float x, float y, float x2, float y2,
                                ResearchBrowserConnectors.Connector connector) {
        float dx = x - x2, dy = y - y2;
        int segments = ResearchBrowserConnectors.segments(dx, dy);
        if (segments <= 0) return;
        float count = minecraft.player == null ? 0 : minecraft.player.tickCount;
        float stepX = dx / segments, stepY = dy / segments;
        boolean horizontal = Math.abs(dx) > Math.abs(dy);
        if (horizontal) stepX *= 2.0F; else stepY *= 2.0F;
        float previousX = x, previousY = y;
        float previousRed = connector.red(), previousGreen = connector.green(), previousBlue = connector.blue();
        float previousAlpha = ResearchBrowserConnectors.VERTEX_ALPHA;
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
            if (step > 0) {
                int color = argb((previousRed + red) / 2.0F, (previousGreen + green) / 2.0F,
                        (previousBlue + blue) / 2.0F, (previousAlpha + alpha) / 2.0F);
                segment(graphics, previousX, previousY, vertexX, vertexY, color);
            }
            previousX = vertexX; previousY = vertexY;
            previousRed = red; previousGreen = green; previousBlue = blue; previousAlpha = alpha;
            if (horizontal) stepX *= 1.0F - 1.0F / (segments * 3.0F / 2.0F);
            else stepY *= 1.0F - 1.0F / (segments * 3.0F / 2.0F);
        }
    }

    /**
     * One segment of the original line. The 1.21.1 GUI cannot draw a three pixel wide diagonal
     * line, so the segment is drawn as the staircase of the original polyline: a horizontal bar
     * at the height of its first vertex and a vertical bar at the x of its second one.
     */
    private static void segment(GuiGraphics graphics, float x1, float y1, float x2, float y2, int color) {
        int half = (int) Math.ceil(ResearchBrowserConnectors.WIDTH / 2.0F);
        graphics.fill((int) Math.floor(Math.min(x1, x2)) - half, (int) Math.floor(y1) - half,
                (int) Math.ceil(Math.max(x1, x2)) + half, (int) Math.ceil(y1) + half, color);
        graphics.fill((int) Math.floor(x2) - half, (int) Math.floor(Math.min(y1, y2)) - half,
                (int) Math.ceil(x2) + half, (int) Math.ceil(Math.max(y1, y2)) + half, color);
    }

    private static int argb(float red, float green, float blue, float alpha) {
        int a = Math.max(0, Math.min(255, Math.round(alpha * 255.0F)));
        int r = Math.max(0, Math.min(255, Math.round(red * 255.0F)));
        int g = Math.max(0, Math.min(255, Math.round(green * 255.0F)));
        int b = Math.max(0, Math.min(255, Math.round(blue * 255.0F)));
        return (a << 24) | (r << 16) | (g << 8) | b;
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

    // 1.21.1 Screen.render invokes this again before drawing its widgets. The
    // background was already drawn above; a second blur would blur the map.
    @Override public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float delta) {}

    private boolean inGraph(double x, double y) { return x >= graphLeft && y >= graphTop && x < graphLeft + graphWidth && y < graphTop + graphHeight; }
    private Node nodeAt(double x, double y) {
        if (!inGraph(x, y)) return null;
        for (Node node : nodes) if (x >= node.x - 2 && y >= node.y - 2 && x < node.x + 24 && y < node.y + 24) return node;
        return null;
    }
    /** Original GuiResearchBrowser:668-687 picks the category of the tab under the mouse. */
    private Tab tabAt(double x, double y) {
        for (Tab tab : tabs) if (x >= tab.x() && x < tab.x() + 24 && y >= tab.y() && y < tab.y() + 24) return tab;
        return null;
    }
    @Override public boolean mouseClicked(double x, double y, int button) {
        Tab tab = tabAt(x, y);
        if (button == 0 && tab != null) {
            category = tab.key();
            thaumcraft.client.lib.ResearchNotifications.selectCategory(category);
            panX = panY = 0;
            rebuildWidgets();
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
        if (button == 0 && inGraph(x, y)) {
            var entries = ResearchBrowserData.entries(category, completed());
            int minX = entries.stream().mapToInt(ResearchDefinition::column).min().orElse(0) * 24;
            int maxX = entries.stream().mapToInt(ResearchDefinition::column).max().orElse(0) * 24;
            int minY = entries.stream().mapToInt(ResearchDefinition::row).min().orElse(0) * 24;
            int maxY = entries.stream().mapToInt(ResearchDefinition::row).max().orElse(0) * 24;
            panX = Math.clamp(panX + dx, -maxX - graphWidth / 3.0, -minX + graphWidth / 3.0);
            panY = Math.clamp(panY + dy, -maxY - graphHeight / 3.0, -minY + graphHeight / 3.0);
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
        thaumcraft.client.lib.ResearchNotifications.rememberCenter((int)(-panX/24),(int)(-panY/24));
        super.removed();
    }
    @Override public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (minecraft.options.keyInventory.matches(key,scanCode)) { onClose(); return true; }
        return super.keyPressed(key,scanCode,modifiers);
    }
    @Override public boolean isPauseScreen() { return false; }
}
