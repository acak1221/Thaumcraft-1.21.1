package thaumcraft.common.config;

import java.util.Properties;

/** Original PacketConfig's seven server facts; registry keys replace its unrelated numeric IDs. */
public record ResearchSettings(boolean cheatSheet, boolean wardedStone, boolean mirrors, boolean hardNodes,
                               boolean wuss, int difficulty, int aspectCap) {
    public static final ResearchSettings DEFAULTS = new ResearchSettings(false, true, true, true, false, 0, 100);

    /** Original GuiResearchBrowser:644. Other integer values keep the original note path. */
    public boolean purchases(boolean secondary) { return difficulty == -1 || difficulty == 0 && secondary; }

    public ResearchSettings withFeatures(boolean wards, boolean allowMirrors) {
        return new ResearchSettings(cheatSheet, wards, allowMirrors, hardNodes, wuss, difficulty, aspectCap);
    }

    public static ResearchSettings read(Properties properties) {
        return new ResearchSettings(bool(properties, "allow_cheat_sheet", false), bool(properties, "allow_warded_stone", true, false),
                bool(properties, "allow_mirrors", true), bool(properties, "hard_mode_nodes", true),
                bool(properties, "wuss_mode", false), integer(properties, "research_difficulty", 0, 8),
                integer(properties, "aspect_total_cap", 100, 100));
    }
    private static boolean bool(Properties properties, String key, boolean fallback) {
        return bool(properties, key, fallback, fallback);
    }
    private static boolean bool(Properties properties, String key, boolean missing, boolean fallback) {
        String value = properties.getProperty(key);
        if (value == null) return missing;
        if (value.strip().equalsIgnoreCase("true")) return true;
        if (value.strip().equalsIgnoreCase("false")) return false;
        return fallback;
    }
    private static int integer(Properties properties, String key, int missing, int fallback) {
        try { return Integer.parseInt(properties.getProperty(key, Integer.toString(missing)).strip()); }
        catch (NumberFormatException invalid) { return fallback; }
    }
}
