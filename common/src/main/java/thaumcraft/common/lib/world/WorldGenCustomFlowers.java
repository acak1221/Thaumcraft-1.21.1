package thaumcraft.common.lib.world;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Original18 attempts/6 integer draws each; no native chunk forcing or fake vanilla flowers. */
public final class WorldGenCustomFlowers {
    private WorldGenCustomFlowers() {}
    public static int place(WorldGenLevel level, RandomSource random, BlockPos origin, Block flower) {
        int placed = 0;
        for (int i = 0; i < 18; i++) {
            BlockPos pos = candidate(random, origin);
            if (!available(level, pos) || !available(level, pos.below())) continue;
            if (level.isEmptyBlock(pos) && soil(level.getBlockState(pos.below()))) {
                level.setBlock(pos, flower.defaultBlockState(), Block.UPDATE_ALL);
                placed++;
            }
        }
        return placed;
    }
    static void plan(MagicalTreePlacement tree, RandomSource random, BlockPos origin, Block flower) {
        for (int i = 0; i < 18; i++) {
            BlockPos pos = candidate(random, origin);
            if (tree.available(pos) && tree.available(pos.below()) && tree.state(pos).isAir() && soil(tree.state(pos.below()))) {
                tree.put(pos, flower.defaultBlockState());
            }
        }
    }
    private static BlockPos candidate(RandomSource random, BlockPos origin) {
        int x = random.nextInt(8) - random.nextInt(8), y = random.nextInt(4) - random.nextInt(4), z = random.nextInt(8) - random.nextInt(8);
        return origin.offset(x, y, z);
    }
    private static boolean available(WorldGenLevel level, BlockPos pos) { return !level.isOutsideBuildHeight(pos) && level.hasChunkAt(pos) && WorldGenWriteArea.canWrite(level, pos); }
    private static boolean soil(BlockState state) { return state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.SAND) || state.is(Blocks.RED_SAND); }
}
