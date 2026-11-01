package thaumcraft.client.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntPredicate;

/**
 * Where the COMPOUND_CRAFTING page of the original draws everything, as plain numbers: the diagram of a wand construct.
 *
 * <p>Original 4.2.3.5 {@code GuiResearchRecipe.drawCompoundCraftingPage}. The page holds a construct of {@code dx} by
 * {@code dy} by {@code dz} blocks and draws every one of its {@code dy} layers at once, one under the other: there is no
 * layer switch. Layer {@code j} 0 is the top layer and the first {@code dx * dz} entries of the list; inside a layer the
 * entries run with the row {@code k} from {@code dz - 1} down to 0 and, inside a row, the column {@code i} from
 * {@code dx - 1} down to 0. The block of column {@code i}, row {@code k} and layer {@code j} is drawn as an item at
 * {@code (i * 16 + k * 16, k * 8 - i * 8 + j * 50)} of the structure, so a layer is an isometric plan whose cells do not
 * overlap and the layers stand fifty pixels apart. The structure starts at {@code (xoff, 108 + yoff)} of the page, with
 * {@code xoff = 64 - (dx * 16 + dz * 16) / 2} (it is centred on x 56) and {@code yoff = -dy * 25}; a construct of more
 * than three layers is drawn {@code 0.2} smaller for every layer above three, with its origin moved to
 * {@code (xoff * (1 + s), 108 + yoff * (1 - s))}. Under it lies a ground plate, the 64 by 44 region at (0, 72) of the
 * overlay sheet at twice its size and half opaque, at {@code (-8 - xoff, -119 + max(3 - dx, 3 - dz) * 8 + dx * 4 + dz * 4
 * + dy * 50)} of the structure. The title "recipe.type.construct" is centred on x 56 at y 0, and a construct that costs
 * vis has its aspects in a row at y 182, x {@code 14 + 18 * n + (5 - count) * 8}, over a faint wand icon (the 12 by 12
 * region at (68, 76) of the overlay sheet at twice its size, 0.4 opaque) at (0, 174). Hovering a block shows its tooltip;
 * when cells overlap the last one in the order of the list wins, because the original kept the last tooltip it set.</p>
 *
 * <p>The coordinates are those of the page of the original: x 0 is the left edge of its recipe area and y 0 the line of
 * the title. The whole drawing fits {@link #FRAME_LEFT}, 0, {@link #FRAME_WIDTH} by {@link #FRAME_HEIGHT}; the book scales
 * that frame into its page like every other recipe page. Nothing here needs a running game.</p>
 *
 * <p>One bound of the port: the original shrinks a construct by 0.2 per layer above three without a limit, so eight
 * layers are drawn at size zero and more mirrored; the port stops at 0.2 of the size (seven layers and more).</p>
 *
 * @param dx the width of the construct, in blocks: the columns of a layer
 * @param dy the height of the construct, in blocks: the number of layers
 * @param dz the depth of the construct, in blocks: the rows of a layer
 */
public record CompoundLayout(int dx, int dy, int dz) {
    /** The largest side the port accepts; the original had no bound and none of its constructs is wider than five. */
    public static final int MAX_SIDE = 32;
    /** The largest number of cells the port accepts. */
    public static final int MAX_CELLS = 4096;
    /** The left edge of the drawing: the ground plate starts eight pixels left of the recipe area. */
    public static final int FRAME_LEFT = -8;
    /** The width of the drawing: the ground plate, 128 pixels. */
    public static final int FRAME_WIDTH = 128;
    /** The height of the drawing: the title at 0 down to the bottom of the aspect row at 198. */
    public static final int FRAME_HEIGHT = 200;
    /** The centre of the title and of the structure. */
    public static final int TITLE_CENTER = 56;
    /** The side of an item cell, before the construct is shrunk. */
    public static final int CELL = 16;
    /** The top of the aspect row. */
    public static final int ASPECT_Y = 182;
    /** The wand icon behind the aspect row: position and size on the page. */
    public static final int WAND_X = 0, WAND_Y = 174, WAND_SIZE = 24;
    /** The ground plate: its size on the page, before the construct is shrunk. */
    public static final int GROUND_WIDTH = 128, GROUND_HEIGHT = 88;
    /** The smallest size of a construct of many layers, see the class comment. */
    public static final float MIN_SCALE = 0.2F;

