package thaumic.tinkerer.fabric.clientsmoke;

import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

/**
 * A harness screen that draws item stacks the way an inventory does: {@code GuiGraphics.renderItem} and the item
 * decorations (count, durability bar), twice the size, each with its registry path and its displayed name under it. Test-only
 * code of the port (no 1.7.10 counterpart); CC BY-NC-SA 3.0 like the add-on, not distributed.
 *
 * <p>The name line is the translated hover name, so a missing translation shows up in the picture as a raw key.</p>
 */
final class ItemGridScreen extends Screen {
    static final int COLUMNS = 10;
    static final int ROWS = 6;
    static final int CELL_W = 64;
    static final int CELL_H = 56;
    private static final int TOP = 18;
    private static final float TEXT_SCALE = 0.6F;

    /** One stack of the grid. {@code note} is a short remark ("damaged", "count 2") shown after the path. */
    record Cell(ItemStack stack, String path, String name, String note) {}

    private final List<Cell> cells;
    private final String heading;

    ItemGridScreen(String heading, List<Cell> cells) {
        super(Component.literal(heading));
        this.heading = heading;
        this.cells = cells;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xFF1E1E22);
        graphics.drawString(font, heading, 6, 4, 0xFFFFFF, true);
        for (int i = 0; i < cells.size(); i++) drawCell(graphics, cells.get(i), (i % COLUMNS) * CELL_W, TOP + (i / COLUMNS) * CELL_H);
    }

    /** Draws one cell; also called on its own by the pre-render guard, where an exception names the cell that caused it. */
    void drawCell(GuiGraphics graphics, Cell cell, int x, int y) {
        graphics.fill(x + 2, y + 1, x + CELL_W - 2, y + CELL_H - 1, 0xFF34343A);
        graphics.pose().pushPose();
        graphics.pose().translate(x + (CELL_W - 32) / 2.0F, y + 3, 0);
        graphics.pose().scale(2.0F, 2.0F, 1.0F);
        graphics.renderItem(cell.stack(), 0, 0);
        graphics.renderItemDecorations(font, cell.stack(), 0, 0);
        graphics.pose().popPose();
        int textY = y + 37;
        textY = text(graphics, cell.path() + (cell.note().isEmpty() ? "" : " " + cell.note()), x + 4, textY, 0xFFFFFF, 2);
        text(graphics, cell.name(), x + 4, textY, 0xFFDD66, 2);
    }

    private int text(GuiGraphics graphics, String text, int x, int y, int color, int maxLines) {
        List<FormattedCharSequence> lines = font.split(Component.literal(text), (int) ((CELL_W - 8) / TEXT_SCALE));
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0);
        graphics.pose().scale(TEXT_SCALE, TEXT_SCALE, 1.0F);
        int lineY = 0;
        for (int i = 0; i < Math.min(maxLines, lines.size()); i++) {
            graphics.drawString(font, lines.get(i), 0, lineY, color, false);
            lineY += 9;
        }
        graphics.pose().popPose();
        return y + (int) (Math.min(maxLines, lines.size()) * 9 * TEXT_SCALE) + 1;
    }

    /** Draws every cell into a throw-away {@link GuiGraphics}; the names of the cells that threw are returned. */
    List<String> dryRun(GuiGraphics graphics) {
        List<String> broken = new java.util.ArrayList<>();
        for (int i = 0; i < cells.size(); i++) {
            Cell cell = cells.get(i);
            try {
                drawCell(graphics, cell, (i % COLUMNS) * CELL_W, TOP + (i / COLUMNS) * CELL_H);
                graphics.flush();
            } catch (RuntimeException | LinkageError e) {
                broken.add(cell.path() + ": " + e);
            }
        }
        return broken;
    }
}
