package thaumcraft.common.lib.world.biomes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.lib.world.biomes.BiomeSurfaceRules.Generator;

/**
 * Native port of the original TC4 1.7.10 {@code BiomeHandler}.
 *
 * <p>The original class did two things. First it was the dispatch that owned a biome:
 * {@code Config.registerBiomes} registered every TC4 biome into the Forge
 * {@code BiomeDictionary} with an explicit type list, and {@code BiomeHandler} answered
 * aura, aspect and greatwood questions from that list. Second it kept the
 * {@code registerBiomeInfo} table that gave every dictionary type its aura level, its
 * aspect tag and its greatwood support.
 *
 * <p>1.21.1 removed {@code BiomeDictionary} and the numeric biome ids. The dispatch is
 * therefore keyed by the native biome resource id and by the original numeric id, and the
 * dictionary types are the original Forge type names, which the native project keeps as
 * the {@code thaumcraft:node_biome_types/*} and {@code thaumcraft:ore_biome_types/*} tag
 * folders. The native runtime consumers of that table are
 * {@code thaumcraft.common.lib.world.NormalNodeBiomeAdapter} and
 * {@code thaumcraft.common.lib.world.MagicalTreeBiomeAdapter}; they read the same tags
 * at runtime. All three consumers share the immutable original values in
 * {@link OriginalBiomeTypes}; later public registrations change only this compatibility registry.
 *
 * <p>Original values: {@code thaumcraft.common.lib.world.biomes.BiomeHandler} lines 22-65
 * and {@code thaumcraft.common.config.Config} lines 778-812.
 */
public final class BiomeHandler {
    /** The original fallback aura of a biome without any dictionary type. */
    public static final int DEFAULT_AURA=100;
    /** The native tag folder that carries the original dictionary types for node consumers. */
    public static final String NODE_TAG_PREFIX="thaumcraft:node_biome_types/";
    /** The native tag folder that carries the original dictionary types for ore consumers. */
    public static final String ORE_TAG_PREFIX="thaumcraft:ore_biome_types/";

    /** The original per dictionary type information of {@code BiomeHandler.registerBiomeInfo}. */
    public record Info(int aura,Aspect tag,boolean greatwood,float greatwoodChance) {}

    private static final Map<String,Info> BIOME_INFO=new LinkedHashMap<>();

    static {
        for (OriginalBiomeTypes.Type type : OriginalBiomeTypes.all())
            registerBiomeInfo(type.name(), type.aura(), type.aspect(), type.greatwood(), type.greatwoodChance());
    }

    private BiomeHandler() {}

    /** The original {@code BiomeHandler.registerBiomeInfo}. */
    public static void registerBiomeInfo(String type,int auraLevel,Aspect tag,boolean greatwood,float greatwoodchance) {
        BIOME_INFO.put(type,new Info(auraLevel,tag,greatwood,greatwoodchance));
    }

    /** The registered information of one original dictionary type, or {@code null} for an unknown type. */
    public static Info info(String type) {return BIOME_INFO.get(type);}

    /** Every original dictionary type name, in the original registration order. */
    public static List<String> types() {return List.copyOf(BIOME_INFO.keySet());}

    // ---------------------------------------------------------------- dispatch

    /** The original generator that owns a native biome key, or {@code null} for a non-TC4 biome. */
    public static Generator generator(ResourceKey<Biome> key) {return BiomeSurfaceRules.generator(key);}

    /** The original generator that owns a native biome resource id, or {@code null} for a non-TC4 biome. */
    public static Generator generator(String id) {return BiomeSurfaceRules.generator(id);}

    /** The original generator that owned an original 1.7.10 numeric biome id, or {@code null}. */
    public static Generator generator(int originalId) {return BiomeSurfaceRules.generator(originalId);}

    /**
     * The original {@code Config.registerBiomes} membership of a TC4 biome.
     * The tainted biome was magical and wasteland, the eerie biome magical and spooky,
     * the eldritch biome magical, spooky and end, the magical forest magical and forest.
     */
    public static List<String> dictionaryTypes(Generator generator) {
        return switch(generator) {
            case EERIE -> List.of("magical","spooky");
            case ELDRITCH -> List.of("magical","spooky","end");
            case MAGICAL_FOREST -> List.of("magical","forest");
            case TAINT -> List.of("magical","wasteland");
        };
    }

    /** The native node tag path of an original dictionary type. */
    public static String nodeTag(String type) {return NODE_TAG_PREFIX+type;}

    /** The native ore tag path of an original dictionary type. */
    public static String oreTag(String type) {return ORE_TAG_PREFIX+type;}

    // -------------------------------------------------------------- aura, tag

    /**
     * The original {@code BiomeHandler.getBiomeAura}: the average aura level of every
     * dictionary type of the biome, or the original fallback of 100 for a biome without
     * a known dictionary type.
     */
    public static int biomeAura(Collection<String> types) {
        if(types==null||types.isEmpty())return DEFAULT_AURA;
        int total=0,count=0;
        for(String type:types) {
            Info info=BIOME_INFO.get(type);
            if(info==null)continue;
            total+=info.aura();count++;
        }
        return count>0?total/count:DEFAULT_AURA;
    }

    /** The aura level of a TC4 biome, from its original dictionary membership. */
    public static int biomeAura(Generator generator) {return biomeAura(dictionaryTypes(generator));}

    /**
     * The original {@code BiomeHandler.getRandomBiomeTag}: the aspect of the drawn
     * dictionary type. The magical type carried no aspect at all and answers null.
     */
    public static Aspect randomBiomeTag(Collection<String> types,int index) {
        if(types==null||types.isEmpty())return null;
        List<String> ordered=new ArrayList<>(types);
        if(index<0||index>=ordered.size())return null;
        Info info=BIOME_INFO.get(ordered.get(index));
        return info==null?null:info.tag();
    }

    /** The original {@code BiomeHandler.getRandomBiomeTag} draw over the original type list. */
    public static Aspect randomBiomeTag(Collection<String> types,Random random) {
        if(types==null||types.isEmpty())return null;
        return randomBiomeTag(types,random.nextInt(types.size()));
    }

    /**
     * The original {@code BiomeHandler.getBiomeSupportsGreatwood}: the greatwood chance of
     * the first dictionary type that supports greatwood, or 0 when none does.
     */
    public static float greatwoodChance(Collection<String> types) {
        if(types==null)return 0.0F;
        for(String type:types) {
            Info info=BIOME_INFO.get(type);
            if(info!=null&&info.greatwood())return info.greatwoodChance();
        }
        return 0.0F;
    }

    /** The greatwood chance of a TC4 biome, from its original dictionary membership. */
    public static float greatwoodChance(Generator generator) {return greatwoodChance(dictionaryTypes(generator));}
}
