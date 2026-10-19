package thaumcraft.common.lib.world;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
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

/** Source: TC4 ThaumcraftWorldGenerator:481-546; one invocation is one chunk's complete ore pass. */
public final class ThaumcraftOreFeature extends Feature<NoneFeatureConfiguration> {
    // Default is stone ONLY, like TC4's Blocks.stone target. This datapack tag
    // replaces the old isReplaceableOreGen extension point, not a deepslate pass.
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
        if (!isSurfaceDimension(level)) return false;
        int chunkX = context.origin().getX() & ~15;
        int chunkZ = context.origin().getZ() & ~15;
        if (!level.hasChunk(chunkX >> 4, chunkZ >> 4)) return false;
        int centerSurface = surface(level, chunkX + 8, chunkZ + 8);
        if (surfaceBiome(level, chunkX + 8, centerSurface, chunkZ + 8).is(BIOME_BLACKLIST)) return false;
        RandomSource random = context.random();
        boolean placed = false;

        /* Native-height adaptation is deliberately ZERO-ANCHORED, as in TC4:
         * cinnabar nextInt(actualHeight/5), infused nextInt(max(5,surface-5)).
         * actualHeight is 1.21's level.getHeight() (384 in vanilla Overworld),
         * so cinnabar is Y 0..75, not -64..11 and not artificially capped at 50.
         * WORLD_SURFACE_WG is the first-free surface Y, including water/leaves.
         * Amber can go below zero in low terrain. Out-of-world samples are
         * skipped, never clamped; an exotic height<5 uses bound1, never bound0.
         * No extra deepslate pass, attempt scaling, retrogen or old-seed identity.
         */
        int cinnabarBound = Math.max(1, level.getHeight() / 5);
        for (int attempt = 0; attempt < 18; attempt++) {
            int x = chunkX + random.nextInt(16);
            int y = random.nextInt(cinnabarBound);
            int z = chunkZ + random.nextInt(16);
            placed |= single(level, random, new BlockPos(x, y, z), ThaumcraftOres.CINNABAR.get().defaultBlockState());
        }
        for (int attempt = 0; attempt < 20; attempt++) {
            int x = chunkX + random.nextInt(16);
            int z = chunkZ + random.nextInt(16);
            int y = surface(level, x, z) - random.nextInt(25);
            placed |= single(level, random, new BlockPos(x, y, z), ThaumcraftOres.AMBER.get().defaultBlockState());
        }
        for (int attempt = 0; attempt < 8; attempt++) {
            int x = chunkX + random.nextInt(16);
            int z = chunkZ + random.nextInt(16);
            int top = surface(level, x, z);
            int y = random.nextInt(Math.max(5, top - 5));
            int variant = random.nextInt(6) + 1;
            if (random.nextInt(3) == 0) variant = biomeVariant(surfaceBiome(level, x, top, z), random, variant);
            BlockPos pos = new BlockPos(x, y, z);
            // Vanilla size6 OreFeature reads a small surrounding heightmap and
            // section area. Preflight it without loading chunks; its own writes
            // use ensureCanWrite. Veins may cross chunks in the native region.
            if (level.isOutsideBuildHeight(pos) || !level.hasChunksAt(pos.offset(-3, 0, -3), pos.offset(3, 0, 3))) continue;
            var state = ThaumcraftOres.INFUSED.get(PRIMALS.get(variant - 1)).get().defaultBlockState();
            placed |= Feature.ORE.place(new OreConfiguration(new TagMatchTest(REPLACEABLE), state, 6),
                    level, context.chunkGenerator(), random, pos);
        }
        return placed;
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
        if (level.isOutsideBuildHeight(pos) || !level.hasChunkAt(pos) || !level.ensureCanWrite(pos)) return false;
        if (!new TagMatchTest(REPLACEABLE).test(level.getBlockState(pos), random)) return false;
        return level.setBlock(pos, state, Block.UPDATE_CLIENTS);
    }

    private static int surface(WorldGenLevel level, int x, int z) {
        return level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
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
