package thaumcraft.common.lib.runic;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import thaumcraft.common.Thaumcraft;

/** Original config clamps, same server properties file; never client-authoritative. */
public record RunicSettings(int rechargeMillis, int delayTicks, int cost) {
    public static final RunicSettings DEFAULTS = new RunicSettings(2000,80,50);
    private static volatile RunicSettings current = DEFAULTS;
    public RunicSettings {
        rechargeMillis = Math.max(500,rechargeMillis); delayTicks = Math.max(0,delayTicks); cost = Math.max(0,cost);
    }
    public static RunicSettings current() { return current; }
    public static void load(Path file) {
        Properties properties = new Properties();
        try {
            if (Files.exists(file)) try(var reader=Files.newBufferedReader(file,StandardCharsets.UTF_8)) { properties.load(reader); }
            current = new RunicSettings(integer(properties,"runic_recharge_speed",2000),integer(properties,"runic_recharge_delay",80),integer(properties,"runic_cost",50));
        } catch (IOException | IllegalArgumentException failure) {
            Thaumcraft.LOG.error("Cannot read runic server settings from {}; using original defaults",file,failure); current = DEFAULTS;
        }
    }
    private static int integer(Properties properties,String key,int fallback) {
        try { return Integer.parseInt(properties.getProperty(key,Integer.toString(fallback)).strip()); }
        catch(NumberFormatException invalid) { return fallback; }
    }
}
