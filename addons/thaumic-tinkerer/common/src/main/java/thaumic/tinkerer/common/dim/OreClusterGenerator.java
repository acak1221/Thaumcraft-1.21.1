/**
 * ThaumicTinkerer is Open Source and distributed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0
 * License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 *
 * Ported to Minecraft 1.21.1 for Thaumcraft 4 Reload (see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.common.dim;

import java.util.Random;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import thaumic.tinkerer.common.core.handler.ConfigHandler;

/**
 * The ore clusters of the Bedrock dimension: {@code density} clusters per chunk, each one ore chosen by weight
 * ({@link EnumOreFrequency}) and generated 200 times at random heights of the same column as a vein of 0 to 19 blocks
 * that replaces bedrock only.
 *
 * <p>1.7.10 ran this from {@code ChunkProviderBedrock.populate}, with Forge's {@code WorldGenMinable} for the veins. Here it
 * runs from {@code ChunkGeneratorBedrock.applyBiomeDecoration} on the {@code WorldGenLevel} of the chunk, and the vein is
 * the 1.7.10 {@code WorldGenMinable.generate} loop, unchanged (the lookup-table {@code sin}, the order of the random
 * draws, the ellipsoid test), behind the small {@link VeinTarget} interface so that it can be tested without a world.
 * A vein starts at 8 to 23 blocks into the chunk's own column and reaches at most four blocks beyond that, so it never
 * leaves the chunk it was started in or the one east and south of it ({@link #reaches}).</p>
 */
public final class OreClusterGenerator {
    /** The number of veins of one cluster (1.7.10: 200). */
    public static final int VEINS_PER_CLUSTER = 200;
    /** Veins are started at heights 6 to 250 (1.7.10: {@code nextInt(245) + 6}). */
    public static final int MIN_VEIN_Y = 6;
    public static final int VEIN_Y_RANGE = 245;
    /** The largest vein, exclusive (1.7.10: {@code nextInt(20)}). */
    public static final int MAX_VEIN_SIZE = 20;

    private OreClusterGenerator() {}

    /** The number of clusters per chunk: the config option {@code bedrockDimension.oreDensity} (1.7.10: {@code density}). */
    public static int density() {
        return ConfigHandler.bedrockOreDensity;
    }

    /** The ores the config keeps out ({@code bedrockDimension.oreBlacklist}). */
    public static String[] blacklist() {
        return ConfigHandler.bedrockOreBlacklist;
    }

    /** What a vein needs from the world: ask whether a block may be replaced, and replace it. */
    public interface VeinTarget {
        /** 1.7.10 {@code isReplaceableOreGen}: true for the block the vein replaces (bedrock) inside the area it may write. */
        boolean isReplaceable(int x, int y, int z);

        void place(int x, int y, int z);
    }

    /**
     * The random of a chunk (1.7.10 {@code ChunkProviderBedrock.populate}): seeded from the world seed alone, two odd
     * multipliers drawn from it, then the chunk coordinates mixed in with the seed.
     */
    public static Random chunkRandom(long worldSeed, int chunkX, int chunkZ) {
        Random random = new Random(worldSeed);
        long i1 = random.nextLong() / 2L * 2L + 1L;
        long j1 = random.nextLong() / 2L * 2L + 1L;
        random.setSeed((long) chunkX * i1 + (long) chunkZ * j1 ^ worldSeed);
        return random;
    }

    /**
     * The clusters of a chunk on a world being generated. Only the area {@code chunkX..chunkX+1} by {@code chunkZ..chunkZ+1}
     * is written (the generation region of the features step allows one chunk of overhang).
     */
    public static void populate(WorldGenLevel level, int chunkX, int chunkZ) {
        int density = density();
        if (density <= 0) return;
        final int minY = level.getMinBuildHeight();
        final int maxY = level.getMaxBuildHeight();
        Random random = chunkRandom(level.getSeed(), chunkX, chunkZ);
        // 1.7.10 drew three numbers here (the position of a lake generator that was never used) before the clusters.
        random.nextInt(16);
        random.nextInt(128);
        random.nextInt(16);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        String[] blacklist = blacklist();
        generate(random, chunkX, chunkZ, density, rnd -> EnumOreFrequency.getRandomOre(rnd, blacklist), new ClusterWorld<BlockState>() {
            @Override
            public boolean isReplaceable(int x, int y, int z) {
                if (y < minY || y >= maxY || !reaches(chunkX, chunkZ, x, z)) return false;
                return level.getBlockState(pos.set(x, y, z)).is(Blocks.BEDROCK);
            }

            @Override
            public void place(int x, int y, int z, BlockState ore) {
                level.setBlock(pos.set(x, y, z), ore, Block.UPDATE_CLIENTS);
            }
        });
    }

    /** What the clusters need from the world: like {@link VeinTarget}, but what is placed is the ore of the cluster. */
    public interface ClusterWorld<T> {
        boolean isReplaceable(int x, int y, int z);

