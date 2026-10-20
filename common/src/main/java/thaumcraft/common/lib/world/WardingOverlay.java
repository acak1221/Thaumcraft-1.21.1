package thaumcraft.common.lib.world;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import thaumcraft.common.blocks.BlockWarded;
import thaumcraft.common.tiles.TileWarded;

/** Original TileWardedRenderer's eight same-owner neighbours and UtilsFX's complete 256-to-47 connected texture map. */
public final class WardingOverlay {
    private static final int[] TEXTURE = {
        0,0,6,6,0,0,6,6,3,3,19,15,3,3,19,15,1,1,18,18,1,1,13,13,2,2,23,31,2,2,27,14,
        0,0,6,6,0,0,6,6,3,3,19,15,3,3,19,15,1,1,18,18,1,1,13,13,2,2,23,31,2,2,27,14,
        4,4,5,5,4,4,5,5,17,17,22,26,17,17,22,26,16,16,20,20,16,16,28,28,21,21,46,42,21,21,43,38,
        4,4,5,5,4,4,5,5,9,9,30,12,9,9,30,12,16,16,20,20,16,16,28,28,25,25,45,37,25,25,40,32,
        0,0,6,6,0,0,6,6,3,3,19,15,3,3,19,15,1,1,18,18,1,1,13,13,2,2,23,31,2,2,27,14,
        0,0,6,6,0,0,6,6,3,3,19,15,3,3,19,15,1,1,18,18,1,1,13,13,2,2,23,31,2,2,27,14,
        4,4,5,5,4,4,5,5,17,17,22,26,17,17,22,26,7,7,24,24,7,7,10,10,29,29,44,41,29,29,39,33,
        4,4,5,5,4,4,5,5,9,9,30,12,9,9,30,12,7,7,24,24,7,7,10,10,8,8,36,35,8,8,34,11
    };
    private static final int[][] PLANE = {{-1,-1},{0,-1},{1,-1},{-1,0},{1,0},{-1,1},{0,1},{1,1}};
    private WardingOverlay() {}
    /** Asset numbers are one-based; original wardedGlassIcon[0] is warded_glass_1. */
    public static int texture(int mask) {
        if (mask < 0 || mask > 255) throw new IllegalArgumentException("Ward neighbour mask");
        return TEXTURE[mask] + 1;
    }
    public static BlockPos neighbour(BlockPos pos, Direction face, int index) {
        if (index < 0 || index > 7) throw new IllegalArgumentException("Ward neighbour index");
        int a = PLANE[index][0], b = PLANE[index][1];
        return switch (face) {
            case DOWN, UP -> pos.offset(a, 0, b);
            case NORTH -> pos.offset(-a, -b, 0);
            case SOUTH -> pos.offset(a, -b, 0);
            case WEST -> pos.offset(0, -b, a);
            case EAST -> pos.offset(0, -b, -a);
        };
    }
    public static int mask(BlockGetter level, BlockPos pos, Direction face, UUID owner) {
        int mask = 0;
        for (int index = 0; index < 8; index++) {
            BlockPos next = neighbour(pos, face, index);
            if (level.getBlockState(next).getBlock() instanceof BlockWarded
                    && level.getBlockEntity(next) instanceof TileWarded ward && ward.ownedBy(owner)) mask |= 1 << index;
        }
        return mask;
    }
    /** Original hides internal wrapper faces even when the adjacent wrapper has a different owner. */
    public static boolean visible(BlockGetter level, BlockPos pos, Direction face) {
        return !(level.getBlockState(pos.relative(face)).getBlock() instanceof BlockWarded);
    }
}
