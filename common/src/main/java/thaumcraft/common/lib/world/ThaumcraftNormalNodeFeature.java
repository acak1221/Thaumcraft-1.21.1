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
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, 5, z);
        if (!level.hasChunkAt(pos)) return false;

        // Utils.getFirstUncoveredY starts at FIVE, not the top heightmap. Retain
        // that zero-anchored first-air/cave distribution in the taller world,
        // but bound the scan by native min/max build height (no infinite loop).
        int q = Math.max(5, level.getMinBuildHeight());
        while (q + 1 < level.getMaxBuildHeight()
                && !level.isEmptyBlock(pos.set(x, q + 1, z))) ++q;
        if (q + 1 >= level.getMaxBuildHeight()) return false;
        if (level.isEmptyBlock(pos.set(x, q + 1, z))) ++q;
        int p = random.nextInt(4);
        pos.set(x, q + p, z);
        if (!level.isOutsideBuildHeight(pos)
                && (level.isEmptyBlock(pos) || level.getBlockState(pos).canBeReplaced())) q += p;
        return createRandomNodeAt(level, new BlockPos(x, q, z), random);
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
