package thaumcraft.common.lib.world.biomes;

import java.util.List;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;

/**
 * Native 1.21.1 decision layer for the four original TC4 4.2.3.5 biome generators.
 *
 * <p>Original 1.7.10 sources (oracle TC4 4.2.3.5, {@code thaumcraft.common.lib.world.biomes}):
 * {@code BiomeGenEerie}, {@code BiomeGenEldritch}, {@code BiomeGenMagicalForest},
 * {@code BiomeGenTaint}, {@code BiomeHandler} and {@code Config.registerBiomes}.
 *
 * <p>Every value is transcribed verbatim. The three overworld biomes already exist
 * natively as {@code data/thaumcraft/worldgen/biome/*.json}; their numbers here are the
 * same numbers those committed files carry, so this class documents the original and
 * {@code BiomeSurfaceRulesTest} proves the committed data still matches. The Eldritch
 * Lands biome had no native counterpart at all and is created by this unit.
 *
 * <p>Documented 1.21.1 adaptations:
 * <ul>
 * <li>1.7.10 stored a per-biome {@code waterColorMultiplier} that the client used as the
 * biome water colour; 1.21.1 stores an absolute {@code water_color}. The native project
 * already uses the original value verbatim for the three committed biomes, so the Eldritch
 * biome follows that single convention instead of inventing a second one.</li>
 * <li>1.7.10 had no separate fog colour: the client used the biome sky colour for the
 * fog. 1.21.1 has {@code fog_color} and {@code water_fog_color}. The three committed
 * overworld biomes keep the native 1.21.1 fog values; the Eldritch biome uses the
 * original's single black sky colour for both, because its sky really was black.</li>
 * <li>1.7.10 biome heights ({@code BiomeGenBase.setHeight}) belong to the removed
 * {@code GenLayerHeight}; 1.21.1 derives terrain height from the dimension noise
 * settings. The original heights are kept here as data only.</li>
 * </ul>
 */
public final class BiomeSurfaceRules {
    /** The 1.7.10 {@code BiomeGenBase} default colour of a biome that never calls {@code setColor}. */
    public static final int DEFAULT_BIOME_COLOUR=0x000000;
    /** The 1.7.10 vanilla overworld water colour, used by a biome that never overrides it. */
    public static final int DEFAULT_WATER_COLOUR=0x3F76E4;
    /** The 1.21.1 native fog colour used by the three committed TC4 overworld biomes. */
    public static final int NATIVE_FOG_COLOUR=12638463;
    /** The 1.21.1 native water fog colour used by the three committed TC4 overworld biomes. */
    public static final int NATIVE_WATER_FOG_COLOUR=329011;
    /** 1.7.10 {@code BiomeGenBase.getSkyColorByTemp} hue/saturation constants. */
    private static final float SKY_HUE=0.62222224F,SKY_HUE_SPAN=0.05F,SKY_SATURATION=0.5F,SKY_SATURATION_SPAN=0.1F;

    private BiomeSurfaceRules() {}

    /** The original 1.7.10 {@code BiomeGen} subclass that owns one TC4 biome. */
    public enum Generator {
        /** {@code BiomeGenEerie}: the DARK node ecology biome. */
        EERIE("Eerie",194,"thaumcraft:eerie"),
        /** {@code BiomeGenEldritch}: the Outer Lands biome of the Eldritch dimension. */
        ELDRITCH("Eldritch",195,"thaumcraft:eldritch_lands"),
        /** {@code BiomeGenMagicalForest}: the PURE node ecology biome. */
        MAGICAL_FOREST("Magical Forest",193,"thaumcraft:magical_forest"),
        /** {@code BiomeGenTaint}: the TAINTED node ecology biome. */
        TAINT("Tainted Land",192,"thaumcraft:taint");
        private final String biomeName;private final int originalId;private final String id;
        Generator(String biomeName,int originalId,String id) {this.biomeName=biomeName;this.originalId=originalId;this.id=id;}
        /** The original 1.7.10 biome name, {@code BiomeGenBase.func_76735_a}. */
        public String biomeName() {return biomeName;}
        /** The original default 1.7.10 numeric biome id, {@code Config} static initialiser. */
        public int originalId() {return originalId;}
        /** The native 1.21.1 resource id of the biome. */
        public String id() {return id;}
    }

    /** The tree generators the original biomes could return from {@code BiomeGenBase.func_150567_a}. */
    public enum TreeKind {
        /** The vanilla 1.7.10 {@code WorldGenTrees} oak, used by the inherited biome tree draw. */
        OAK,
        /** The vanilla 1.7.10 {@code WorldGenBigTree}, drawn one time in ten by the default biome tree. */
        BIG_OAK,
        /** {@code WorldGenSilverwoodTrees}, the magical forest's own tree. */
        SILVERWOOD,
        /** {@code WorldGenGreatwoodTrees}, the magical forest's own tree. */
        GREATWOOD,
        /** {@code WorldGenBigMagicTree}, the magical forest's default tree and the taint biome's rare tree. */
        BIG_MAGIC
    }

