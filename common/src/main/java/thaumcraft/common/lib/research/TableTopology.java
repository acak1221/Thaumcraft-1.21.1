package thaumcraft.common.lib.research;

import java.util.List;
import net.minecraft.core.Direction;

/** Original BlockTable metadata geometry, independent of mutable world state. */
public final class TableTopology {
    public static final List<Direction> SEARCH_ORDER = List.of(
            Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST);
    public enum Decision { KEEP, WAIT, COLLAPSE }

    private TableTopology() {}

    public static boolean isMain(int variant) { return variant >= 2 && variant <= 5; }
    public static boolean isSecondary(int variant) { return variant >= 6 && variant <= 9; }

    /** ItemInkwell:63 checks TileTable and md != 6, including secondary 7/8/9. */
    public static boolean canStartConversion(int variant) {
        return variant >= 0 && variant <= 9 && !isMain(variant) && variant != 6;
    }

    /** The neighbour must be a dummy TileTable with old metadata below six. */
    public static boolean canBeConversionPartner(int variant) { return variant == 0 || variant == 1; }

    public static Direction direction(int variant) {
        return switch (variant) {
            case 2, 6 -> Direction.NORTH;
            case 3, 7 -> Direction.SOUTH;
            case 4, 8 -> Direction.WEST;
            case 5, 9 -> Direction.EAST;
            default -> throw new IllegalArgumentException("Table variant has no link: " + variant);
        };
    }

    public static int partnerVariant(int mainVariant) {
        return switch (mainVariant) {
            case 2 -> 7;
            case 3 -> 6;
            case 4 -> 9;
            case 5 -> 8;
            default -> throw new IllegalArgumentException("Not a research table main: " + mainVariant);
        };
    }

    public static int mainVariant(Direction direction) {
        return switch (direction) {
            case NORTH -> 2;
            case SOUTH -> 3;
            case WEST -> 4;
            case EAST -> 5;
            default -> throw new IllegalArgumentException("Vertical research table link");
        };
    }

    public static int placementVariant(Direction facing) {
        if (facing.getAxis() == Direction.Axis.Y) throw new IllegalArgumentException("Vertical table placement facing");
        return facing.getAxis() == Direction.Axis.X ? 1 : 0;
    }

    /**
     * Original neighbour checks are deliberately asymmetric and nonreciprocal.
     * There is no portable metadata value for an arbitrary modern block, so a
     * non-table partner is explicitly invalid instead of borrowing another state.
     */
    public static Decision inspect(int variant, boolean neighbourLoaded, boolean neighbourIsTable,
                                   int neighbourVariant, boolean neighbourIsResearchTable) {
        if (!isMain(variant) && !isSecondary(variant)) return Decision.KEEP;
        if (!neighbourLoaded) return Decision.WAIT;
        if (isMain(variant)) {
            return neighbourIsTable && isSecondary(neighbourVariant) ? Decision.KEEP : Decision.COLLAPSE;
        }
        return neighbourIsResearchTable ? Decision.KEEP : Decision.COLLAPSE;
    }
}
