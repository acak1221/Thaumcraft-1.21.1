package thaumcraft.common.lib.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import thaumcraft.common.config.ThaumcraftNodes;
import thaumcraft.common.config.ThaumcraftBiomeSettings;

/**
 * Original ThaumcraftWorldGenerator.generateWildNodes:189-215 and
 * createRandomNodeAt:218-399, sharing actual all-type generation with embedded Silverwood and primal impacts.
 *
 * <p>The original calls the wild node pass once per chunk, rolls
 * {@code Config.nodeRarity} (36) for it and draws the place of the node inside the chunk.
 * The native placed feature carries that placement: one attempt per chunk, the rarity
 * filter of the original and the random place of the chunk
 * ({@code data/thaumcraft/worldgen/placed_feature/normal_aura_nodes.json}).
 */
public final class ThaumcraftNormalNodeFeature extends Feature<NoneFeatureConfiguration> {
    /** The original {@code Config.nodeRarity} (oracle Config:872), the rarity the placed feature carries. */
    public static final int NODE_RARITY = StructurePlacementRules.NODE_RARITY; // original Config defaults:872-873
    public static final int SPECIAL_NODE_RARITY = 18;

    public ThaumcraftNormalNodeFeature() { super(NoneFeatureConfiguration.CODEC); }

    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        if (!ThaumcraftBiomeSettings.worldgen().auraNodes()) return false;
        WorldGenLevel level = context.level();
        // This minimum unit intentionally registers only ordinary Overworld
        // nodes; Nether, End and custom-dimension ecology remain pending.
        if (!level.getLevel().dimension().equals(Level.OVERWORLD)) return false;
        RandomSource random = context.random();
        // Oracle ThaumcraftWorldGenerator:190-194 - the rarity roll and the place of the
        // node inside the chunk belong to the placement of the original, so the feature
        // only places the node the placement selected.
        int x = context.origin().getX();
        int z = context.origin().getZ();
        int y = wildNodeY(level, random, x, z);
        if (y < 0) return false;
        return createRandomNodeAt(level, new BlockPos(x, y, z), random);
    }

    /**
     * The height of the wild node of the original (oracle ThaumcraftWorldGenerator:194-209).
     *
     * <p>The original scans for the first uncovered block with
     * {@code Utils.getFirstUncoveredY} (oracle Utils:116-122, the scan starts at the height
     * five), lifts the node by one when the block above is air, draws the lift of the original
     * ({@code nextInt(4)}) and lifts it again when the block it points at is air or
     * replaceable.</p>
     *
     * @return the height of the node, or minus one when the original would not place one
     */
    public static int wildNodeY(WorldGenLevel level, RandomSource random, int x, int z) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, 5, z);
        if (!level.hasChunkAt(pos)) return -1;
        // Utils.getFirstUncoveredY starts at FIVE, not the top heightmap. Retain
        // that zero-anchored first-air/cave distribution in the taller world,
        // but bound the scan by native min/max build height (no infinite loop).
        int q = Math.max(5, level.getMinBuildHeight());
        while (q + 1 < level.getMaxBuildHeight() && !level.isEmptyBlock(pos.set(x, q + 1, z))) ++q;
        if (q + 1 >= level.getMaxBuildHeight()) return -1;
        if (level.isEmptyBlock(pos.set(x, q + 1, z))) ++q;
        int p = random.nextInt(4);
        pos.set(x, q + p, z);
        if (!level.isOutsideBuildHeight(pos)
                && (level.isEmptyBlock(pos) || level.getBlockState(pos).canBeReplaced())) q += p;
        return q;
    }

    /**
     * The wild node pass of the original, replayed for a chunk that exists already.
     *
     * <p>Oracle ThaumcraftWorldGenerator:189-215: the pass draws the rarity of the node
     * ({@code Config.nodeRarity}), the place of the node inside the chunk and then the node
     * itself. The regeneration of the original calls the same pass with {@code newGen = false}
     * (oracle ServerTickEventsFML:96).</p>
     */
    public static boolean placeWildNode(WorldGenLevel level, RandomSource random, int chunkX, int chunkZ) {
        if (!level.getLevel().dimension().equals(Level.OVERWORLD)) return false;
        // Oracle ThaumcraftWorldGenerator:190 - the rarity roll of the original.
        if (random.nextInt(NODE_RARITY) != 0) return false;
        // Oracle ThaumcraftWorldGenerator:193-194 - the place of the node inside the chunk.
        int x = chunkX * 16 + random.nextInt(16);
        int z = chunkZ * 16 + random.nextInt(16);
        int y = wildNodeY(level, random, x, z);
        if (y < 0) return false;
        return createRandomNodeAt(level, new BlockPos(x, y, z), random);
    }

    /** Actual ordinary-wild invocation of the shared original all-type factory. */
    public static boolean createRandomNodeAt(WorldGenLevel level, BlockPos pos, RandomSource random) {
        if (!level.getLevel().dimension().equals(Level.OVERWORLD)
                || level.isOutsideBuildHeight(pos) || !level.hasChunkAt(pos) || !WorldGenWriteArea.canWrite(level, pos)) return false;
        Holder<Biome> biome = BiomeColumns.surfaceHolder(level, pos.getX(), pos.getZ());
        if (biome == null || !biome.is(ThaumcraftNodes.NODE_BIOMES)) return false;
        return NodeGeneration.createRandomNodeAt(level, pos, random, false, false, false);
    }
}
