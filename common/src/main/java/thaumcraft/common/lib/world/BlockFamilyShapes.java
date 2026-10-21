package thaumcraft.common.lib.world;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The boxes of the TC4 4.2.3.5 block families that the 1.21.1 port reproduces.
 *
 * <p>Every box is taken verbatim from the oracle; the file and the line of the
 * oracle are named in the comment of the method. The boxes live here and not in
 * the block classes, so a unit test can read them without a registered block.</p>
 */
public final class BlockFamilyShapes {
    private BlockFamilyShapes() {}

    /**
     * BlockWoodenDevice:249-256 - the box of the arcane pressure plate is the thin square that
     * is inset by a sixteenth on every side. The unpressed plate is a sixteenth high, the
     * pressed one only half of that.
     */
    public static VoxelShape arcanePressurePlate(double height) {
        double inset = EldritchBlockRules.PLATE_BOX_INSET;
        return Block.box(inset * 16.0D, 0.0D, inset * 16.0D,
                (1.0D - inset) * 16.0D, height * 16.0D, (1.0D - inset) * 16.0D);
    }

    /**
     * BlockMetalDevice:264-268 - the selection box of the item filter is the metal plate
     * across the top three sixteenths of the block, for the open and for the closed one.
     */
    public static VoxelShape itemFilterPlate() {
        return Block.box(0.0D, 13.0D, 0.0D, 16.0D, 16.0D, 16.0D);
    }

    /**
     * BlockMetalDevice:275 - the brainbox is a cube that is inset by three sixteenths.
     */
    public static VoxelShape brainbox() {
        return Block.box(3.0D, 3.0D, 3.0D, 13.0D, 13.0D, 13.0D);
    }

    /**
     * BlockStoneDevice:464-480 - the selection box of the wand pedestal is the half wide
     * column in the middle of the block.
     */
    public static VoxelShape wandPedestal() {
        return Block.box(4.0D, 0.0D, 4.0D, 12.0D, 16.0D, 12.0D);
    }

    /**
     * BlockStoneDevice:449-461 - the wand pedestal stands on a full base, it carries the
     * focus in the middle and it has the wide step between the two.
     */
    public static VoxelShape wandPedestalCollision() {
        return Shapes.or(
                Block.box(0.0D, 0.0D, 0.0D, 16.0D, 4.0D, 16.0D),
                Block.box(4.0D, 8.0D, 4.0D, 12.0D, 16.0D, 12.0D),
                Block.box(2.0D, 4.0D, 2.0D, 14.0D, 8.0D, 14.0D));
    }

    /**
     * BlockMirror:268-295 - the plate of the mirror is a sixteenth thick and it sits on the
     * side of the block it is indexed by: the bottom, the top, the north (-Z), the south (+Z),
     * the west (-X) and the east (+X) side.
     */
    public static VoxelShape mirrorPlate(Direction side) {
        double w = EldritchBlockRules.MIRROR_PLATE_WIDTH;
        return switch (side) {
            case DOWN -> Block.box(0.0D, 0.0D, 0.0D, 16.0D, w * 16.0D, 16.0D);
            case UP -> Block.box(0.0D, 16.0D - w * 16.0D, 0.0D, 16.0D, 16.0D, 16.0D);
            case NORTH -> Block.box(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, w * 16.0D);
            case SOUTH -> Block.box(0.0D, 0.0D, 16.0D - w * 16.0D, 16.0D, 16.0D, 16.0D);
            case WEST -> Block.box(0.0D, 0.0D, 0.0D, w * 16.0D, 16.0D, 16.0D);
            case EAST -> Block.box(16.0D - w * 16.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D);
        };
    }
}
