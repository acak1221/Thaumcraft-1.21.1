package thaumcraft.common.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Properties;
import thaumcraft.common.Thaumcraft;

/** One immutable startup snapshot, separate from the research protocol.
 * Local blue forest tint is never overwritten by a remote server snapshot. */
public final class ThaumcraftBiomeSettings {
    private record Snapshot(BiomeSettings biomes,OriginalSpawnSettings spawns,OriginalWorldgenSettings worldgen,OriginalRegenSettings regen) {}
    private static final Snapshot DEFAULTS=new Snapshot(BiomeSettings.DEFAULTS,OriginalSpawnSettings.DEFAULTS,OriginalWorldgenSettings.DEFAULTS,OriginalRegenSettings.DEFAULTS);
    private static volatile Snapshot current=DEFAULTS;
    private ThaumcraftBiomeSettings() {}
    public static BiomeSettings current() {return current.biomes();}
    public static OriginalSpawnSettings spawns() {return current.spawns();}
    public static OriginalWorldgenSettings worldgen() {return current.worldgen();}
    /** The regeneration options of the original (oracle Config:378-386), every one of them off. */
    public static OriginalRegenSettings regen() {return current.regen();}
    /** Common existing-properties loader calls once, before entity/biome hooks.
     * Parse every option before publishing so malformed input never mixes units. */
    public static void configure(Properties values) {
        Objects.requireNonNull(values);current=new Snapshot(BiomeSettings.read(values),OriginalSpawnSettings.read(values),OriginalWorldgenSettings.read(values),OriginalRegenSettings.read(values));
    }
    public static void defaults() {current=DEFAULTS;}
    public static void load(Path file) {
        var properties=new Properties();
        try {
            if(Files.exists(file))try(var reader=Files.newBufferedReader(file,StandardCharsets.UTF_8)){properties.load(reader);}
            configure(properties);
        } catch(IOException|IllegalArgumentException failure) {
            Thaumcraft.LOG.error("Cannot read Thaumcraft biome/worldgen configuration at {}; using original defaults",file,failure);defaults();
        }
    }
}
