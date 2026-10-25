package thaumcraft.client.gui;

import java.util.List;
import java.util.Locale;
import java.util.Properties;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
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
 * <p>The five categories are tabs above a scrolling list, so every setting stays reachable at any
 * window size and GUI scale. Each row names the setting and explains it in its tooltip together
 * with the original key. Like the original, a changed setting needs a restart.
 */
public final class ThaumcraftConfigScreen extends Screen {
    private static final int HEADER = 52, FOOTER = 44, ROW_HEIGHT = 24;
    private static final int TEXT = 0xFFFFFF, DIM = 0xA0A0A0;
    private final Screen parent;
    private final Properties values;
    private ThaumcraftConfigStore.Category category = ThaumcraftConfigStore.Category.GENERAL;

    public ThaumcraftConfigScreen(Screen parent) {
        super(Component.translatable("tc.port.config.title"));
        this.parent = parent;
        this.values = ThaumcraftConfigStore.read(ThaumcraftConfigStore.file());
    }

    @Override protected void init() {
        var categories = ThaumcraftConfigStore.Category.values();
        int tabWidth = Math.min(110, (this.width - 20) / categories.length);
        int x = (this.width - tabWidth * categories.length) / 2;
        for (ThaumcraftConfigStore.Category candidate : categories) {
            Button tab = Button.builder(categoryName(candidate), button -> {
                category = candidate;
                rebuildWidgets();
            }).bounds(x + 1, 24, tabWidth - 2, 20).tooltip(Tooltip.create(Component.literal(candidate.id()))).build();
            tab.active = candidate != category;
            addRenderableWidget(tab);
            x += tabWidth;
        }
        SettingsList list = new SettingsList(this.minecraft, this.width, this.height - HEADER - FOOTER, HEADER);
        for (ThaumcraftConfigStore.Entry entry : ThaumcraftConfigStore.entries(category)) list.add(new Row(entry));
        addRenderableWidget(list);
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose())
                .bounds(this.width / 2 - 100, this.height - 26, 200, 20).build());
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 9, TEXT);
        if (ThaumcraftConfigStore.entries(category).isEmpty()) {
            graphics.drawCenteredString(this.font, Component.translatable("tc.port.config.empty"), this.width / 2, HEADER + 12, DIM);
        }
        graphics.drawCenteredString(this.font, Component.translatable("tc.port.config.restart"), this.width / 2, this.height - FOOTER + 5, DIM);
    }

    @Override public void onClose() {
        // The original saved its configuration when the screen closed; the port writes the file.
        ThaumcraftConfigStore.write(ThaumcraftConfigStore.file(), values);
        if (this.parent != null) this.minecraft.setScreen(this.parent);
        else this.minecraft.setScreen(null);
    }

    static Component categoryName(ThaumcraftConfigStore.Category category) {
        return Component.translatable("tc.port.config.category." + category.id().toLowerCase(Locale.ROOT));
    }

    static Component settingName(ThaumcraftConfigStore.Entry entry) {
        return Component.translatable("tc.port.config." + entry.key());
    }

    /** The explanation of the setting, then the key it has in the file and in the original. */
    static Component settingTooltip(ThaumcraftConfigStore.Entry entry) {
        return Component.translatable("tc.port.config." + entry.key() + ".tooltip").append("\n")
                .append(Component.literal(entry.key()).withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
    }

    private final class SettingsList extends ContainerObjectSelectionList<Row> {
        SettingsList(Minecraft minecraft, int width, int height, int top) {
            super(minecraft, width, height, top, ROW_HEIGHT);
        }

        void add(Row row) { addEntry(row); }

        @Override public int getRowWidth() { return Math.min(340, this.width - 40); }
    }

    private final class Row extends ContainerObjectSelectionList.Entry<Row> {
        private final ThaumcraftConfigStore.Entry entry;
        private final Component name;
        private final AbstractWidget control;

        Row(ThaumcraftConfigStore.Entry entry) {
            this.entry = entry;
            this.name = settingName(entry);
            if (entry.flag()) {
                control = CycleButton.onOffBuilder(entry.booleanValue(values)).displayOnlyValue()
                        .create(0, 0, 80, 20, name, (button, value) -> values.setProperty(entry.key(), Boolean.toString(value)));
            } else {
                control = new IntegerSlider(entry, 120);
            }
            control.setTooltip(Tooltip.create(settingTooltip(entry)));
        }

        @Override public void render(GuiGraphics graphics, int index, int top, int left, int width, int height,
                                     int mouseX, int mouseY, boolean hovering, float partialTick) {
            var font = ThaumcraftConfigScreen.this.font;
            int labelWidth = width - control.getWidth() - 8;
            graphics.drawString(font, trim(name, labelWidth), left, top + (height - 8) / 2 - 1, TEXT);
            if (mouseX >= left && mouseX < left + labelWidth && mouseY >= top && mouseY < top + height) {
                ThaumcraftConfigScreen.this.setTooltipForNextRenderPass(font.split(settingTooltip(entry), 220));
            }
            control.setPosition(left + width - control.getWidth(), top);
            control.render(graphics, mouseX, mouseY, partialTick);
        }

        @Override public List<? extends GuiEventListener> children() { return List.of(control); }
        @Override public List<? extends NarratableEntry> narratables() { return List.of(control); }
    }

    /** An integer setting between its minimum and maximum; research difficulty names its three modes. */
    private final class IntegerSlider extends AbstractSliderButton {
        private final ThaumcraftConfigStore.Entry entry;

        IntegerSlider(ThaumcraftConfigStore.Entry entry, int width) {
            super(0, 0, width, 20, Component.empty(), fraction(entry, entry.integerValue(values)));
            this.entry = entry;
            updateMessage();
        }

        private static double fraction(ThaumcraftConfigStore.Entry entry, int value) {
            int span = entry.max() - entry.min();
            return span <= 0 ? 0 : Mth.clamp((value - entry.min()) / (double) span, 0, 1);
        }

        private int current() { return entry.min() + (int) Math.round(value * (entry.max() - entry.min())); }

        @Override protected void updateMessage() {
            int current = current();
            setMessage("research_difficulty".equals(entry.key())
                    ? Component.translatable("tc.port.config.research_difficulty.value." + current)
                    : Component.literal(Integer.toString(current)));
        }

        @Override protected void applyValue() { values.setProperty(entry.key(), Integer.toString(current())); }
    }

    /** A label cut to the width it has, with an ellipsis, so it never runs under its control. */
    private net.minecraft.util.FormattedCharSequence trim(Component text, int width) {
        if (this.font.width(text) <= width) return text.getVisualOrderText();
        var kept = this.font.substrByWidth(text, Math.max(0, width - this.font.width("...")));
        return net.minecraft.locale.Language.getInstance().getVisualOrder(
                net.minecraft.network.chat.FormattedText.composite(kept, net.minecraft.network.chat.FormattedText.of("...")));
    }
}
