package thaumcraft.common.config;

import java.util.Properties;

/** Actual Config constructor/global natural-spawn switches. Spore spawning
 * remains separately owned by TaintSettings and is intentionally absent here. */
public record OriginalSpawnSettings(boolean angryZombies,boolean fireBats,boolean wisps,boolean taintacles,boolean pechs,boolean elderCreatures) {
    public static final OriginalSpawnSettings DEFAULTS=new OriginalSpawnSettings(true,true,true,true,true,true);
    public static OriginalSpawnSettings read(Properties values) {
        return new OriginalSpawnSettings(flag(values,"spawn_angry_zombies"),flag(values,"spawn_fire_bats"),flag(values,"spawn_wisps"),
            flag(values,"spawn_taintacles"),flag(values,"spawn_pechs"),flag(values,"spawn_eldercreatures"));
    }
    private static boolean flag(Properties values,String key) {
        String value=values.getProperty(key,"true").trim();
        if(value.equalsIgnoreCase("true"))return true;if(value.equalsIgnoreCase("false"))return false;
        throw new IllegalArgumentException("Invalid original natural spawn option "+key);
    }
}