    // ---------------------------------------------------------------- dispatch

    /** The generator that owns a native biome key, or {@code null} for a non-TC4 biome. */
    public static Generator generator(ResourceKey<Biome> key) {
        return key==null?null:generator(key.location().toString());
    }

    /** The generator that owns a native biome resource id, or {@code null} for a non-TC4 biome. */
    public static Generator generator(String id) {
        for(Generator generator:Generator.values())if(generator.id().equals(id))return generator;
        return null;
    }

    /** The generator that owned an original 1.7.10 numeric biome id, or {@code null}. */
    public static Generator generator(int originalId) {
        for(Generator generator:Generator.values())if(generator.originalId()==originalId)return generator;
        return null;
    }

    // ----------------------------------------------------------------- climate

    /** 1.7.10 {@code BiomeGenBase.func_76732_a} temperature; a biome that never called it keeps the 0.5 default. */
    public static float temperature(Generator generator) {
        return switch(generator) {
            case MAGICAL_FOREST -> 0.7F;
            case EERIE,ELDRITCH,TAINT -> 0.5F;
        };
    }

    /** 1.7.10 {@code BiomeGenBase.func_76732_a} rainfall; a biome that never called it keeps the 0.5 default. */
    public static float rainfall(Generator generator) {
        return switch(generator) {
            case MAGICAL_FOREST -> 0.6F;
            case EERIE,ELDRITCH,TAINT -> 0.5F;
        };
    }

    /** 1.7.10 {@code BiomeGenBase.isHighHumidity}: rainfall above 0.85. No TC4 biome is a high humidity biome. */
    public static boolean highHumidity(Generator generator) {return rainfall(generator)>0.85F;}

    /** 1.7.10 {@code BiomeGenBase.func_76745_m} disabled rain; only the Eldritch biome disables it. */
    public static boolean precipitation(Generator generator) {return generator!=Generator.ELDRITCH;}

    /** 1.7.10 {@code BiomeGenBase.setHeight} root height; 1.21.1 owns terrain height per dimension. */
    public static float rootHeight(Generator generator) {
        return generator==Generator.MAGICAL_FOREST?0.2F:0.1F;
    }

    /** 1.7.10 {@code BiomeGenBase.setHeight} height variation; 1.21.1 owns terrain height per dimension. */
    public static float heightVariation(Generator generator) {return 0.2F;}

    // ---------------------------------------------------------------- colours

    /** 1.7.10 {@code BiomeGenBase.func_76739_b} base biome colour. */
    public static int baseColour(Generator generator,boolean blueMagicalForest) {
        return switch(generator) {
            case EERIE -> 0x404840;
            case ELDRITCH -> DEFAULT_BIOME_COLOUR;
            case MAGICAL_FOREST -> blueMagicalForest?0x66AACC:6747307;
            case TAINT -> 7160201;
        };
    }

    /** 1.7.10 {@code BiomeGenBase.func_150558_b} grass colour. */
    public static int grassColour(Generator generator,boolean blueMagicalForest) {
        return switch(generator) {
            case EERIE -> 0x404840;
            case ELDRITCH -> DEFAULT_BIOME_COLOUR;
            case MAGICAL_FOREST -> blueMagicalForest?0x66AACC:5635969;
            case TAINT -> 7160201;
        };
    }

    /** 1.7.10 {@code BiomeGenBase.func_150571_c} foliage colour. */
    public static int foliageColour(Generator generator,boolean blueMagicalForest) {
        return switch(generator) {
            case EERIE -> 4215616;
            case ELDRITCH -> DEFAULT_BIOME_COLOUR;
            case MAGICAL_FOREST -> blueMagicalForest?0x77CCEE:6750149;
            case TAINT -> 8154503;
        };
    }

    /** The 1.21.1 native sky colour of a TC4 biome. */
    public static int skyColour(Generator generator) {
        return switch(generator) {
            case EERIE -> 0x222299;
            case ELDRITCH -> 0x000000;
            case MAGICAL_FOREST -> 8038143;
            case TAINT -> 8144127;
        };
    }

