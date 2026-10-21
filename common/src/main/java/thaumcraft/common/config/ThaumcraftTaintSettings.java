package thaumcraft.common.config;

import java.util.Properties;

/** Original independent taint settings; these are not added to PacketConfig's seven research facts. */
public final class ThaumcraftTaintSettings {
    public record Values(int spreadRate,boolean spawnSpores,boolean glowing) {}
    public static final Values DEFAULTS=new Values(200,true,true);
    private static volatile Values values=DEFAULTS;
    private ThaumcraftTaintSettings() {}
    public static int taintSpreadRate() { return values.spreadRate(); }
    public static boolean spawnTaintSpore() { return values.spawnSpores(); }
    public static boolean glowingTaint() { return values.glowing(); }
    public static Values values() { return values; }
    public static void configure(Values input) { values=java.util.Objects.requireNonNull(input); Config.apply(values); }
    public static void load(Properties properties) {
        int rate=200;
        try { rate=Integer.parseInt(properties.getProperty("biome_taint_spread","200").strip()); }
        catch(NumberFormatException ignored) {}
        configure(new Values(rate,bool(properties,"spawn_taint_spores"),bool(properties,"glowing_taint")));
    }
    private static boolean bool(Properties properties,String key) {
        String value=properties.getProperty(key,"true").strip();return value.equalsIgnoreCase("false")?false:true;
    }
}
