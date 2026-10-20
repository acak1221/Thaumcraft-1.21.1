package thaumcraft.common.lib.world;

import java.util.concurrent.atomic.AtomicLongArray;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftNodes;
import thaumcraft.common.tiles.TileNode;
import thaumcraft.common.tiles.TileNode.NodeModifier;
import thaumcraft.common.tiles.TileNode.NodeType;

/**
 * Original ThaumcraftWorldGenerator.generateWildNodes:189-215 and
 * createRandomNodeAt:218-399, restricted to naturally rolled NORMAL and UNSTABLE nodes.
 * One invocation is ONE chunk attempt, so the placed feature has no count,
 * rarity or in-square modifier. Other special rolls are skipped, never rerolled.
 */
public final class ThaumcraftNormalNodeFeature extends Feature<NoneFeatureConfiguration> {
    public static final int NODE_RARITY = 36; // original Config defaults:872-873
    public static final int SPECIAL_NODE_RARITY = 18;
    private static final AtomicLongArray SKIPPED_TYPES = new AtomicLongArray(NodeType.values().length);

    public ThaumcraftNormalNodeFeature() { super(NoneFeatureConfiguration.CODEC); }

    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        // This minimum unit intentionally registers only ordinary Overworld
        // nodes; Nether, End and custom-dimension ecology remain pending.
        if (!level.getLevel().dimension().equals(Level.OVERWORLD)) return false;
        RandomSource random = context.random();
        if (random.nextInt(NODE_RARITY) != 0) return false;
        int x = (context.origin().getX() & ~15) + random.nextInt(16);
        int z = (context.origin().getZ() & ~15) + random.nextInt(16);
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

    /** Original non-silverwood, non-eerie, non-small branch; also usable by runtime checks. */
    public static boolean createRandomNodeAt(WorldGenLevel level, BlockPos pos, RandomSource random) {
        if (!level.getLevel().dimension().equals(Level.OVERWORLD)
                || level.isOutsideBuildHeight(pos) || !level.hasChunkAt(pos) || !level.ensureCanWrite(pos)
                || !level.isEmptyBlock(pos)) return false;
        Holder<Biome> biome = surfaceBiome(level, pos);
        if (!biome.is(ThaumcraftNodes.NODE_BIOMES)) return false;
        NodeType type = NodeType.NORMAL;
        if (random.nextInt(SPECIAL_NODE_RARITY) == 0) {
            int choice = random.nextInt(10);
            type = choice < 3 ? NodeType.DARK : choice < 6 ? NodeType.UNSTABLE : choice < 9 ? NodeType.PURE : NodeType.HUNGRY;
        }
        NodeModifier modifier = null;
        if (random.nextInt(SPECIAL_NODE_RARITY / 2) == 0) modifier = NodeModifier.values()[random.nextInt(3)];
        if (type != NodeType.NORMAL && type != NodeType.UNSTABLE) {
            // Aggregate every skip, but warn only once per type, not per chunk.
            if (SKIPPED_TYPES.getAndIncrement(type.ordinal()) == 0) {
                Thaumcraft.LOG.warn("Natural aura nodes: rolled {} is pending and skipped without replacement; only NORMAL/UNSTABLE ecology is implemented", type);
            }
            return false;
        }

        var biomeInfo = NormalNodeBiomeAdapter.describe(biome);
        int value = random.nextInt(biomeInfo.aura() / 2) + biomeInfo.aura() / 2;
        AspectList aspects = new AspectList();
        Aspect biomeAspect = biomeInfo.randomTag(random);
        var basic = Aspect.getPrimalAspects();
        var complex = Aspect.getCompoundAspects();
        if (biomeAspect != null) aspects.add(biomeAspect, 2);
        else {
            aspects.add(complex.get(random.nextInt(complex.size())), 1);
            aspects.add(basic.get(random.nextInt(basic.size())), 1);
        }
        for (int i = 0; i < 3; i++) if (random.nextBoolean()) {
            var pool = random.nextInt(SPECIAL_NODE_RARITY) == 0 ? complex : basic;
            aspects.merge(pool.get(random.nextInt(pool.size())), 1);
        }
        addEnvironmentalAspects(level, pos, aspects);
        Aspect[] sorted = aspects.getAspectsSorted();
        int[] spread = new int[sorted.length];
        float total = 0;
        for (int i = 0; i < spread.length; i++) {
            spread[i] = aspects.getAmount(sorted[i]) == 2 ? 50 + random.nextInt(25) : 25 + random.nextInt(50);
            total += spread[i];
        }
        for (int i = 0; i < spread.length; i++) {
            aspects.merge(sorted[i], (int)(spread[i] / total * value));
        }
        if (!level.setBlock(pos, ThaumcraftContent.AURA_NODE.get().defaultBlockState(), Block.UPDATE_CLIENTS)) return false;
        if (level.getBlockEntity(pos) instanceof TileNode node) {
            node.initializeNatural(aspects, type, modifier);
            return true;
        }
        // A node without its state must never survive as the 25-each fixture.
        level.removeBlock(pos, false);
        return false;
    }

    public static long skippedSpecialNodes(NodeType type) { return SKIPPED_TYPES.get(type.ordinal()); }

    private static Holder<Biome> surfaceBiome(WorldGenLevel level, BlockPos pos) {
        // Old biomes were 2D. Sample surface climate, not a modern cave biome.
        int top = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, pos.getX(), pos.getZ());
        int y = Math.max(level.getMinBuildHeight(), Math.min(top, level.getMaxBuildHeight() - 1));
        return level.getBiome(new BlockPos(pos.getX(), y, pos.getZ()));
    }

    private static void addEnvironmentalAspects(WorldGenLevel level, BlockPos origin, AspectList aspects) {
        int water = 0, lava = 0, stone = 0, foliage = 0;
        BlockPos.MutableBlockPos sample = new BlockPos.MutableBlockPos();
        for (int x = -5; x <= 5; x++) for (int y = -5; y <= 5; y++) for (int z = -5; z <= 5; z++) {
            sample.setWithOffset(origin, x, y, z);
            // Read only the existing generation region; never request/load a
            // remote chunk for the original 11-cube environment calculation.
            if (level.isOutsideBuildHeight(sample) || !level.hasChunkAt(sample)) continue;
            var state = level.getBlockState(sample);
            if (state.getFluidState().is(FluidTags.WATER)) ++water;
            else if (state.getFluidState().is(FluidTags.LAVA)) ++lava;
            else if (state.is(Blocks.STONE)) ++stone;
            // Forge isFoliage -> native leaves. Deepslate is intentionally NOT
            // counted as legacy stone; this is an explicit 1.21 adaptation.
            if (state.is(BlockTags.LEAVES)) ++foliage;
        }
        if (water > 100) aspects.merge(Aspect.WATER, 1);
        if (lava > 100) { aspects.merge(Aspect.FIRE, 1); aspects.merge(Aspect.EARTH, 1); }
        if (stone > 500) aspects.merge(Aspect.EARTH, 1);
        if (foliage > 100) aspects.merge(Aspect.PLANT, 1);
    }
}
