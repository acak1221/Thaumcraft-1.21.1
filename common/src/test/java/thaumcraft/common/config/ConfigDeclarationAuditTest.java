package thaumcraft.common.config;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Oracle Config.syncConfigurable:369-502 and static initializer:830-895. */
class ConfigDeclarationAuditTest {
    private static final Map<String, String> DEFAULTS = Map.ofEntries(
            Map.entry("allow_mirrors", "true"), Map.entry("hard_mode_nodes", "true"),
            Map.entry("wuss_mode", "false"), Map.entry("allow_cheat_sheet", "false"),
            Map.entry("allow_warded_stone", "true"), Map.entry("blue_magical_forest", "false"),
            Map.entry("glowing_taint", "true"), Map.entry("shaders", "true"),
            Map.entry("biome_taint_spread", "200"), Map.entry("spawn_angry_zombies", "true"),
            Map.entry("spawn_fire_bats", "true"), Map.entry("spawn_wisps", "true"),
            Map.entry("spawn_taintacles", "true"), Map.entry("spawn_taint_spores", "true"),
            Map.entry("spawn_pechs", "true"), Map.entry("spawn_eldercreatures", "true"),
            Map.entry("generate_aura_nodes", "true"), Map.entry("generate_structures", "true"),
            Map.entry("generate_cinnibar_ore", "true"), Map.entry("generate_amber_ore", "true"),
            Map.entry("generate_infused_stone", "true"), Map.entry("generate_trees", "true"),
            Map.entry("generate_taint", "true"), Map.entry("research_difficulty", "0"),
            Map.entry("aspect_total_cap", "100"), Map.entry("magical_forest_biome_weight", "5"),
            Map.entry("taint_biome_weight", "2"), Map.entry("regen_key", "DEFAULT"),
            Map.entry("aura_nodes", "false"), Map.entry("structures", "false"),
            Map.entry("cinnibar_ore", "false"), Map.entry("amber_ore", "false"),
            Map.entry("infused_stone", "false"), Map.entry("trees", "false"),
            Map.entry("runic_recharge_speed", "2000"), Map.entry("runic_recharge_delay", "80"),
            Map.entry("runic_cost", "50"), Map.entry("notification_delay", "5000"),
            Map.entry("notification_max", "15"),
            // The port's own Tweaks category (ThaumcraftTweaks.DEFAULTS), not oracle keys.
            Map.entry("more_randomized_loot", "true"), Map.entry("earth_shock_targets", "2"),
            Map.entry("infusion_keeps_item_data", "true"), Map.entry("filled_container_to_hand", "true"),
            Map.entry("research_search", "true"), Map.entry("research_counter", "true"),
            Map.entry("notification_clear_button", "true"));

    @Test void everyEditableKeyHasAnExplicitRuntimeParserNotJustAnEditor() throws Exception {
        Path root = Path.of("src/main/java/thaumcraft/common/config");
        StringBuilder parsers = new StringBuilder();
        try (var files = Files.list(root)) {
            for (Path file : files.filter(p -> p.getFileName().toString().endsWith("Settings.java")).toList()) {
                parsers.append(Files.readString(file).replaceAll("(?s)/\\*.*?\\*/|//[^\\n]*", ""));
            }
        }
        parsers.append(Files.readString(Path.of("src/main/java/thaumcraft/common/lib/runic/RunicSettings.java")));
        parsers.append(Files.readString(Path.of("src/main/java/thaumcraft/client/lib/PlayerNotifications.java")));
        parsers.append(Files.readString(Path.of("src/main/java/thaumcraft/common/config/ThaumcraftTweaks.java")));
        for (String key : DEFAULTS.keySet()) {
            assertTrue(parsers.toString().contains("\"" + key + "\""), "no runtime reader: " + key);
        }
        Set<String> keys = new HashSet<>();
        for (var entry : ThaumcraftConfigStore.ENTRIES) {
            assertTrue(keys.add(entry.key()), "duplicate key " + entry.key());
            assertTrue(parsers.toString().contains("\"" + entry.key() + "\""), "no runtime reader: " + entry.key());
        }
        assertEquals(32, keys.size());
        assertEquals(46, DEFAULTS.size());
    }

    @Test void editorDefaultsMatchTheOracleAndDoNotSilentlyDisableGlowingTaint(@TempDir Path directory) {
        Properties missing = new Properties();
        for (var entry : ThaumcraftConfigStore.ENTRIES) {
            assertEquals(DEFAULTS.get(entry.key()), entry.value(missing), entry.key());
        }
        Path file = directory.resolve("thaumcraft.properties");
        ThaumcraftConfigStore.write(file, missing);
        Properties written = ThaumcraftConfigStore.read(file);
        assertEquals("true", written.getProperty("glowing_taint"));
        assertEquals(ThaumcraftTaintSettings.DEFAULTS.glowing(), Boolean.parseBoolean(written.getProperty("glowing_taint")));
    }

    @Test void allRuntimeParserDefaultsMatchTheOracle() {
        Properties empty = new Properties();
        assertEquals(new ResearchSettings(false, true, true, true, false, 0, 100), ResearchSettings.read(empty));
        assertEquals(new BiomeSettings(5, 2, true, true, false), BiomeSettings.read(empty));
        assertEquals(new OriginalSpawnSettings(true, true, true, true, true, true), OriginalSpawnSettings.read(empty));
        assertEquals(new OriginalWorldgenSettings(true, true, true, true, true), OriginalWorldgenSettings.read(empty));
        assertEquals(new OriginalRegenSettings(false, false, false, false, false, false, "DEFAULT"), OriginalRegenSettings.read(empty));
        assertEquals(new ThaumcraftTaintSettings.Values(200, true, true), ThaumcraftTaintSettings.DEFAULTS);
        assertEquals(new thaumcraft.common.lib.runic.RunicSettings(2000, 80, 50),
                thaumcraft.common.lib.runic.RunicSettings.DEFAULTS);
        assertEquals(ThaumcraftTweaks.DEFAULTS, readTweaks(empty));
        assertEquals(46, DEFAULTS.size());
    }

    private static ThaumcraftTweaks.Values readTweaks(Properties properties) {
        ThaumcraftTweaks.Values before = ThaumcraftTweaks.values();
        try {
            ThaumcraftTweaks.load(properties);
            return ThaumcraftTweaks.values();
        } finally {
            ThaumcraftTweaks.configure(before);
        }
    }
}
