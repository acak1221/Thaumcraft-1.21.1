package thaumcraft.common.lib.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.common.config.ThaumcraftNaturals;
import thaumcraft.common.config.ThaumcraftPlants;

/** Actual TC4 WorldGenSilverwoodTrees:31-164 (NOT WorldGenSilverwoodTreesOld). */
public final class WorldGenSilverwoodTrees {
    private final int minTreeHeight;
    private final int randomTreeHeight;
    private final boolean worldgen;

    public WorldGenSilverwoodTrees(boolean notify, int minTreeHeight, int randomTreeHeight) {
        this.worldgen = !notify;
        this.minTreeHeight = minTreeHeight;
        this.randomTreeHeight = randomTreeHeight;
    }

    public boolean generate(WorldGenLevel level, RandomSource random, BlockPos origin) {
        int height = random.nextInt(randomTreeHeight) + minTreeHeight;
        int x = origin.getX(), y = origin.getY(), z = origin.getZ();
        MagicalTreePlacement tree = new MagicalTreePlacement(level);
        // Native-height adaptation: legacy [1,256) becomes [min+1,max),
        // including the FULL canopy/root extent, without height rescaling.
        if (y < level.getMinBuildHeight() + 1 || y + height + 1 > level.getMaxBuildHeight()) return false;
        for (int yy = y; yy <= y + 1 + height; yy++) {
            int spread = yy == y ? 0 : yy >= y + 1 + height - 2 ? 3 : 1;
            for (int xx = x - spread; xx <= x + spread; xx++) for (int zz = z - spread; zz <= z + spread; zz++) {
                BlockPos pos = new BlockPos(xx, yy, zz);
                if (!tree.available(pos) || yy > y && !MagicalTreePlacement.replaceableByLeaves(tree.state(pos))) return false;
            }
        }
        if (!MagicalTreePlacement.soil(tree.state(origin.below())) || y >= level.getMaxBuildHeight() - height - 1) return false;
        // Native equivalent of soil.onPlantGrow; never a remote soil write.
        tree.put(origin.below(), Blocks.DIRT.defaultBlockState());
        int start = y + height - 5;
        int end = y + height + 3 + random.nextInt(3);
        if (end >= level.getMaxBuildHeight()) return false;
        for (int yy = start; yy <= end; yy++) {
            int centerY = Mth.clamp(yy, y + height - 3, y + height);
            for (int xx = x - 5; xx <= x + 5; xx++) for (int zz = z - 5; zz <= z + 5; zz++) {
                double dx = xx - x, dy = yy - centerY, dz = zz - z;
                // Draw for EVERY canopy sample, even one outside the sphere.
                if (dx * dx + dy * dy + dz * dz >= 10 + random.nextInt(8)) continue;
                BlockPos pos = new BlockPos(xx, yy, zz);
                if (!tree.available(pos)) return false;
                if (MagicalTreePlacement.replaceableByLeaves(tree.state(pos))) {
                    tree.put(pos, ThaumcraftNaturals.SILVERWOOD_LEAVES.get().defaultBlockState());
                }
            }
        }
        int chance = (int)(height * 1.5);
        boolean lastBlock = false;
        for (int yy = 0; yy < height; yy++) {
            BlockPos pos = origin.above(yy);
            if (!MagicalTreePlacement.replaceableByLeaves(tree.state(pos))) continue;
            if (yy > 0 && !lastBlock && random.nextInt(chance) == 0) {
                tree.put(pos, thaumcraft.common.config.ThaumcraftContent.SILVERWOOD_KNOT.get().defaultBlockState()
                        .setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y));
                var biome = BiomeColumns.surfaceHolder(level, pos.getX(), pos.getZ());
                if (biome == null) return false;
                tree.node(pos, NodeGeneration.plan(biome, pos, random, true, false, false,
                        sample -> tree.available(sample) ? tree.state(sample) : null));
                chance += height;
                lastBlock = true;
            } else {
                log(tree, pos, Direction.Axis.Y);
                lastBlock = false;
            }
            log(tree, pos.west(), Direction.Axis.Y); log(tree, pos.east(), Direction.Axis.Y);
            log(tree, pos.north(), Direction.Axis.Y); log(tree, pos.south(), Direction.Axis.Y);
        }
        log(tree, origin.above(height), Direction.Axis.Y);
        int[][] diagonals = {{-1, -1}, {1, 1}, {-1, 1}, {1, -1}};
        for (int[] corner : diagonals) log(tree, origin.offset(corner[0], 0, corner[1]), Direction.Axis.Y);
        for (int[] corner : diagonals) if (random.nextInt(3) != 0) log(tree, origin.offset(corner[0], 1, corner[1]), Direction.Axis.Y);
        log(tree, origin.west(2), Direction.Axis.X); log(tree, origin.east(2), Direction.Axis.X);
        log(tree, origin.north(2), Direction.Axis.Z); log(tree, origin.south(2), Direction.Axis.Z);
        log(tree, origin.west(2).below(), Direction.Axis.Y); log(tree, origin.east(2).below(), Direction.Axis.Y);
        log(tree, origin.north(2).below(), Direction.Axis.Y); log(tree, origin.south(2).below(), Direction.Axis.Y);
        for (int[] corner : diagonals) log(tree, origin.offset(corner[0], height - 4, corner[1]), Direction.Axis.Y);
        for (int[] corner : diagonals) if (random.nextInt(3) == 0) log(tree, origin.offset(corner[0], height - 5, corner[1]), Direction.Axis.Y);
        log(tree, origin.offset(-2, height - 4, 0), Direction.Axis.X); log(tree, origin.offset(2, height - 4, 0), Direction.Axis.X);
        log(tree, origin.offset(0, height - 4, -2), Direction.Axis.Z); log(tree, origin.offset(0, height - 4, 2), Direction.Axis.Z);
        if (worldgen) {
            WorldGenCustomFlowers.plan(tree, random, origin, ThaumcraftPlants.SHIMMERLEAF.get());
        }
        return tree.finish();
    }

    private static void log(MagicalTreePlacement tree, BlockPos pos, Direction.Axis axis) {
        tree.put(pos, ThaumcraftNaturals.SILVERWOOD_LOG.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, axis));
    }

}