    /**
     * The exact 1.7.10 {@code BiomeGenBase.getSkyColorByTemp} result for a biome temperature.
     * The magical forest never overrode the sky colour, so its original value came from this
     * formula; the committed native biome JSON keeps the rounded 1.21.1 equivalent instead.
     */
    public static int defaultSkyColour(float temperature) {
        float scaled=temperature/3.0F;
        if(scaled<-1.0F)scaled=-1.0F;
        if(scaled>1.0F)scaled=1.0F;
        float hue=SKY_HUE-scaled*SKY_HUE_SPAN,saturation=SKY_SATURATION+scaled*SKY_SATURATION_SPAN;
        int sector=(int)(hue*6.0F);
        float fraction=hue*6.0F-sector;
        float p=1.0F-saturation,q=1.0F-saturation*fraction,t=1.0F-saturation*(1.0F-fraction);
        int red,green,blue;
        switch(sector) {
            case 0 -> {red=(int)(1.0F*255.0F);green=(int)(t*255.0F);blue=(int)(p*255.0F);}
            case 1 -> {red=(int)(q*255.0F);green=(int)(1.0F*255.0F);blue=(int)(p*255.0F);}
            case 2 -> {red=(int)(p*255.0F);green=(int)(1.0F*255.0F);blue=(int)(t*255.0F);}
            case 3 -> {red=(int)(p*255.0F);green=(int)(q*255.0F);blue=(int)(1.0F*255.0F);}
            case 4 -> {red=(int)(t*255.0F);green=(int)(p*255.0F);blue=(int)(1.0F*255.0F);}
            case 5 -> {red=(int)(1.0F*255.0F);green=(int)(p*255.0F);blue=(int)(q*255.0F);}
            default -> {return 0;}
        }
        return (red<<16)|(green<<8)|blue;
    }

    /** The 1.21.1 native water colour of a TC4 biome; the original 1.7.10 water colour multiplier. */
    public static int waterColour(Generator generator) {
        return switch(generator) {
            case EERIE -> 3035999;
            case ELDRITCH -> DEFAULT_WATER_COLOUR;
            case MAGICAL_FOREST -> 30702;
            case TAINT -> 0xCC1188;
        };
    }

    /** The 1.21.1 native fog colour of a TC4 biome. */
    public static int fogColour(Generator generator) {
        return generator==Generator.ELDRITCH?0x000000:NATIVE_FOG_COLOUR;
    }

    /** The 1.21.1 native water fog colour of a TC4 biome. */
    public static int waterFogColour(Generator generator) {return NATIVE_WATER_FOG_COLOUR;}

    // ----------------------------------------------------------- surface blocks

    /** 1.7.10 {@code BiomeGenBase.field_76752_A} top block of the biome. */
    public static String topBlock(Generator generator) {
        return generator==Generator.ELDRITCH?"minecraft:dirt":"minecraft:grass_block";
    }

    /** 1.7.10 {@code BiomeGenBase.field_76753_B} filler block of the biome. */
    public static String fillerBlock(Generator generator) {return "minecraft:dirt";}

    // ------------------------------------------------- decorator draw counters

    /** 1.7.10 {@code BiomeDecorator.treesPerChunk}. */
    public static int treesPerChunk(Generator generator) {
        return switch(generator) {
            case EERIE,MAGICAL_FOREST,TAINT -> 2;
            case ELDRITCH -> 0;
        };
    }

    /** 1.7.10 {@code BiomeDecorator.flowersPerChunk}; the tainted biome really used -999. */
    public static int flowersPerChunk(Generator generator) {
        return switch(generator) {
            case EERIE -> 1;
            case MAGICAL_FOREST -> 10;
            case TAINT -> -999;
            case ELDRITCH -> 0;
        };
    }

    /** 1.7.10 {@code BiomeDecorator.grassPerChunk}. */
    public static int grassPerChunk(Generator generator) {
        return switch(generator) {
            case EERIE -> 2;
            case MAGICAL_FOREST -> 12;
            case TAINT -> 2;
            case ELDRITCH -> 0;
        };
    }

    /** 1.7.10 {@code BiomeDecorator.waterlilyPerChunk}. */
    public static int waterlilyPerChunk(Generator generator) {
        return generator==Generator.MAGICAL_FOREST?6:0;
    }

    /** 1.7.10 {@code BiomeDecorator.mushroomsPerChunk}. */
    public static int mushroomsPerChunk(Generator generator) {
        return generator==Generator.MAGICAL_FOREST?6:0;
    }

    /** 1.7.10 {@code BiomeDecorator.reedsPerChunk}; the tainted biome really used -999. */
    public static int reedsPerChunk(Generator generator) {
        return generator==Generator.TAINT?-999:10;
    }

    /** 1.7.10 {@code BiomeDecorator.deadBushPerChunk}, never changed by TC4. */
    public static int deadBushPerChunk(Generator generator) {return 4;}

    /** 1.7.10 {@code BiomeDecorator.bigMushroomsPerChunk}, never changed by TC4. */
    public static int bigMushroomsPerChunk(Generator generator) {return 1;}

