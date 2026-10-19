package thaumcraft.client.gui;

import dev.architectury.networking.NetworkManager;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.items.ItemInkwell;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.network.ResearchPurchasePayload;
import thaumcraft.common.lib.network.ResearchNoteRequestPayload;
import thaumcraft.common.lib.research.ResearchBrowserData;
import thaumcraft.common.lib.research.ResearchDefinition;
import thaumcraft.common.lib.research.ResearchPurchase;
import thaumcraft.common.lib.research.ResearchNoteIssuance;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/** Native research map: original coordinates, clue gates and completed entries. */
public final class GuiResearchBrowser extends Screen {
    private static final Pattern TEXTURE = Pattern.compile("new ResourceLocation\\(\"thaumcraft\",\\s*\"([^\"]+)\"\\)");
    private static final ResourceLocation FRAME = texture("textures/gui/gui_research.png");
    private final List<Node> nodes = new ArrayList<>();
    private String category = "BASICS";
    private int left, top, panelWidth, panelHeight, graphLeft, graphTop, graphWidth, graphHeight;
    private double panX, panY;
    private record Node(ResearchDefinition research, int x, int y) {}

    public GuiResearchBrowser() { super(Component.translatable("item.thaumcraft.thaumonomicon")); }
    private Set<String> completed() {
        return minecraft.player == null ? Set.of() : KnowledgeAccess.get(minecraft.player).getResearchComplete();
    }
    private static ResourceLocation texture(String path) { return ResourceLocation.fromNamespaceAndPath("thaumcraft", path); }

    @Override protected void init() {
        panelWidth = Math.min(510, width - 16); panelHeight = Math.min(300, height - 16);
        left = (width - panelWidth) / 2; top = (height - panelHeight) / 2;
        graphLeft = left + 116; graphTop = top + 28;
        graphWidth = panelWidth - 126; graphHeight = panelHeight - 58;
        int y = top + 30;
        var categories = ResearchBrowserData.categories(completed());
        if (!categories.contains(category)) category = "BASICS";
        for (String key : categories) {
            Button button = Button.builder(Component.translatable("tc.research_category." + key), b -> {
                category = key; panX = panY = 0; rebuildWidgets();
            }).bounds(left + 8, y, 102, 20).build();
            button.active = !key.equals(category);
            button.setTooltip(Tooltip.create(Component.translatable("tc.research_category." + key)));
            addRenderableWidget(button); y += 24;
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
        for (Node node : nodes) {
            for (String parent : node.research.parents()) {
                for (Node target : nodes) if (target.research.key().equals(parent)) {
                    int color = completed.contains(parent) ? 0xFFAA8A48 : 0xFF55504A;
                    graphics.hLine(Math.min(node.x, target.x) + 11, Math.max(node.x, target.x) + 11, node.y + 11, color);
                    graphics.vLine(target.x + 11, Math.min(node.y, target.y) + 11, Math.max(node.y, target.y) + 11, color);
                }
            }
        }
        for (Node node : nodes) {
            boolean unlocked = completed.contains(node.research.key());
            boolean available = node.research.hasPrerequisites(completed);
            int frameX = node.research.flags().contains("Round") ? 54 : node.research.isHidden() ? 86 : node.research.isSecondary() ? 110 : 0;
            graphics.blit(FRAME, node.x - 2, node.y - 2, frameX, 230, 26, 26, 256, 256);
            if (!ResearchIconDisplay.render(graphics, font, node.research.iconExpression(), node.x + 3, node.y + 3)) {
                var expression = TEXTURE.matcher(node.research.iconExpression() == null ? "" : node.research.iconExpression());
                ResourceLocation icon = expression.matches() ? texture(expression.group(1).toLowerCase(java.util.Locale.ROOT))
                        : texture("textures/items/thaumonomicon.png");
                if (minecraft.getResourceManager().getResource(icon).isEmpty()) icon = texture("textures/items/thaumonomicon.png");
                graphics.blit(icon, node.x + 3, node.y + 3, 16, 16, 0, 0, 256, 256, 256, 256);
            }
            if (!available && !unlocked) graphics.fill(node.x, node.y, node.x + 22, node.y + 22, 0x99000000);
            if (unlocked) graphics.fill(node.x + 17, node.y + 17, node.x + 22, node.y + 22, 0xFF75AD45);
        }
        graphics.disableScissor();
        super.render(graphics, mouseX, mouseY, delta);
        Node hover = nodeAt(mouseX, mouseY);
        if (hover != null) {
            int tooltipWidth = Math.max(120, Math.min(300, width - 24));
            var lines = tooltip(hover.research).stream()
                    .flatMap(line -> font.split(line, tooltipWidth).stream()).toList();
            graphics.renderTooltip(font, lines, mouseX, mouseY);
        }
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
        if (!research.isSecondary()) {
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
        if (research.isSecondary() && !preview.costs().isEmpty()) {
            lines.add(Component.translatable("tc.port.research.cost_header").withStyle(ChatFormatting.GRAY));
            preview.costs().entrySet().stream().sorted((first, second) -> {
                int amount = Integer.compare(second.getValue(), first.getValue());
                return amount != 0 ? amount : first.getKey().compareTo(second.getKey());
            }).forEach(cost -> {
                Aspect aspect = Aspect.getAspect(cost.getKey());
                if (aspect == null || !knowledge.hasDiscoveredAspect(cost.getKey())) {
                    // Original TC4 draws an anonymous icon without an amount for
                    // an undiscovered aspect. Do not reveal its name or price.
                    lines.add(Component.translatable("tc.port.research.cost_unknown").withStyle(ChatFormatting.DARK_GRAY));
                } else {
                    int pool = knowledge.getAspectPoolFor(cost.getKey());
                    lines.add(Component.translatable("tc.port.research.cost", aspect.getName(), pool, cost.getValue())
                            .withStyle(pool >= cost.getValue() ? ChatFormatting.GREEN : ChatFormatting.RED));
                }
            });
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
    @Override public boolean mouseClicked(double x, double y, int button) {
        Node node = nodeAt(x, y);
        if (button == 0 && node != null && minecraft.player != null) {
            var knowledge = KnowledgeAccess.get(minecraft.player);
            if (knowledge.isResearchComplete(node.research.key())) {
                minecraft.setScreen(new GuiResearchRecipe(this, node.research));
            } else if (!node.research.isSecondary()) {
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
    @Override public boolean isPauseScreen() { return false; }
}
