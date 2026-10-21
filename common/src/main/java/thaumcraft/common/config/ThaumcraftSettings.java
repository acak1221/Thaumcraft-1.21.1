package thaumcraft.common.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Properties;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.lib.WarpEvents;

/** Restart-scoped server settings and a separate remote-server snapshot on the client thread. */
public final class ThaumcraftSettings {
    private static volatile ResearchSettings server = ResearchSettings.DEFAULTS;
    private static volatile ResearchSettings remote = ResearchSettings.DEFAULTS;
    private static volatile Thread clientThread;
    /**
     * Original {@code Config.shaders} (oracle {@code thaumcraft/common/config/Config.java:410-412},
     * default {@code true} in the static block at :847): "This setting will disable certain
     * thaumcraft shaders for those who experience FPS drops." The original read it from the
     * client's own Forge configuration file, so it is a local rendering option here as well and
     * it is deliberately not part of the server snapshot.
     */
    private static volatile boolean shaders = true;
    private ThaumcraftSettings() {}

    public static ResearchSettings server() { return server; }
    public static ResearchSettings current() { return Thread.currentThread() == clientThread ? remote : server; }
    /** Original {@code Config.shaders}: the local switch of the four warp post chains. */
    public static boolean shaders() { return shaders; }
    public static void configureServer(ResearchSettings settings) {
        server = java.util.Objects.requireNonNull(settings);
        WarpEvents.setWussMode(settings.wuss());
        // The original published the seven research facts in its public Config as well.
        Config.apply(settings);
    }
    public static void initializeClient() {
        if (clientThread != null && clientThread != Thread.currentThread()) throw new IllegalStateException("Client settings thread changed");
        clientThread = Thread.currentThread(); remote = ResearchSettings.DEFAULTS;
    }
    public static void receive(ResearchSettings settings) {
        requireClientThread(); remote = java.util.Objects.requireNonNull(settings);
    }
    /**
     * Original EventHandlerNetwork.clientLogsOut restores the client's own
     * configuration after a disconnect. A client that never received a remote
     * snapshot has nothing to restore, so the unset client thread is allowed.
     */
    public static void resetClient() {
        if (clientThread != null) requireClientThread();
        remote = ResearchSettings.DEFAULTS;
    }
    private static void requireClientThread() {
        if (Thread.currentThread() != clientThread) throw new IllegalStateException("Remote settings require the client thread");
    }
    public static void load(Path file) {
        Properties properties = new Properties();
        try {
            if (Files.exists(file)) {
                try (var reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) { properties.load(reader); }
            } else {
                Files.createDirectories(file.toAbsolutePath().getParent());
                Files.writeString(file, "# Thaumcraft server research settings; restart required.\n"
                        + "# research_difficulty: -1 purchases all, 0 purchases Secondary, 1 uses notes for all.\n"
                        + "allow_cheat_sheet=false\nallow_warded_stone=true\nallow_mirrors=true\nhard_mode_nodes=true\n"
                        + "wuss_mode=false\nresearch_difficulty=0\naspect_total_cap=100\n"
                        + "biome_taint_spread=200\nspawn_taint_spores=true\nglowing_taint=true\n"
                        + "magical_forest_biome_weight=5\ntaint_biome_weight=2\nblue_magical_forest=false\n"
                        + "spawn_angry_zombies=true\nspawn_pechs=true\nspawn_fire_bats=true\nspawn_wisps=true\n"
                        + "spawn_eldercreatures=true\nspawn_taintacles=true\n"
                        + "generate_trees=true\ngenerate_taint=true\ngenerate_aura_nodes=true\n"
                        + "generate_structures=true\ngenerate_cinnibar_ore=true\ngenerate_amber_ore=true\ngenerate_infused_stone=true\n"
                        + "shaders=true\n",
                        StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
            }
            configureServer(ResearchSettings.read(properties));
            ThaumcraftTaintSettings.load(properties);
            ThaumcraftBiomeSettings.configure(properties);
            // Original Config.shaders: a local rendering switch, not a server fact.
            shaders = !"false".equalsIgnoreCase(properties.getProperty("shaders", "true").strip());
            Config.shaders = shaders;
        } catch (IOException | IllegalArgumentException failure) {
            Thaumcraft.LOG.error("Cannot read Thaumcraft configuration at {}; using original defaults", file, failure);
            configureServer(ResearchSettings.DEFAULTS);
            ThaumcraftTaintSettings.configure(ThaumcraftTaintSettings.DEFAULTS);
            ThaumcraftBiomeSettings.defaults();
            shaders = true;
            Config.shaders = true;
        }
    }
}
