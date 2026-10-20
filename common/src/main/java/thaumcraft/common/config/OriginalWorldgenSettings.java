package thaumcraft.common.config;

import java.util.Properties;

/** Remaining original fresh-generation flags for existing node/ore/structure
 * consumers. Tree/unused-taint/display options remain in BiomeSettings. */
public record OriginalWorldgenSettings(boolean auraNodes,boolean structures,boolean cinnabarOre,boolean amberOre,boolean infusedStone) {
    public static final OriginalWorldgenSettings DEFAULTS=new OriginalWorldgenSettings(true,true,true,true,true);
    public static OriginalWorldgenSettings read(Properties values) {
        return new OriginalWorldgenSettings(flag(values,"generate_aura_nodes"),flag(values,"generate_structures"),flag(values,"generate_cinnibar_ore"),
            flag(values,"generate_amber_ore"),flag(values,"generate_infused_stone"));
    }
    private static boolean flag(Properties values,String key) {
        String value=values.getProperty(key,"true").trim();
        if(value.equalsIgnoreCase("true"))return true;if(value.equalsIgnoreCase("false"))return false;
        throw new IllegalArgumentException("Invalid original world generation option "+key);
    }
}
