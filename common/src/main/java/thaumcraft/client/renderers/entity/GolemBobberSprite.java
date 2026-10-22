package thaumcraft.client.renderers.entity;

/**
 * The sheet cell the original golem bobber draws.
 *
 * <p>TC4 4.2.3.5, oracle {@code thaumcraft/client/renderers/entity/RenderGolemBobber.java:31,40-45}:
 * the texture is the <b>vanilla 1.7.10</b> {@code textures/particle/particles.png}
 * (a 128x128 sheet) and the quad reads the cell <b>(1,2)</b>:</p>
 *
 * <pre>
 *   int b0 = 1;                       // the column of the cell
 *   int b1 = 2;                       // the row of the cell
 *   float f2 = (b0 * 8 + 0) / 128.0f; // 0.0625  the left edge of the cell
 *   float f3 = (b0 * 8 + 8) / 128.0f; // 0.125   the right edge of the cell
 *   float f4 = (b1 * 8 + 0) / 128.0f; // 0.125   the top edge of the cell
 *   float f5 = (b1 * 8 + 8) / 128.0f; // 0.1875  the bottom edge of the cell
 * </pre>
 *
 * <p>The cell is eight pixels wide and high, so the sheet fractions of the
 * original are the fractions of the shipped 8x8 asset. The class holds no
 * Minecraft type, so the numbers can be asserted without a client.</p>
 */
public final class GolemBobberSprite {
    /** The width and the height of the vanilla 1.7.10 particle sheet. */
    public static final int SHEET = 128;
    /** The side of one cell of the sheet, in pixels. */
    public static final int CELL = 8;
    /** The column of the original's cell. */
    public static final int CELL_X = 1;
    /** The row of the original's cell. */
    public static final int CELL_Y = 2;
    /** The left edge of the cell in the sheet. */
    public static final float U0 = (CELL_X * CELL) / (float) SHEET;
    /** The right edge of the cell in the sheet. */
    public static final float U1 = (CELL_X * CELL + CELL) / (float) SHEET;
    /** The top edge of the cell in the sheet (the sheet's row zero is its top). */
    public static final float V0 = (CELL_Y * CELL) / (float) SHEET;
    /** The bottom edge of the cell in the sheet. */
    public static final float V1 = (CELL_Y * CELL + CELL) / (float) SHEET;
    /** The first column of the cell in the sheet, in pixels. */
    public static final int PIXEL_X = CELL_X * CELL;
    /** The first row of the cell in the sheet, in pixels. */
    public static final int PIXEL_Y = CELL_Y * CELL;

    /** The sheet fraction of a cell column, as a fraction of the cell itself. */
    public static float cellU(float sheetU) { return (sheetU - U0) / (U1 - U0); }

    /** The sheet fraction of a cell row, as a fraction of the cell itself. */
    public static float cellV(float sheetV) { return (sheetV - V0) / (V1 - V0); }

    private GolemBobberSprite() {}
}
