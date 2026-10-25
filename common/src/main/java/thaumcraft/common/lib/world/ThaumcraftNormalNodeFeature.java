package thaumcraft.common.lib.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
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
        if (!AuraDimensionRules.supportsWildNodes(level.getLevel().dimension())) return false;
        // Oracle ThaumcraftWorldGenerator:189-215 - the whole wild node pass of the original
        // draws from the random of the chunk (the random of the chunk provider of 1.7.10),
        // so the port draws it from the random of the chunk seed. The rarity roll of the
        // pass is the first draw of that stream, and the totem of the chunk reads the same
        // stream, so the two are exclusive the way the auraGen flag of the original makes
        // them exclusive (oracle :419 and :455).
        int chunkX = context.origin().getX() >> 4;
        int chunkZ = context.origin().getZ() >> 4;
        RandomSource random = RandomSource.create(
                StructurePlacementRules.chunkSeed(level.getLevel().getSeed(), chunkX, chunkZ));
        // Oracle :130-132,548-555: Nether has only the wild pass, no scattered temples.
        return level.getLevel().dimension().equals(Level.NETHER)
                ? placeWildNode(level, random, chunkX, chunkZ, false)
                : placeAuraPass(level, random, chunkX, chunkZ);
    }

    /**
     * Oracle ThaumcraftWorldGenerator:411-420 - the whole aura branch of the original: the node
     * of the scattered feature first, then the wild node pass, whose return value overwrites the
     * scattered flag.
     *
     * <p>The original runs this branch behind {@code Config.genAura && (newGen ||
     * Config.regenAura)}; the caller owns that gate, so the fresh-generation feature and the
     * regeneration queue replay exactly the same body.</p>
     *
     * @return the {@code auraGen} value of the original, the return of the wild node pass
     */
    public static boolean placeAuraPass(WorldGenLevel level, RandomSource random, int chunkX, int chunkZ) {
        if (!AuraDimensionRules.supportsWildNodes(level.getLevel().dimension())) return false;
        boolean scatteredPlaced = false;
        // Oracle ThaumcraftWorldGenerator:413-418 - the node of a vanilla scattered feature sits
        // three blocks above the surface of its centre.
        AuraGenRules.Temple temple = AuraGenRules.scatteredTemple(level, chunkX, chunkZ);
        if (temple != null) {
            scatteredPlaced = true;
            int x = temple.centre().getX();
            int z = temple.centre().getZ();
            int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) + 3;
            createRandomNodeAt(level, new BlockPos(x, y, z), random);
        }
        // Oracle ThaumcraftWorldGenerator:419 - the wild node pass overwrites the flag.
        return AuraGenRules.auraGenAfterWildPass(scatteredPlaced,
                placeWildNode(level, random, chunkX, chunkZ, scatteredPlaced));
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
     *
     * <p>The rarity draw happens before the {@code !auraGen} test of the original, so a chunk
     * that already carries a scattered node still consumes the draw. The returned flag is the
     * flag of the original: it is true when the roll hit and the height was valid, even if the
     * biome of the node refused it afterwards ({@code createRandomNodeAt} is {@code void} in
     * the original).</p>
     *
     * @param auraGen the flag the scattered branch left behind
     * @return the value the original returns from {@code generateWildNodes}
     */
    public static boolean placeWildNode(WorldGenLevel level, RandomSource random, int chunkX, int chunkZ,
                                        boolean auraGen) {
        if (!AuraDimensionRules.supportsWildNodes(level.getLevel().dimension())) return false;
        // Oracle ThaumcraftWorldGenerator:190 - the rarity roll of the original.
        if (random.nextInt(NODE_RARITY) != 0) return false;
        if (auraGen) return false;
        // Oracle ThaumcraftWorldGenerator:193-194 - the place of the node inside the chunk.
        int x = chunkX * 16 + random.nextInt(16);
        int z = chunkZ * 16 + random.nextInt(16);
        int y = wildNodeY(level, random, x, z);
        if (y < 0) return false;
        createRandomNodeAt(level, new BlockPos(x, y, z), random);
        return true;
    }

    /**
     * The decision of {@link #placeWildNode} without placing the node, for the aura totem of the
     * same chunk.
     *
     * <p>The totem runs as a separate placed feature, so it replays the draws of the wild pass
     * from the same chunk seed and only needs the resulting flag. The draws are identical to the
     * ones of the real pass: the rarity, the place inside the chunk and the height draw.</p>
     */
    public static boolean wildNodeDecision(WorldGenLevel level, RandomSource random, int chunkX, int chunkZ,
                                           boolean auraGen) {
        if (!level.getLevel().dimension().equals(Level.OVERWORLD)
                || !AuraDimensionRules.supportsWildNodes(level.getLevel().dimension())) return false;
        if (random.nextInt(NODE_RARITY) != 0) return false;
        if (auraGen) return false;
        int x = chunkX * 16 + random.nextInt(16);
        int z = chunkZ * 16 + random.nextInt(16);
        return wildNodeY(level, random, x, z) >= 0;
    }

    /** Actual ordinary-wild invocation of the shared original all-type factory. */
    public static boolean createRandomNodeAt(WorldGenLevel level, BlockPos pos, RandomSource random) {
        if (!AuraDimensionRules.supportsWildNodes(level.getLevel().dimension())
                || level.isOutsideBuildHeight(pos) || !level.hasChunkAt(pos) || !WorldGenWriteArea.canWrite(level, pos)) return false;
        Holder<Biome> biome = BiomeColumns.surfaceHolder(level, pos.getX(), pos.getZ());
        if (biome == null || !biome.is(ThaumcraftNodes.NODE_BIOMES)) return false;
        return NodeGeneration.createRandomNodeAt(level, pos, random, false, false, false);
    }

}
