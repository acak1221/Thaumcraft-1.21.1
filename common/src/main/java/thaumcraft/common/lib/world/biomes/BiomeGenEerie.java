package thaumcraft.common.lib.world.biomes;

import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import thaumcraft.common.config.ThaumcraftBiomes;
import thaumcraft.common.lib.world.biomes.BiomeSurfaceRules.Generator;
import thaumcraft.common.lib.world.biomes.BiomeSurfaceRules.TreeKind;

/**
 * Native 1.21.1 replacement of the original TC4 1.7.10 {@code BiomeGenEerie}.
 *
 * <p>Original source: {@code thaumcraft.common.lib.world.biomes.BiomeGenEerie} lines
 * 27-74 of TC4 4.2.3.5. 1.21.1 registers a biome as a datapack document, so the biome
 * definition itself lives in {@code data/thaumcraft/worldgen/biome/eerie.json}; this class
 * keeps the original generator identity and the original values that the native biome
 * document carries.
 *
 * <p>The eerie biome is the DARK node ecology biome. It is never inserted into the
 * climate palette; a dark node creates it. It keeps the 1.7.10 inherited oak tree draw
 * with two trees per chunk, one flower per chunk and two grass per chunk, its dead bush
 * count is the untouched vanilla default of four, and it has no dead tree generator at
 * all. Its distinctive colour is the near black 0x404840 grass with 0x405340 foliage and
 * the 0x222299 sky, which 1.7.10 also used as the distance fog colour.
 */
public final class BiomeGenEerie {
    /** The original 1.7.10 biome name, {@code BiomeGenBase.func_76735_a}. */
    public static final String NAME="Eerie";
    /** The native 1.21.1 biome key of the eerie biome. */
    public static final ResourceKey<Biome> KEY=ThaumcraftBiomes.EERIE;
    /** The original generator that owns this biome. */
    public static final Generator GENERATOR=Generator.EERIE;

    private BiomeGenEerie() {}

    /** The original grass colour, {@code BiomeGenEerie.func_150558_b}. */
    public static int grassColour() {return BiomeSurfaceRules.grassColour(GENERATOR,false);}

    /** The original foliage colour, {@code BiomeGenEerie.func_150571_c}. */
    public static int foliageColour() {return BiomeSurfaceRules.foliageColour(GENERATOR,false);}

    /** The original sky colour, {@code BiomeGenEerie.func_76731_a}; 1.7.10 used it as the fog colour too. */
    public static int skyColour() {return BiomeSurfaceRules.skyColour(GENERATOR);}

    /** The original water colour, {@code BiomeGenEerie.getWaterColorMultiplier}. */
    public static int waterColour() {return BiomeSurfaceRules.waterColour(GENERATOR);}

    /** The original temperature and rainfall, {@code BiomeGenEerie.func_76732_a}. */
    public static float temperature() {return BiomeSurfaceRules.temperature(GENERATOR);}

    public static float rainfall() {return BiomeSurfaceRules.rainfall(GENERATOR);}

    /**
     * The original tree draw. The eerie biome never overrode
     * {@code BiomeGenBase.func_150567_a}, so it inherited the vanilla draw: one big oak
     * tree in ten, otherwise the ordinary oak.
     */
    public static TreeKind treeKind(RandomSource random) {
        int draw=random.nextInt(10);
        return BiomeSurfaceRules.treeKind(GENERATOR,draw,0);
    }

    /** The original number of trees per chunk. */
    public static int treesPerChunk() {return BiomeSurfaceRules.treesPerChunk(GENERATOR);}

    /** The original number of flowers per chunk. */
    public static int flowersPerChunk() {return BiomeSurfaceRules.flowersPerChunk(GENERATOR);}

    /** The original number of grass per chunk. */
    public static int grassPerChunk() {return BiomeSurfaceRules.grassPerChunk(GENERATOR);}

    /** The original dead bush count; TC4 never changed the vanilla default of four. */
    public static int deadBushesPerChunk() {return BiomeSurfaceRules.deadBushPerChunk(GENERATOR);}

    /** The original eerie biome had no dead tree generator; it inherited the vanilla oak. */
    public static boolean hasDeadTrees() {return false;}
}
