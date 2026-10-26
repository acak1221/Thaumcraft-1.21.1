package thaumcraft.common.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * The editable configuration of the port and the file it lives in.
 *
 * <p>The original TC4 4.2.3.5 kept its settings in a Forge configuration and showed them in the
 * in-game screen of {@code thaumcraft/client/ThaumcraftGuiFactory.java:24-25} and
 * {@code thaumcraft/client/gui/ThaumcraftGuiConfig.java:10-34}. That screen offered exactly five
 * categories of the original {@code Config} (oracle
 * {@code thaumcraft/common/config/Config.java:104-111}): {@code general},
 * {@code Monster_Spawning}, {@code World_Generation}, {@code World_Regeneration} and
 * {@code Research}.
 *
 * <p>1.21.1 has no Forge configuration screen, so the port keeps the settings in the properties
 * file {@code config/thaumcraft.properties} and this store is the catalogue that the native
 * screen edits. Every entry names the original category and the original key, so the screen shows
 * the original layout with the settings the port really has. The sixth category, {@code Tweaks},
 * is the port's own: the improvements adopted from TC4Tweaks.
 */
public final class ThaumcraftConfigStore {
    /** The original category of a setting. */
    public enum Category {
        GENERAL("general"),
        MONSTER_SPAWNING("Monster_Spawning"),
        WORLD_GENERATION("World_Generation"),
        WORLD_REGENERATION("World_Regeneration"),
        RESEARCH("Research"),
        /** Not an original category: the improvements adopted from TC4Tweaks ({@link ThaumcraftTweaks}). */
        TWEAKS("Tweaks");
        private final String id;
        Category(String id) { this.id = id; }
        public String id() { return id; }
    }

    /** One editable setting. */
    public record Entry(Category category, String key, boolean flag, boolean defaultFlag, int defaultNumber, int min, int max) {
        public String value(Properties values) {
            String raw = values.getProperty(key);
            if (raw == null) return flag ? Boolean.toString(defaultFlag) : Integer.toString(defaultNumber);
            return raw.strip();
        }
        public boolean booleanValue(Properties values) { return Boolean.parseBoolean(value(values)); }
        public int integerValue(Properties values) {
            try { return Integer.parseInt(value(values)); }
            catch (NumberFormatException invalid) { return defaultNumber; }
        }
    }

    /** The header of the file the port writes. */
    private static final String[] HEADER = {
            "# Thaumcraft server research settings; restart required.",
            "# research_difficulty: -1 purchases all, 0 purchases Secondary, 1 uses notes for all."};

