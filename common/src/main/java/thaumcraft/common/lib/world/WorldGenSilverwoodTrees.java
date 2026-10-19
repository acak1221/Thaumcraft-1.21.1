package thaumcraft.common.lib.world;

import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.config.ThaumcraftNaturals;
import thaumcraft.common.config.ThaumcraftPlants;

/** Actual TC4 WorldGenSilverwoodTrees:31-164 (NOT WorldGenSilverwoodTreesOld). */
public final class WorldGenSilverwoodTrees {
    private static final AtomicLong SKIPPED_PURE_KNOTS = new AtomicLong();
    private static final AtomicLong SKIPPED_KNOT_TREES = new AtomicLong();
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
        boolean pureKnot = false;
        for (int yy = 0; yy < height; yy++) {
            BlockPos pos = origin.above(yy);
            if (!MagicalTreePlacement.replaceableByLeaves(tree.state(pos))) continue;
            if (yy > 0 && !lastBlock && random.nextInt(chance) == 0) {
                // No NORMAL node and no inert/counterfeit PURE knot is placed.
                // Plan the wood-equivalent non-foliage state ONLY for the RNG
                // environment calculation; the ENTIRE transaction is rejected
                // below when any knot was selected. Never retry inside a roll.
                log(tree, pos, Direction.Axis.Y);
                consumePureNodeRoll(tree, pos, random);
                pureKnot = true;
                SKIPPED_PURE_KNOTS.incrementAndGet();
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
        if (pureKnot) {
            if (SKIPPED_KNOT_TREES.getAndIncrement() == 0) Thaumcraft.LOG.warn(
                    "Silverwood PURE-knot ecology is pending: knot-bearing tree candidate skipped atomically, without NORMAL-node/log substitution or reroll; saplings are restored");
            return false;
        }
        return tree.finish();
    }

    public static long skippedPureKnots() { return SKIPPED_PURE_KNOTS.get(); }
    public static long skippedKnotTrees() { return SKIPPED_KNOT_TREES.get(); }

    private static void log(MagicalTreePlacement tree, BlockPos pos, Direction.Axis axis) {
        tree.put(pos, ThaumcraftNaturals.SILVERWOOD_LOG.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, axis));
    }

    /**
     * Consume createRandomNodeAt(silverwood=true):218-387 before continuing the
     * trunk stream. This is RNG accounting, NOT an implementation of a PURE node.
     * Biome types/environment use the declared native adapter, so cross-version
     * seed identity is deliberately not claimed. No node block/entity is touched.
     */
    private static void consumePureNodeRoll(MagicalTreePlacement tree, BlockPos pos, RandomSource random) {
        if (random.nextInt(9) == 0) random.nextInt(3); // Config.specialNodeRarity=18
        int surface = tree.level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, pos.getX(), pos.getZ());
        var biome = MagicalTreeBiomeAdapter.describe(ThaumcraftTreeFeature.biome(tree.level, pos.getX(), surface, pos.getZ()));
        int aura = biome.aura() / 4;
        random.nextInt(Math.max(1, aura / 2));
        AspectList aspects = new AspectList();
        Aspect tag = biome.randomTag(random);
        var complex = Aspect.getCompoundAspects();
        var basic = Aspect.getPrimalAspects();
        if (tag != null) aspects.add(tag, 2);
        else {
            aspects.add(complex.get(random.nextInt(complex.size())), 1);
            aspects.add(basic.get(random.nextInt(basic.size())), 1);
        }
        for (int i = 0; i < 3; i++) if (random.nextBoolean()) {
            var pool = random.nextInt(18) == 0 ? complex : basic;
            aspects.merge(pool.get(random.nextInt(pool.size())), 1);
        }
        aspects.merge(random.nextBoolean() ? Aspect.LIFE : Aspect.ORDER, 2);
        int water = 0, lava = 0, stone = 0, foliage = 0;
        for (BlockPos sample : BlockPos.betweenClosed(pos.offset(-5, -5, -5), pos.offset(5, 5, 5))) {
            if (!tree.available(sample)) continue;
            BlockState state = tree.state(sample);
            if (state.getFluidState().is(FluidTags.WATER)) ++water;
            else if (state.getFluidState().is(FluidTags.LAVA)) ++lava;
            else if (state.is(Blocks.STONE)) ++stone;
            if (state.is(BlockTags.LEAVES)) ++foliage;
        }
        if (water > 100) aspects.merge(Aspect.WATER, 1);
        if (lava > 100) { aspects.merge(Aspect.FIRE, 1); aspects.merge(Aspect.EARTH, 1); }
        if (stone > 500) aspects.merge(Aspect.EARTH, 1);
        if (foliage > 100) aspects.merge(Aspect.PLANT, 1);
        for (Aspect aspect : aspects.getAspectsSorted()) {
            random.nextInt(aspects.getAmount(aspect) == 2 ? 25 : 50);
        }
    }
}
