package thaumcraft.common.lib.world.biomes;

import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import thaumcraft.common.config.ThaumcraftBiomes;
import thaumcraft.common.lib.world.biomes.BiomeSurfaceRules.Generator;
import thaumcraft.common.lib.world.biomes.BiomeSurfaceRules.TreeKind;

/**
 * Native 1.21.1 replacement of the original TC4 1.7.10 {@code BiomeGenTaint}.
 *
 * <p>Original source: {@code thaumcraft.common.lib.world.biomes.BiomeGenTaint} lines
 * 36-122 of TC4 4.2.3.5. 1.21.1 registers a biome as a datapack document, so the biome
 * definition itself lives in {@code data/thaumcraft/worldgen/biome/taint.json}; this
 * class keeps the original generator identity and the original generator specific draws.
 *
 * <p>The tainted biome is the TAINTED node ecology biome. Its original quirks are kept:
 * the flower counter and the reed counter were both configured to -999, so the biome
 * really grows no flowers and no reeds even though the inherited decorator still performs
 * its own ten reed attempts; the taint crust blobs, the ten surface taint fibres and the
 * eight first uncovered height fibre attempts that also expand the biome column are the
 * original taint decoration; and one time in eight the tainted biome grows a big magic
 * tree instead of the inherited oak.
 */
public final class BiomeGenTaint {
    /** The original 1.7.10 biome name, {@code BiomeGenBase.func_76735_a}. */
    public static final String NAME="Tainted Land";
    /** The native 1.21.1 biome key of the tainted biome. */
    public static final ResourceKey<Biome> KEY=ThaumcraftBiomes.TAINT;
    /** The original generator that owns this biome. */
    public static final Generator GENERATOR=Generator.TAINT;

    private BiomeGenTaint() {}

    /** The original grass colour, {@code BiomeGenTaint.func_150558_b}. */
    public static int grassColour() {return BiomeSurfaceRules.grassColour(GENERATOR,false);}

    /** The original foliage colour, {@code BiomeGenTaint.func_150571_c}. */
    public static int foliageColour() {return BiomeSurfaceRules.foliageColour(GENERATOR,false);}

    /** The original sky colour, {@code BiomeGenTaint.func_76731_a}. */
    public static int skyColour() {return BiomeSurfaceRules.skyColour(GENERATOR);}

    /** The original water colour, {@code BiomeGenTaint.getWaterColorMultiplier}. */
    public static int waterColour() {return BiomeSurfaceRules.waterColour(GENERATOR);}

    /** The original temperature and rainfall; the tainted biome never called {@code setTemperatureRainfall}. */
    public static float temperature() {return BiomeSurfaceRules.temperature(GENERATOR);}

    public static float rainfall() {return BiomeSurfaceRules.rainfall(GENERATOR);}

    /**
     * The original tree draw, {@code BiomeGenTaint.func_150567_a}: one big magic tree in
     * eight, otherwise the inherited oak.
     */
    public static TreeKind treeKind(RandomSource random) {
        int draw=random.nextInt(BiomeSurfaceRules.TAINT_BIG_MAGIC_CHANCE);
        return BiomeSurfaceRules.treeKind(GENERATOR,draw,0);
    }

    /** The original number of trees per chunk. */
    public static int treesPerChunk() {return BiomeSurfaceRules.treesPerChunk(GENERATOR);}

    /** The original flower counter; the tainted biome really configured -999. */
    public static int flowersPerChunk() {return BiomeSurfaceRules.flowersPerChunk(GENERATOR);}

    /** The original grass counter. */
    public static int grassPerChunk() {return BiomeSurfaceRules.grassPerChunk(GENERATOR);}

    /** The original reed counter; the tainted biome really configured -999. */
    public static int reedsPerChunk() {return BiomeSurfaceRules.reedsPerChunk(GENERATOR);}

    /** The number of flowers the original decorator really placed; the -999 counter places none. */
    public static int effectiveFlowers() {return BiomeSurfaceRules.effectiveCount(flowersPerChunk());}

    /** The number of reeds the original decorator really placed; the -999 counter places none. */
    public static int effectiveReeds() {return BiomeSurfaceRules.effectiveCount(reedsPerChunk());}

    /** The original taint crust blob count, {@code random.nextInt(3)}. */
    public static int taintBlobs(RandomSource random) {return BiomeSurfaceRules.taintBlobCount(random.nextInt(3));}

    /** The original number of surface taint fibre attempts per chunk. */
    public static int surfaceFibres() {return BiomeSurfaceRules.TAINT_FIBRES;}

    /** The original number of first uncovered height taint fibre attempts per chunk. */
    public static int columnFibres() {return BiomeSurfaceRules.TAINT_COLUMN_FIBRES;}
}
