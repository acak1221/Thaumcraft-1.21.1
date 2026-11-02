package thaumcraft.common.lib.world;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.structure.templatesystem.TagMatchTest;
import thaumcraft.common.config.ThaumcraftOres;
import thaumcraft.common.config.ThaumcraftBiomeSettings;

/** Source: TC4 ThaumcraftWorldGenerator:481-546; one invocation is one chunk's complete ore pass. */
public final class ThaumcraftOreFeature extends Feature<NoneFeatureConfiguration> {
    // The original stone host keeps its own datapack extension point. The
    // owner-requested deepslate variants use vanilla's separate host tag.
    public static final TagKey<Block> REPLACEABLE = TagKey.create(Registries.BLOCK, id("ore_replaceables"));
    public static final TagKey<Biome> BIOME_BLACKLIST = TagKey.create(Registries.BIOME, id("ore_generation_blacklist"));
    private static final ResourceLocation OUTER_LANDS = id("outer_lands");
    private static final List<String> PRIMALS = List.of("aer", "ignis", "aqua", "terra", "ordo", "perditio");

    // Config.registerBiomes:782-812, in source order. A type has one vote even
    // when several types have the same aspect. 0 = null/MAGICAL (reroll);
    // -1 = non-primal (retain the initial uniform primal, NOT reroll by depth).
    private static final List<BiomeType> BIOME_TYPES = List.of(
            type("water", 3), type("ocean", 3), type("river", 3), type("wet", 3),
            type("hot", 2), type("desert", 2), type("nether", 2), type("mesa", 2),
            type("dense", 5), type("snowy", 5), type("cold", 5), type("frozen", 5), type("mushroom", 5),
            type("coniferous", 4), type("forest", 4), type("sandy", 4), type("beach", 4),
            type("savanna", 1), type("mountain", 1), type("hills", 1), type("plains", 1),
            type("dry", 6), type("sparse", 6), type("swamp", 6), type("wasteland", 6),
            type("jungle", -1), type("lush", -1), type("magical", 0), type("end", -1),
            type("spooky", -1), type("dead", -1));

