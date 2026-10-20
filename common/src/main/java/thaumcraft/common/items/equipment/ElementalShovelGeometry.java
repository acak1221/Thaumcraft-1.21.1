package thaumcraft.common.items.equipment;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;

/**
 * Where the Shovel of the Earthmover works (original ItemElementalShovel). Mining takes the 3 x 3 square across the face that was hit.
 * Placing copies the clicked block into a 3 x 3 square beside the clicked face, laid out by the shovel's orientation: 0 the square across the
 * face; 1 a vertical square (the height and one horizontal axis: along the player's facing on a top or bottom face, the depth of a north
 * or south face, the width of a west or east one); 2 the same vertical square on a top or bottom face and a horizontal square (a floor) on
 * a side face.
 */
public final class ElementalShovelGeometry {
    /** Orientations the key cycles through, original {@code o % 3}. */
    public static final int ORIENTATIONS = 3;

    private ElementalShovelGeometry() {}

    /** The next orientation after a press of the key. */
    public static int next(int orientation) { return Math.floorMod(orientation + 1, ORIENTATIONS); }

    /** The player's facing in quarter turns, original {@code floor(yaw * 4 / 360 + 0.5) & 3}. */
    public static int quarterTurn(float yaw) { return Mth.floor(yaw * 4.0F / 360.0F + 0.5) & 3; }

    /** The 3 x 3 square across {@code face}, around {@code at}, in the original loop order (first axis outer, second axis inner). */
    public static List<BlockPos> plane(BlockPos at, Direction face) {
        List<BlockPos> cells = new ArrayList<>(9);
        for (int a = -1; a <= 1; ++a) for (int b = -1; b <= 1; ++b) cells.add(at.offset(across(face, a, b)));
        return cells;
    }

    private static BlockPos across(Direction face, int a, int b) {
        int side = face.get3DDataValue();
        if (side <= 1) return new BlockPos(a, 0, b);
        if (side <= 3) return new BlockPos(a, b, 0);
        return new BlockPos(0, b, a);
    }

    /** The nine blocks a click on {@code face} of {@code clicked} fills, beside that face. */
    public static List<BlockPos> layout(BlockPos clicked, Direction face, int orientation, float yaw) {
        int side = face.get3DDataValue();
        int turn = quarterTurn(yaw);
        BlockPos beside = clicked.relative(face);
        List<BlockPos> cells = new ArrayList<>(9);
        for (int a = -1; a <= 1; ++a) {
            for (int b = -1; b <= 1; ++b) {
                int x = 0, y = 0, z = 0;
                if (orientation == 1) {
                    y = b;
                    if (side <= 1) { if (turn == 0 || turn == 2) x = a; else z = a; }
                    else if (side <= 3) z = a;
                    else x = a;
                } else if (orientation == 2) {
                    if (side <= 1) { y = b; if (turn == 0 || turn == 2) x = a; else z = a; }
                    else { z = b; x = a; }
                } else {
                    BlockPos plane = across(face, a, b);
                    x = plane.getX(); y = plane.getY(); z = plane.getZ();
                }
                cells.add(beside.offset(x, y, z));
            }
        }
        return cells;
    }
}