    /** 1.7.10 {@code BiomeDecorator.cactiPerChunk}, never changed by TC4. */
    public static int cactiPerChunk(Generator generator) {return 0;}

    /** 1.7.10 {@code BiomeDecorator.sandPerChunk}, never changed by TC4. */
    public static int sandPerChunk(Generator generator) {return 4;}

    /** 1.7.10 {@code BiomeDecorator.sandPerChunk2}, never changed by TC4. */
    public static int sandPerChunk2(Generator generator) {return 10;}

    /** 1.7.10 {@code BiomeDecorator.clayPerChunk}, never changed by TC4. */
    public static int clayPerChunk(Generator generator) {return 4;}

    /**
     * The number of decorations the 1.7.10 decorator actually performs for a configured
     * counter. The tainted biome configured -999 for flowers and reeds, so it performs
     * none of them; the original quirk is kept instead of being clamped at the source.
     */
    public static int effectiveCount(int configured) {return configured>0?configured:0;}

    // ------------------------------------------------------ magical forest trees

    /** The magical forest's own tree draw, {@code BiomeGenMagicalForest.func_150567_a}. */
    public static TreeKind treeKind(Generator generator,int firstDraw,int secondDraw) {
        if(generator==Generator.MAGICAL_FOREST) {
            if(firstDraw==0)return TreeKind.SILVERWOOD;
            return secondDraw==0?TreeKind.GREATWOOD:TreeKind.BIG_MAGIC;
        }
        if(generator==Generator.TAINT)return firstDraw==0?TreeKind.BIG_MAGIC:TreeKind.OAK;
        return firstDraw==0?TreeKind.BIG_OAK:TreeKind.OAK;
    }

    /** The number of draws the original tree selection consumes before the tree is known. */
    public static int treeDraws(Generator generator) {
        return generator==Generator.MAGICAL_FOREST?2:1;
    }

    /** The magical forest's own grass draw, {@code BiomeGenMagicalForest.func_76730_b}: one in four double grass. */
    public static boolean doubleGrass(int draw) {return draw==0;}

    /** The magical forest's nine red flower types, {@code BlockFlower.field_149859_a}. */
    public static final List<String> MAGICAL_FOREST_FLOWERS=List.of("minecraft:poppy","minecraft:blue_orchid","minecraft:allium",
        "minecraft:azure_bluet","minecraft:red_tulip","minecraft:orange_tulip","minecraft:white_tulip","minecraft:pink_tulip",
        "minecraft:oxeye_daisy");

    /** The weight every one of the nine original magical forest flowers carried. */
    public static final int MAGICAL_FOREST_FLOWER_WEIGHT=10;

    // ------------------------------------------------- magical forest decoration

    /** The original magical forest mossy cobblestone blob count, {@code random.nextInt(3)}. */
    public static int mossyBlobCount(int draw) {return draw;}

    /** The original magical forest big mushroom grid is four by four blocks. */
    public static final int BIG_MUSHROOM_GRID=4;

    /** The original magical forest big mushroom draw was one in forty. */
    public static final int BIG_MUSHROOM_CHANCE=40;

    /** The original magical forest generated ten mana pods per chunk. */
    public static final int MANA_PODS=10;

    /** The original magical forest generated its mana pods from y64. */
    public static final int MANA_POD_Y=64;

    /** The original magical forest generated eight vishrooms per chunk. */
    public static final int VISHROOMS=8;

    /** The original magical forest vishroom search descended to y50. */
    public static final int VISHROOM_MIN_Y=50;

    /** The original magical forest vishroom had to touch a wood log. */
    public static final int VISHROOM_ADJACENCY=1;

    // ------------------------------------------------------------ taint decoration

    /** The original tainted biome taint crust blob count, {@code random.nextInt(3)}. */
    public static int taintBlobCount(int draw) {return draw;}

    /** The original tainted biome generated ten surface taint fibres per chunk. */
    public static final int TAINT_FIBRES=10;

    /** The original tainted biome made eight first uncovered height fibre attempts per chunk. */
    public static final int TAINT_COLUMN_FIBRES=8;

    /** The original tainted biome grew a big magic tree one time in eight. */
    public static final int TAINT_BIG_MAGIC_CHANCE=8;

    // ------------------------------------------------------------- eldritch rules

    /** The original Eldritch biome decorated nothing at all. */
    public static boolean decorates(Generator generator) {return generator!=Generator.ELDRITCH;}

    /** The original Eldritch biome had no tree draw of its own. */
    public static int eldritchTreeDraws() {return 0;}

    /** The original Eldritch biome was a single biome world. */
    public static boolean singleBiomeWorld(Generator generator) {return generator==Generator.ELDRITCH;}
}