        void place(int x, int y, int z, T ore);
    }

    /**
     * 1.7.10 {@code OreClusterGenerator.generate}: for each of {@code density} clusters pick a column of the chunk and one
     * ore, then 200 times a vein of random size at a random height in that column. The draw order is the original's:
     * x, z, the ore, then for each vein the height, the size and the vein's own draws.
     */
    public static <T> void generate(Random random, int chunkX, int chunkZ, int density, Function<Random, T> orePicker,
            ClusterWorld<T> world) {
        for (int k = 0; k < density; k++) {
            int firstBlockXCoord = 16 * chunkX + random.nextInt(16);
            int firstBlockZCoord = 16 * chunkZ + random.nextInt(16);
            T ore = orePicker.apply(random);
            if (ore == null) continue;
            for (int l = 0; l < VEINS_PER_CLUSTER; l++) {
                int firstBlockYCoord = random.nextInt(VEIN_Y_RANGE) + MIN_VEIN_Y;
                int size = random.nextInt(MAX_VEIN_SIZE);
                generateVein(random, size, firstBlockXCoord, firstBlockYCoord, firstBlockZCoord, new VeinTarget() {
                    @Override
                    public boolean isReplaceable(int x, int y, int z) {
                        return world.isReplaceable(x, y, z);
                    }

                    @Override
                    public void place(int x, int y, int z) {
                        world.place(x, y, z, ore);
                    }
                });
            }
        }
    }

    /** True when the column (x, z) is one the clusters of chunk (chunkX, chunkZ) may write: that chunk and the one east and south. */
    public static boolean reaches(int chunkX, int chunkZ, int x, int z) {
        int cx = x >> 4;
        int cz = z >> 4;
        return cx >= chunkX && cx <= chunkX + 1 && cz >= chunkZ && cz <= chunkZ + 1;
    }

    /**
     * 1.7.10 {@code WorldGenMinable.generate(world, random, x, y, z)} for {@code numberOfBlocks} blocks: a chain of
     * ellipsoids along a line of random direction through (x + 8, y, z + 8), the radius swelling in the middle of the line.
     * Nothing is placed for a size of 0.
     */
    public static void generateVein(Random rand, int numberOfBlocks, int x, int y, int z, VeinTarget target) {
        float f = rand.nextFloat() * (float) Math.PI;
        double d0 = (float) (x + 8) + Mth.sin(f) * (float) numberOfBlocks / 8.0F;
        double d1 = (float) (x + 8) - Mth.sin(f) * (float) numberOfBlocks / 8.0F;
        double d2 = (float) (z + 8) + Mth.cos(f) * (float) numberOfBlocks / 8.0F;
        double d3 = (float) (z + 8) - Mth.cos(f) * (float) numberOfBlocks / 8.0F;
        double d4 = y + rand.nextInt(3) - 2;
        double d5 = y + rand.nextInt(3) - 2;

        for (int l = 0; l <= numberOfBlocks; ++l) {
            double d6 = d0 + (d1 - d0) * (double) l / (double) numberOfBlocks;
            double d7 = d4 + (d5 - d4) * (double) l / (double) numberOfBlocks;
            double d8 = d2 + (d3 - d2) * (double) l / (double) numberOfBlocks;
            double d9 = rand.nextDouble() * (double) numberOfBlocks / 16.0D;
            double d10 = (double) (Mth.sin((float) l * (float) Math.PI / (float) numberOfBlocks) + 1.0F) * d9 + 1.0D;
            double d11 = (double) (Mth.sin((float) l * (float) Math.PI / (float) numberOfBlocks) + 1.0F) * d9 + 1.0D;
            int i1 = Mth.floor(d6 - d10 / 2.0D);
            int j1 = Mth.floor(d7 - d11 / 2.0D);
            int k1 = Mth.floor(d8 - d10 / 2.0D);
            int l1 = Mth.floor(d6 + d10 / 2.0D);
            int i2 = Mth.floor(d7 + d11 / 2.0D);
            int j2 = Mth.floor(d8 + d10 / 2.0D);

            for (int k2 = i1; k2 <= l1; ++k2) {
                double d12 = ((double) k2 + 0.5D - d6) / (d10 / 2.0D);

                if (d12 * d12 < 1.0D) {
                    for (int l2 = j1; l2 <= i2; ++l2) {
                        double d13 = ((double) l2 + 0.5D - d7) / (d11 / 2.0D);

                        if (d12 * d12 + d13 * d13 < 1.0D) {
                            for (int i3 = k1; i3 <= j2; ++i3) {
                                double d14 = ((double) i3 + 0.5D - d8) / (d10 / 2.0D);

                                if (d12 * d12 + d13 * d13 + d14 * d14 < 1.0D && target.isReplaceable(k2, l2, i3)) {
                                    target.place(k2, l2, i3);
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
