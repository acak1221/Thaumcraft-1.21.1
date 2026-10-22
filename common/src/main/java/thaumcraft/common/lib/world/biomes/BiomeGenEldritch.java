package thaumcraft.common.lib.world.biomes;

import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import thaumcraft.common.lib.world.biomes.BiomeSurfaceRules.Generator;
import thaumcraft.common.lib.world.biomes.OriginalSpawnRules.Row;

/**
 * Native 1.21.1 replacement of the original TC4 1.7.10 {@code BiomeGenEldritch}.
 *
 * <p>Original source: {@code thaumcraft.common.lib.world.biomes.BiomeGenEldritch} lines
 * 23-50 of TC4 4.2.3.5. This is the biome of the Outer Lands dimension: the original
 * {@code WorldProviderOuter} built its single biome world with
 * {@code new WorldChunkManagerHell(ThaumcraftWorldGenerator.biomeEldritchLands, 0.0F)},
 * so this biome is the whole world of that dimension.
 *
 * <p>The original rules are kept exactly: dirt as the top block and as the filler block,
 * a black sky colour, disabled rain, no decoration at all, and a monster list that was
 * cleared and then filled with exactly an inhabited zombie and an eldritch guardian, both
 * with weight one and a group size of one. Its dictionary membership was magical, spooky
 * and end, which made it a magical, spooky end biome for every aura and greatwood query.
 *
 * <p>The native registration of this biome was missing: the native project shipped the
 * eerie, magical forest and tainted biome documents and its tags already referenced
 * {@code thaumcraft:eldritch_lands}, but no biome document and no registry key existed.
 * This unit adds the key and the document.
 *
 * <p>The native biome document encodes both original monster rows (oracle
 * {@code BiomeGenEldritch.java:31-32}); their registered native IDs are
 * {@code thaumcraft:inhabited_zombie} and {@code thaumcraft:eldritch_guardian}.
 */
public final class BiomeGenEldritch {
    /** The original 1.7.10 biome name, {@code BiomeGenBase.func_76735_a}. */
    public static final String NAME="Eldritch";
    /** The native 1.21.1 biome key of the Eldritch Lands biome. */
    public static final ResourceKey<Biome> KEY=ResourceKey.create(Registries.BIOME,
        ResourceLocation.fromNamespaceAndPath("thaumcraft","eldritch_lands"));
    /** The original generator that owns this biome. */
    public static final Generator GENERATOR=Generator.ELDRITCH;
    /** The original 1.7.10 biome id of the Eldritch Lands biome. */
    public static final int ORIGINAL_BIOME_ID=195;
    /** The original entity of the first cleared monster row. */
    public static final String INHABITED_ZOMBIE="thaumcraft:inhabitedzombie";
    /** The original entity of the second cleared monster row. */
    public static final String ELDRITCH_GUARDIAN="thaumcraft:eldritch_guardian";

    private BiomeGenEldritch() {}

    /** The original top block of the Eldritch biome, {@code BiomeGenBase.field_76752_A}. */
    public static String topBlock() {return BiomeSurfaceRules.topBlock(GENERATOR);}

    /** The original filler block of the Eldritch biome, {@code BiomeGenBase.field_76753_B}. */
    public static String fillerBlock() {return BiomeSurfaceRules.fillerBlock(GENERATOR);}

    /** The original sky colour, {@code BiomeGenEldritch.func_76731_a}. */
    public static int skyColour() {return BiomeSurfaceRules.skyColour(GENERATOR);}

    /** The original grass colour; the Eldritch biome never overrode the 1.7.10 default colour. */
    public static int grassColour() {return BiomeSurfaceRules.grassColour(GENERATOR,false);}

    /** The original foliage colour; the Eldritch biome never overrode the 1.7.10 default colour. */
    public static int foliageColour() {return BiomeSurfaceRules.foliageColour(GENERATOR,false);}

    /** The original water colour; the Eldritch biome never overrode the 1.7.10 default colour. */
    public static int waterColour() {return BiomeSurfaceRules.waterColour(GENERATOR);}

    /** The original temperature and rainfall; the Eldritch biome never called {@code setTemperatureRainfall}. */
    public static float temperature() {return BiomeSurfaceRules.temperature(GENERATOR);}

    public static float rainfall() {return BiomeSurfaceRules.rainfall(GENERATOR);}

    /** The original Eldritch biome disabled rain, {@code BiomeGenEldritch.func_76745_m}. */
    public static boolean precipitation() {return BiomeSurfaceRules.precipitation(GENERATOR);}

    /** The original Eldritch biome decorated nothing, {@code BiomeGenEldritch.func_76728_a}. */
    public static boolean decorates() {return BiomeSurfaceRules.decorates(GENERATOR);}

    /** The original monster rows of the cleared Eldritch biome monster list. */
    public static List<Row> spawnRows() {return List.of(new Row(INHABITED_ZOMBIE,1,1,1),new Row(ELDRITCH_GUARDIAN,1,1,1));}

    /**
     * The original rows that the native biome document carries. The inhabited zombie
     * entity exists natively since the eldritch bestiary landed, so both original
     * rows are encoded; the native registry flattened the original id.
     */
    public static List<Row> encodableSpawnRows() {
        return List.of(new Row("thaumcraft:inhabited_zombie",1,1,1),new Row(ELDRITCH_GUARDIAN,1,1,1));
    }

    /** The original dictionary membership of the Eldritch biome, {@code Config.registerBiomes}. */
    public static List<String> dictionaryTypes() {return BiomeHandler.dictionaryTypes(GENERATOR);}
}