    public ThaumcraftOreFeature() { super(NoneFeatureConfiguration.CODEC); }

    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        var options = ThaumcraftBiomeSettings.worldgen();
        return placePass(level, context.random(), context.origin().getX() >> 4, context.origin().getZ() >> 4,
                options.cinnabarOre(), options.amberOre(), options.infusedStone());
    }

    /**
     * Oracle ThaumcraftWorldGenerator:481-546 - one chunk's complete ore pass.
     *
     * <p>The original runs the pass unconditionally from {@code generateSurface:421-423} and
     * gates each ore type inside it with {@code Config.genX && (newGen || Config.regenX)}. The
     * fresh generation passes the {@code Config.genX} values, the regeneration queue passes
     * {@code Config.genX && Config.regenX}, and both replay exactly the same draws.</p>
     */
    public static boolean placePass(WorldGenLevel level, RandomSource random, int chunkX, int chunkZ,
                                    boolean cinnabar, boolean amber, boolean infused) {
        if (!isSurfaceDimension(level) || !NodeEcologyPolicy.permitsLevel(NodeEcologyPolicy.dimensionLevel(level.getLevel().dimension()))) return false;
        if (!level.hasChunk(chunkX, chunkZ)) return false;
        int originX = chunkX << 4;
        int originZ = chunkZ << 4;
        int centerSurface = surface(level, originX + 8, originZ + 8);
        var centreBiome = surfaceBiome(level, originX + 8, centerSurface, originZ + 8);
        if (centreBiome.is(BIOME_BLACKLIST) || !NodeEcologyPolicy.permitsLevel(NodeEcologyPolicy.biomeLevel(centreBiome))) return false;
        boolean placed = false;

        /* Native-height adaptation is deliberately ZERO-ANCHORED, as in TC4:
         * cinnabar nextInt(actualHeight/5), infused nextInt(max(5,surface-5)).
         * actualHeight is 1.21's level.getHeight() (384 in vanilla Overworld),
         * so cinnabar is Y 0..75, not -64..11 and not artificially capped at 50.
         * WORLD_SURFACE is the live first-free surface Y, including water/leaves.
         * Amber can go below zero in low terrain. Out-of-world samples are
         * skipped, never clamped; an exotic height<5 uses bound1, never bound0.
         * These original stone draws stay unchanged. The owner-requested
         * deepslate adaptation appends its own negative-height pass below.
         */
        int cinnabarBound = Math.max(1, level.getHeight() / 5);
        if (cinnabar) for (int attempt = 0; attempt < 18; attempt++) {
            int x = originX + random.nextInt(16);
            int y = random.nextInt(cinnabarBound);
            int z = originZ + random.nextInt(16);
            placed |= single(level, random, new BlockPos(x, y, z), ThaumcraftOres.CINNABAR.get().defaultBlockState());
        }
        if (amber) for (int attempt = 0; attempt < 20; attempt++) {
            int x = originX + random.nextInt(16);
            int z = originZ + random.nextInt(16);
            int y = surface(level, x, z) - random.nextInt(25);
            placed |= single(level, random, new BlockPos(x, y, z), ThaumcraftOres.AMBER.get().defaultBlockState());
        }
        if (infused) for (int attempt = 0; attempt < 8; attempt++) {
            int x = originX + random.nextInt(16);
            int z = originZ + random.nextInt(16);
            int top = surface(level, x, z);
            int y = random.nextInt(Math.max(5, top - 5));
            int variant = infusedVariant(level, random, x, top, z);
            placed |= infusedVein(level, random, new BlockPos(x, y, z), variant, false);
        }
        // MC 1.21's deep underground has no counterpart in the zero-anchored
        // original. A separate eight-vein pass gives it the same six aspects
        // without moving/duplicating the original stone pass or touching old
        // chunks unless the existing explicit regeneration option runs it.
        int deepBottom = level.getMinBuildHeight();
        int deepTop = Math.min(0, level.getMaxBuildHeight());
        if (infused && deepBottom < deepTop) for (int attempt = 0; attempt < 8; attempt++) {
            int x = originX + random.nextInt(16);
            int z = originZ + random.nextInt(16);
            int top = surface(level, x, z);
            int y = deepBottom + random.nextInt(deepTop - deepBottom);
            int variant = infusedVariant(level, random, x, top, z);
            placed |= infusedVein(level, random, new BlockPos(x, y, z), variant, true);
        }
        return placed;
    }

    private static int infusedVariant(WorldGenLevel level, RandomSource random, int x, int top, int z) {
        int variant = random.nextInt(6) + 1;
        return random.nextInt(3) == 0 ? biomeVariant(surfaceBiome(level, x, top, z), random, variant) : variant;
    }

    private static boolean infusedVein(WorldGenLevel level, RandomSource random, BlockPos pos, int variant, boolean deepOnly) {
        // Vanilla size6 OreFeature reads a small surrounding heightmap and
        // section area. Preflight without loading chunks or logging failed far
        // writes. A vein may still cross into the permitted neighbouring chunk.
        BlockPos min = pos.offset(-3, 0, -3), max = pos.offset(3, 0, 3);
        if (level.isOutsideBuildHeight(pos) || !level.hasChunksAt(min, max)
                || !WorldGenWriteArea.canWrite(level, min) || !WorldGenWriteArea.canWrite(level, max)) return false;
        String tag = PRIMALS.get(variant - 1);
        var deep = OreConfiguration.target(new TagMatchTest(BlockTags.DEEPSLATE_ORE_REPLACEABLES),
                ThaumcraftOres.DEEPSLATE_INFUSED.get(tag).get().defaultBlockState());
        var targets = deepOnly ? List.of(deep) : List.of(
                OreConfiguration.target(new TagMatchTest(REPLACEABLE), ThaumcraftOres.INFUSED.get(tag).get().defaultBlockState()), deep);
        return Feature.ORE.place(new OreConfiguration(targets, 6),
                level, level.getLevel().getChunkSource().getGenerator(), random, pos);
    }

    public static boolean isSurfaceDimension(WorldGenLevel level) {
        var dimension = level.getLevel().dimension();
        if (dimension.equals(Level.NETHER) || dimension.equals(Level.END) || dimension.location().equals(OUTER_LANDS)) return false;
        // 1.7's default dimension branch covered modded surface providers too.
        // Native adapter: skylit, non-ceiling dimensions qualify, not only the
        // Overworld key or vanilla biome IDs. Loader hooks must inject once in
        // eligible surface biomes; this final guard also protects direct /place.
        return level.dimensionType().hasSkyLight() && !level.dimensionType().hasCeiling();
    }

    private static boolean single(WorldGenLevel level, RandomSource random, BlockPos pos, BlockState state) {
        if (level.isOutsideBuildHeight(pos) || !level.hasChunkAt(pos) || !WorldGenWriteArea.canWrite(level, pos)) return false;
        if (!new TagMatchTest(REPLACEABLE).test(level.getBlockState(pos), random)) return false;
        return level.setBlock(pos, state, Block.UPDATE_CLIENTS);
    }

    private static int surface(WorldGenLevel level, int x, int z) {
        return level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
    }

    private static Holder<Biome> surfaceBiome(WorldGenLevel level, int x, int top, int z) {
        // TC4 biomes were 2D. Do not bias a surface vein using a 1.21 cave biome.
        int y = Math.max(level.getMinBuildHeight(), Math.min(top, level.getMaxBuildHeight() - 1));
        return level.getBiome(new BlockPos(x, y, z));
    }

    private static int biomeVariant(Holder<Biome> biome, RandomSource random, int initial) {
        List<BiomeType> types = new ArrayList<>();
        for (BiomeType type : BIOME_TYPES) if (biome.is(type.tag)) types.add(type);
        // Forge BiomeDictionary no longer exists. ore_biome_types/* adapts its
        // original types to vanilla tags plus optional shared c: tags. Unknown
        // modded biomes retain all six uniform choices, just like a null lookup.
        if (types.isEmpty()) return 1 + random.nextInt(6);
        int chosen = types.get(random.nextInt(types.size())).variant;
        return chosen == 0 ? 1 + random.nextInt(6) : chosen < 0 ? initial : chosen;
    }

    private static BiomeType type(String name, int variant) {
        return new BiomeType(TagKey.create(Registries.BIOME, id("ore_biome_types/" + name)), variant);
    }
    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("thaumcraft", path); }
    private record BiomeType(TagKey<Biome> tag, int variant) {}
}
