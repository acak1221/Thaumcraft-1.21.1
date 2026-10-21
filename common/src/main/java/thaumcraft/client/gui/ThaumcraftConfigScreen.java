package thaumcraft.client.gui;

import java.util.List;
import java.util.Properties;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import thaumcraft.common.config.ThaumcraftConfigStore;

/**
 * The native configuration screen of the port.
 *
 * <p>The original showed its settings in the Forge configuration screen that
 * {@code thaumcraft/client/ThaumcraftGuiFactory.java:24-25} opened and
 * {@code thaumcraft/client/gui/ThaumcraftGuiConfig.java:10-34} filled with the five categories of
 * the original {@code Config} (oracle {@code thaumcraft/common/config/Config.java:104-111}). 1.21.1
 * has no Forge configuration screen, so this screen edits the properties file of the port
 * ({@code config/thaumcraft.properties}) and keeps the original category names and keys.
 *
 * <p>Like the original, a changed setting needs a restart: the file itself says so.
 */
public final class ThaumcraftConfigScreen extends Screen {
    private static final int PANEL_WIDTH = 268, PANEL_HEIGHT = 232;
    private static final int ROW_HEIGHT = 22;
    private static final int TEXT = 0xE0E0E0, DIM = 0xA0A0A0;
    private final Screen parent;
    private final Properties values;
    private ThaumcraftConfigStore.Category category = ThaumcraftConfigStore.Category.GENERAL;

    public ThaumcraftConfigScreen(Screen parent) {
        super(Component.literal("Thaumcraft"));
        this.parent = parent;
        this.values = ThaumcraftConfigStore.read(ThaumcraftConfigStore.file());
    }

    @Override protected void init() { rebuild(); }

    /** The five original categories as tabs, the settings of the selected one below them. */
    private void rebuild() {
        clearWidgets();
        int left = (this.width - PANEL_WIDTH) / 2, top = (this.height - PANEL_HEIGHT) / 2;
        int tabWidth = (PANEL_WIDTH - 8) / ThaumcraftConfigStore.Category.values().length;
        int x = left + 4;
        for (ThaumcraftConfigStore.Category candidate : ThaumcraftConfigStore.Category.values()) {
            final ThaumcraftConfigStore.Category selected = candidate;
            addRenderableWidget(Button.builder(Component.literal(candidate.id()), button -> {
                category = selected;
                rebuild();
            }).bounds(x, top + 20, tabWidth - 2, 20).build());
            x += tabWidth;
        }
        List<ThaumcraftConfigStore.Entry> entries = ThaumcraftConfigStore.entries(category);
        int row = top + 46;
        for (ThaumcraftConfigStore.Entry entry : entries) {
            if (entry.flag()) {
                Button button = addRenderableWidget(Button.builder(Component.literal(entry.value(values)), b -> {
                    values.setProperty(entry.key(), Boolean.toString(!entry.booleanValue(values)));
                    b.setMessage(Component.literal(entry.value(values)));
                }).bounds(left + PANEL_WIDTH - 92, row, 60, 18).build());
                button.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal(entry.key())));
            } else {
                Button down = addRenderableWidget(Button.builder(Component.literal("-"), b -> {
                    int value = Math.max(entry.min(), entry.integerValue(values) - 1);
                    values.setProperty(entry.key(), Integer.toString(value));
                    b.setMessage(Component.literal(Integer.toString(value)));
                }).bounds(left + PANEL_WIDTH - 122, row, 20, 18).build());
                Button up = addRenderableWidget(Button.builder(Component.literal("+"), b -> {
                    int value = Math.min(entry.max(), entry.integerValue(values) + 1);
                    values.setProperty(entry.key(), Integer.toString(value));
                    b.setMessage(Component.literal(Integer.toString(value)));
                }).bounds(left + PANEL_WIDTH - 62, row, 20, 18).build());
                down.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal(entry.key())));
                up.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal(entry.key())));
                addRenderableWidget(Button.builder(Component.literal(entry.value(values)), b -> {
                }).bounds(left + PANEL_WIDTH - 100, row, 36, 18).build());
            }
            row += ROW_HEIGHT;
        }
        addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose()).bounds(left + PANEL_WIDTH - 84, top + PANEL_HEIGHT - 26, 80, 20).build());
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        int left = (this.width - PANEL_WIDTH) / 2, top = (this.height - PANEL_HEIGHT) / 2;
        graphics.drawString(this.font, this.title, left + 6, top + 6, TEXT);
        graphics.drawString(this.font, Component.literal(category.id()), left + 6, top + 32, DIM);
        int row = top + 46;
        for (ThaumcraftConfigStore.Entry entry : ThaumcraftConfigStore.entries(category)) {
            graphics.drawString(this.font, Component.literal(entry.key()), left + 6, row + 5, TEXT);
            row += ROW_HEIGHT;
        }
        if (ThaumcraftConfigStore.entries(category).isEmpty())
            graphics.drawString(this.font, Component.literal("no settings in this category"), left + 6, row + 5, DIM);
        graphics.drawString(this.font, Component.literal("restart required"), left + 6, top + PANEL_HEIGHT - 22, DIM);
    }

    @Override public void onClose() {
        // The original saved its configuration when the screen closed; the port writes the file.
        ThaumcraftConfigStore.write(ThaumcraftConfigStore.file(), values);
        if (this.parent != null) this.minecraft.setScreen(this.parent);
        else this.minecraft.setScreen(null);
    }
}
