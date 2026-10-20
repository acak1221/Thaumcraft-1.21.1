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
        WorldGenLevel level = context.level();
        if (!surfaceDimension(level)||!ThaumcraftBiomeSettings.current().generateTrees()
                || NodeEcologyPolicy.dimensionLevel(level.getLevel().dimension())!=-1
                || level.getLevel().getChunkSource().getGenerator() instanceof FlatLevelSource) return false;
        int chunkX = context.origin().getX() & ~15;
        int chunkZ = context.origin().getZ() & ~15;
        if (!level.hasChunkAt(new BlockPos(chunkX + 8, level.getMinBuildHeight(), chunkZ + 8))) return false;
        int top = level.getHeight(Heightmap.Types.WORLD_SURFACE, chunkX + 8, chunkZ + 8);
        if (biome(level, chunkX + 8, top, chunkZ + 8).is(BLACKLIST)) return false;
        RandomSource random = context.random();
        boolean placed = false;
        // Not two independent placed features: silverwood generation consumes
        // this stream BEFORE the greatwood rarity draw, exactly as in TC4.
        if (random.nextInt(60) == 3) {
            int x = chunkX + random.nextInt(16);
            int z = chunkZ + random.nextInt(16);
            int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
            if (MagicalTreeBiomeAdapter.silverwood(biome(level, x, y, z))) {
                placed = new WorldGenSilverwoodTrees(false, 7, 4).generate(level, random, new BlockPos(x, y, z));
            }
        }
        if (random.nextInt(25) == 7) {
            int x = chunkX + random.nextInt(16);
            int z = chunkZ + random.nextInt(16);
            int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
            if (MagicalTreeBiomeAdapter.greatwoodChance(biome(level, x, y, z)) > random.nextFloat()) {
                placed |= new WorldGenGreatwoodTrees(false).generate(level, random, new BlockPos(x, y, z), random.nextInt(8) == 0);
            }
        }
        // Original vegetation pass:473-477 samples x/z even outside deserts;
        // only hot sand-top biomes consume the one-in30 flower draw. The native
        // tag explicitly maps vanilla desert and permits a declared addon mapping.
        int flowerX = chunkX + random.nextInt(16), flowerZ = chunkZ + random.nextInt(16);
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