    /**
     * The settings of the port in the categories of the original. The keys and their defaults are
     * the ones of the original {@code Config} (oracle
     * {@code thaumcraft/common/config/Config.java:369-500}), the file itself is the port's own.
     */
    public static final List<Entry> ENTRIES = List.of(
            // Original general category.
            new Entry(Category.GENERAL, "allow_mirrors", true, true, 0, 0, 0),
            new Entry(Category.GENERAL, "hard_mode_nodes", true, true, 0, 0, 0),
            new Entry(Category.GENERAL, "wuss_mode", true, false, 0, 0, 0),
            new Entry(Category.GENERAL, "allow_cheat_sheet", true, false, 0, 0, 0),
            new Entry(Category.GENERAL, "allow_warded_stone", true, true, 0, 0, 0),
            new Entry(Category.GENERAL, "blue_magical_forest", true, false, 0, 0, 0),
            new Entry(Category.GENERAL, "glowing_taint", true, true, 0, 0, 0),
            new Entry(Category.GENERAL, "shaders", true, true, 0, 0, 0),
            new Entry(Category.GENERAL, "biome_taint_spread", false, false, 200, 0, 1000),
            // Original Monster_Spawning category.
            new Entry(Category.MONSTER_SPAWNING, "spawn_angry_zombies", true, true, 0, 0, 0),
            new Entry(Category.MONSTER_SPAWNING, "spawn_fire_bats", true, true, 0, 0, 0),
            new Entry(Category.MONSTER_SPAWNING, "spawn_wisps", true, true, 0, 0, 0),
            new Entry(Category.MONSTER_SPAWNING, "spawn_taintacles", true, true, 0, 0, 0),
            new Entry(Category.MONSTER_SPAWNING, "spawn_taint_spores", true, true, 0, 0, 0),
            new Entry(Category.MONSTER_SPAWNING, "spawn_pechs", true, true, 0, 0, 0),
            new Entry(Category.MONSTER_SPAWNING, "spawn_eldercreatures", true, true, 0, 0, 0),
            // Original World_Generation category.
            new Entry(Category.WORLD_GENERATION, "generate_aura_nodes", true, true, 0, 0, 0),
            new Entry(Category.WORLD_GENERATION, "generate_structures", true, true, 0, 0, 0),
            new Entry(Category.WORLD_GENERATION, "generate_cinnibar_ore", true, true, 0, 0, 0),
            new Entry(Category.WORLD_GENERATION, "generate_amber_ore", true, true, 0, 0, 0),
            new Entry(Category.WORLD_GENERATION, "generate_infused_stone", true, true, 0, 0, 0),
            new Entry(Category.WORLD_GENERATION, "generate_trees", true, true, 0, 0, 0),
            new Entry(Category.WORLD_GENERATION, "generate_taint", true, true, 0, 0, 0),
            // The original World_Regeneration category (oracle Config.java:381-387) has no
            // counterpart in the port: the port does not regenerate chunks at all.
            // Original Research category.
            new Entry(Category.RESEARCH, "research_difficulty", false, false, 0, -1, 1),
            new Entry(Category.RESEARCH, "aspect_total_cap", false, false, 100, 100, 1000),
            // Port settings, not original keys: ThaumcraftTweaks.DEFAULTS.
            new Entry(Category.TWEAKS, "more_randomized_loot", true, true, 0, 0, 0),
            new Entry(Category.TWEAKS, "earth_shock_targets", false, false, 2, 0, 2),
            new Entry(Category.TWEAKS, "infusion_keeps_item_data", true, true, 0, 0, 0),
            new Entry(Category.TWEAKS, "filled_container_to_hand", true, true, 0, 0, 0));

    private ThaumcraftConfigStore() {}

    /** The file the port reads its settings from. */
    public static Path file() {
        return dev.architectury.platform.Platform.getConfigFolder().resolve("thaumcraft.properties");
    }

    /** The settings of one category in the order the screen shows them. */
    public static List<Entry> entries(Category category) {
        List<Entry> selected = new ArrayList<>();
        for (Entry entry : ENTRIES) if (entry.category() == category) selected.add(entry);
        return selected;
    }

    /** The values of the file, or the defaults when the file does not exist. */
    public static Properties read(Path file) {
        Properties values = new Properties();
        if (file != null && Files.exists(file)) {
            try (var reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) { values.load(reader); }
            catch (IOException failure) { thaumcraft.common.Thaumcraft.LOG.error("Cannot read the Thaumcraft configuration at {}", file, failure); }
        }
        return values;
    }

    /**
     * Writes the values back. The known keys keep the order of the file the port creates and every
     * other key of the file is kept, so a hand written file survives the screen.
     */
    public static void write(Path file, Properties values) {
        Map<String, String> ordered = new LinkedHashMap<>();
        for (Entry entry : ENTRIES) ordered.put(entry.key(), values.getProperty(entry.key(), entry.flag()
                ? Boolean.toString(entry.defaultFlag()) : Integer.toString(entry.defaultNumber())));
        for (String key : values.stringPropertyNames()) if (!ordered.containsKey(key)) ordered.put(key, values.getProperty(key));
        StringBuilder text = new StringBuilder();
        for (String line : HEADER) text.append(line).append('\n');
        for (Map.Entry<String, String> row : ordered.entrySet()) text.append(row.getKey()).append('=').append(row.getValue()).append('\n');
        try {
            if (file.getParent() != null) Files.createDirectories(file.getParent());
            Files.writeString(file, text.toString(), StandardCharsets.UTF_8);
        } catch (IOException failure) {
            thaumcraft.common.Thaumcraft.LOG.error("Cannot write the Thaumcraft configuration at {}", file, failure);
        }
    }
}
