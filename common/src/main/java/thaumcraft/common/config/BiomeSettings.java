package thaumcraft.common.config;

import java.util.Properties;

/** Immutable startup options from original Config.initialize/syncConfigurable.
 * generateTaint was stored but never consumed in4.2.3.5; retain its original
 * ineffectiveness instead of inventing a new biome-disable behavior. */
public record BiomeSettings(int magicalForestWeight,int taintWeight,boolean generateTrees,boolean generateTaint,boolean blueMagicalForest) {
    public static final BiomeSettings DEFAULTS=new BiomeSettings(5,2,true,true,false);
    public BiomeSettings {
        if(magicalForestWeight<0||taintWeight<0||(long)magicalForestWeight+taintWeight+60>Integer.MAX_VALUE)
            throw new IllegalArgumentException("Biome weights must be nonnegative and fit the original weighted lottery");
    }
    public int warmWeight() {return 60+magicalForestWeight+taintWeight;}
    public int coolWeight() {return 40+magicalForestWeight+taintWeight;}
    public static BiomeSettings read(Properties properties) {
        return new BiomeSettings(integer(properties,"magical_forest_biome_weight",5),integer(properties,"taint_biome_weight",2),
            flag(properties,"generate_trees",true),flag(properties,"generate_taint",true),flag(properties,"blue_magical_forest",false));
    }
    private static int integer(Properties properties,String key,int fallback) {
        try{return Integer.parseInt(properties.getProperty(key,Integer.toString(fallback)).trim());}
        catch(NumberFormatException malformed){throw new IllegalArgumentException("Invalid original biome option "+key,malformed);}
    }
    private static boolean flag(Properties properties,String key,boolean fallback) {
        String value=properties.getProperty(key,Boolean.toString(fallback)).trim();
        if(value.equalsIgnoreCase("true"))return true;if(value.equalsIgnoreCase("false"))return false;
        throw new IllegalArgumentException("Invalid original biome option "+key);
    }
}
