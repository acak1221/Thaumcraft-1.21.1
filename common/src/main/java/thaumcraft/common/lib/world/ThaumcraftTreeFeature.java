package thaumcraft.common.lib.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import thaumcraft.common.config.ThaumcraftPlants;
import thaumcraft.common.config.ThaumcraftBiomeSettings;
import net.minecraft.world.level.levelgen.FlatLevelSource;

/** One chunk's tree pass, from ThaumcraftWorldGenerator:459-472,563-584. */
public final class ThaumcraftTreeFeature extends Feature<NoneFeatureConfiguration> {
    public static final TagKey<Biome> BLACKLIST = TagKey.create(Registries.BIOME,
            ResourceLocation.fromNamespaceAndPath("thaumcraft", "magical_tree_blacklist"));
    private static final TagKey<Biome> CINDERPEARL = TagKey.create(Registries.BIOME,
            ResourceLocation.fromNamespaceAndPath("thaumcraft", "cinderpearl_biomes"));

    public ThaumcraftTreeFeature() { super(NoneFeatureConfiguration.CODEC); }

    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        if (!ThaumcraftBiomeSettings.current().generateTrees()) return false;
        return placePass(context.level(), context.random(),
                context.origin().getX() >> 4, context.origin().getZ() >> 4);
    }

    /**
     * Oracle ThaumcraftWorldGenerator:459-479 - one chunk's complete vegetation pass.
     *
     * <p>The original runs the pass behind {@code Config.genTrees && (newGen ||
     * Config.regenTrees)}. The fresh generation passes the {@code Config.genTrees} gate, the
     * regeneration queue passes {@code Config.genTrees && Config.regenTrees}, and both replay
     * exactly the same draws.</p>
     */
    public static boolean placePass(WorldGenLevel level, RandomSource random, int chunkX, int chunkZ) {
        if (!surfaceDimension(level)||NodeEcologyPolicy.dimensionLevel(level.getLevel().dimension())!=-1
                || level.getLevel().getChunkSource().getGenerator() instanceof FlatLevelSource) return false;
        int originX = chunkX << 4;
        int originZ = chunkZ << 4;
        if (!level.hasChunkAt(new BlockPos(originX + 8, level.getMinBuildHeight(), originZ + 8))) return false;
        int top = level.getHeight(Heightmap.Types.WORLD_SURFACE, originX + 8, originZ + 8);
        if (biome(level, originX + 8, top, originZ + 8).is(BLACKLIST)) return false;
        boolean placed = false;
        // Not two independent placed features: silverwood generation consumes
        // this stream BEFORE the greatwood rarity draw, exactly as in TC4.
        if (random.nextInt(60) == 3) {
            int x = originX + random.nextInt(16);
            int z = originZ + random.nextInt(16);
            int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
            if (MagicalTreeBiomeAdapter.silverwood(biome(level, x, y, z))) {
                placed = new WorldGenSilverwoodTrees(false, 7, 4).generate(level, random, new BlockPos(x, y, z));
            }
        }
        if (random.nextInt(25) == 7) {
            int x = originX + random.nextInt(16);
            int z = originZ + random.nextInt(16);
            int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
            if (MagicalTreeBiomeAdapter.greatwoodChance(biome(level, x, y, z)) > random.nextFloat()) {
                placed |= new WorldGenGreatwoodTrees(false).generate(level, random, new BlockPos(x, y, z), random.nextInt(8) == 0);
            }
        }
        // Original vegetation pass:473-477 samples x/z even outside deserts;
        // only hot sand-top biomes consume the one-in30 flower draw. The native
        // tag explicitly maps vanilla desert and permits a declared addon mapping.
        int flowerX = originX + random.nextInt(16), flowerZ = originZ + random.nextInt(16);
        int flowerY = level.getHeight(Heightmap.Types.WORLD_SURFACE, flowerX, flowerZ);
        var flowerBiome = biome(level, flowerX, flowerY, flowerZ);
        if (flowerY < level.getMaxBuildHeight() && flowerBiome.is(CINDERPEARL) && flowerBiome.value().getBaseTemperature() > 1 && random.nextInt(30) == 0) {
            placed |= WorldGenCustomFlowers.place(level, random, new BlockPos(flowerX, flowerY, flowerZ), ThaumcraftPlants.CINDERPEARL.get()) > 0;
        }
        return placed;
    }

    static Holder<Biome> biome(WorldGenLevel level, int x, int y, int z) {
        // TC4's 2D biome at the surface, not a modern cave-biome sample.
        return level.getBiome(new BlockPos(x, Math.max(level.getMinBuildHeight(), Math.min(y, level.getMaxBuildHeight() - 1)), z));
    }

    private static boolean surfaceDimension(WorldGenLevel level) {
        var key = level.getLevel().dimension();
        return !key.equals(Level.NETHER) && !key.equals(Level.END)
                && !key.location().equals(ResourceLocation.fromNamespaceAndPath("thaumcraft", "outer_lands"))
                && level.dimensionType().hasSkyLight() && !level.dimensionType().hasCeiling();
    }
}