    /** One block of the construct: its index in the list of the page and where the structure draws it. */
    public record Cell(int index, int layer, int row, int column, int x, int y) {
        /** The depth the original gave to the items of this layer: 60 for the top layer, ten less for every layer below. */
        public int depth() {
            return 60 - layer * 10;
        }
    }

    public CompoundLayout {
        if (!valid(dx, dy, dz)) throw new IllegalArgumentException("compound construct " + dx + "x" + dy + "x" + dz);
    }

    /** Whether the port draws a construct of this size: every side from 1 to {@link #MAX_SIDE}, at most {@link #MAX_CELLS} cells. */
    public static boolean valid(int dx, int dy, int dz) {
        return dx >= 1 && dy >= 1 && dz >= 1 && dx <= MAX_SIDE && dy <= MAX_SIDE && dz <= MAX_SIDE
                && (long) dx * dy * dz <= MAX_CELLS;
    }

    /** The number of cells, and of the entries the list of the page should have. */
    public int cells() {
        return dx * dy * dz;
    }

    /** The original {@code xoff}: the left edge of the structure that centres it on x 56. */
    public int xoff() {
        return 64 - (dx * 16 + dz * 16) / 2;
    }

    /** The original {@code yoff}: the structure is raised by half a layer for every layer. */
    public int yoff() {
        return -dy * 25;
    }

    /** The original {@code sz}: 0.2 for every layer above three, 0 up to three layers (bounded, see the class comment). */
    public float shrink() {
        return dy > 3 ? Math.min((dy - 3) * 0.2F, 1 - MIN_SCALE) : 0;
    }

    /** The size the structure is drawn at: 1 up to three layers. */
    public float scale() {
        return 1 - shrink();
    }

    /** The x of the origin of the structure on the page. */
    public float originX() {
        return xoff() * (1 + shrink());
    }

    /** The y of the origin of the structure on the page. */
    public float originY() {
        return 108 + yoff() * (1 - shrink());
    }

    /** The left edge of the ground plate, in the space of the structure. */
    public int groundX() {
        return -8 - xoff();
    }

    /** The top edge of the ground plate, in the space of the structure. */
    public int groundY() {
        return -119 + Math.max(3 - dx, 3 - dz) * 8 + dx * 4 + dz * 4 + dy * 50;
    }

    /** Every cell, in the order of the list of the page (layer, then row from the back, then column from the back). */
    public List<Cell> cellsInOrder() {
        List<Cell> cells = new ArrayList<>(cells());
        int index = 0;
        for (int j = 0; j < dy; j++) {
            for (int k = dz - 1; k >= 0; k--) {
                for (int i = dx - 1; i >= 0; i--) {
                    cells.add(new Cell(index++, j, k, i, i * 16 + k * 16, -i * 8 + k * 8 + j * 50));
                }
            }
        }
        return cells;
    }

    /** The x of the left edge of a cell on the page. */
    public float pageX(Cell cell) {
        return originX() + cell.x() * scale();
    }

    /** The y of the top edge of a cell on the page. */
    public float pageY(Cell cell) {
        return originY() + cell.y() * scale();
    }

    /**
     * The index of the cell under a point of the page, or -1. Only cells for which {@code shown} answers true count (the
     * original skipped the null entries of the list); of several the last one in the order of the list wins.
     */
    public int hit(float x, float y, IntPredicate shown) {
        int found = -1;
        float side = CELL * scale();
        for (Cell cell : cellsInOrder()) {
            if (!shown.test(cell.index())) continue;
            float left = pageX(cell), top = pageY(cell);
            if (x >= left && y >= top && x < left + side && y < top + side) found = cell.index();
        }
        return found;
    }

    /** The x of the aspect at {@code position} of a row of {@code count} aspects. */
    public static int aspectX(int position, int count) {
        return 14 + 18 * position + (5 - count) * 8;
    }
}
