package thaumcraft.client.renderers.block;

import net.minecraft.core.Direction;

/**
 * The geometry of the arcane furnace.
 *
 * <p>Original {@code BlockArcaneFurnaceRenderer} (oracle
 * {@code thaumcraft/client/renderers/block/BlockArcaneFurnaceRenderer.java})
 * draws the eleven metadata values of the furnace. The core is a quarter high
 * block of lava, the shell blocks carry one of the twenty seven furnace
 * textures and the metadata ten nozzle draws three sheets that grow out of the
 * block towards the core: the grate, the grin and the fire.
 *
 * <p>The class is a pure geometry helper, so the texture selection and the
 * nozzle planes can be checked in a unit test.
 */
public final class ArcaneFurnaceBakedModel {
    /** The original number of furnace textures. */
    public static final int TEXTURE_COUNT = 27;
    /** The original texture of the plain shell. */
    public static final int TEXTURE_PLAIN = 7;
    /** The original texture of the shell next to a nozzle. */
    public static final int TEXTURE_NOZZLE = 6;
    /** The original texture of the filled top of the core. */
    public static final int TEXTURE_TOP = 10;
    /** The original thickness of a nozzle sheet, in sixteenths of a block. */
    public static final float SHEET_THICKNESS = 1.0F;
    /** The original depth of the grate sheet, in sixteenths of a block. */
    public static final float GRATE_DEPTH = 10.0F;
    /** The original depth of the grin sheet, in sixteenths of a block. */
    public static final float GRIN_DEPTH = 12.8F;
    /** The original depth of the fire sheet, in sixteenths of a block. */
    public static final float FIRE_DEPTH = 14.4F;

    private ArcaneFurnaceBakedModel() {}

    /**
     * The original texture index of a face of a shell block.
     *
     * @param meta       the original metadata of the block, zero to nine
     * @param level      the original render level of the block, zero to eighteen
     * @param nozzleSide the side the nozzle of the furnace is on, or minus one
     * @param face       the face the texture is asked for
     */
    public static int textureForSide(int meta, int level, int nozzleSide, Direction face) {
        int side = face.get3DDataValue();
        int nozzleOffset = nozzleSide == side ? 3 : 0;
        switch (face) {
            case DOWN:
            case UP:
                if (face == Direction.UP && level == 18) {
                    switch (meta) {
                        case 2: return 16;
                        case 4: return 17;
                        case 6: return 26;
                        case 8: return 25;
                        default: {
                        }
                    }
                }
                if (nozzleOffset == 3) return TEXTURE_NOZZLE;
                if (meta == 5) return TEXTURE_TOP;
                int index = (meta - 1) % 3 + ((meta - 1) / 3) * 9;
                return index >= 0 ? index : TEXTURE_PLAIN;
            case NORTH:
                switch (meta) {
                    case 1: return 2 + level + nozzleOffset;
                    case 2: return 1 + level + nozzleOffset;
                    case 3: return level + nozzleOffset;
                    default: return level != 9 ? TEXTURE_PLAIN : TEXTURE_NOZZLE;
                }
            case SOUTH:
                switch (meta) {
                    case 7: return level + nozzleOffset;
                    case 8: return 1 + level + nozzleOffset;
                    case 9: return 2 + level + nozzleOffset;
                    default: return level != 9 ? TEXTURE_PLAIN : TEXTURE_NOZZLE;
                }
            case WEST:
                switch (meta) {
                    case 1: return level + nozzleOffset;
                    case 4: return 1 + level + nozzleOffset;
                    case 7: return 2 + level + nozzleOffset;
                    default: return level != 9 ? TEXTURE_PLAIN : TEXTURE_NOZZLE;
                }
            case EAST:
                switch (meta) {
                    case 3: return 2 + level + nozzleOffset;
                    case 6: return 1 + level + nozzleOffset;
                    case 9: return level + nozzleOffset;
                    default: return level != 9 ? TEXTURE_PLAIN : TEXTURE_NOZZLE;
                }
            default:
                return nozzleOffset == 0 ? TEXTURE_PLAIN : TEXTURE_NOZZLE;
        }
    }

    /** One sheet of the output nozzle of the arcane furnace. */
    public record NozzlePlane(int textureIndex, Direction outwardFace, float minX, float maxX, float minZ, float maxZ, float maxY) {}

    /**
     * The three sheets of the output nozzle.
     *
     * <p>The original faces point away from the core of the furnace: the grate,
     * the grin and the fire grow out of the block towards the core, but the
     * visible sheets face the other way, otherwise the terrain renderer culls
     * them and the nozzle disappears.
     */
    public static NozzlePlane[] nozzleGeometry(Direction coreFacing) {
        if (coreFacing == null || coreFacing.getAxis().isVertical()) coreFacing = Direction.SOUTH;
        return new NozzlePlane[]{
                plane(coreFacing, GRATE_DEPTH),
                plane(coreFacing, GRIN_DEPTH),
                plane(coreFacing, FIRE_DEPTH)};
    }

    private static NozzlePlane plane(Direction coreFacing, float depth) {
        float coordinate = coreFacing.getAxisDirection() == Direction.AxisDirection.POSITIVE
                ? depth : 16.0F - depth;
        Direction outward = coreFacing.getOpposite();
        float minX = 0.0F;
        float maxX = 16.0F;
        float minZ = 0.0F;
        float maxZ = 16.0F;
        switch (outward) {
            case EAST -> {
                minX = coordinate - SHEET_THICKNESS;
                maxX = coordinate;
            }
            case WEST -> {
                minX = coordinate;
                maxX = coordinate + SHEET_THICKNESS;
            }
            case SOUTH -> {
                minZ = coordinate - SHEET_THICKNESS;
                maxZ = coordinate;
            }
            default -> {
                minZ = coordinate;
                maxZ = coordinate + SHEET_THICKNESS;
            }
        }
        return new NozzlePlane(TEXTURE_PLAIN, outward, minX, maxX, minZ, maxZ, 24.0F);
    }
}
