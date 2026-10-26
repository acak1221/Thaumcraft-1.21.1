package thaumcraft.common.config;

import java.util.Properties;

/**
 * Original PacketConfig's seven server facts; registry keys replace its unrelated numeric IDs.
 *
 * <p>The eighth fact is the port's own {@code aspect_inference}: the client resolves item aspects
 * for its tooltips and overlays, so it has to resolve them like the server.</p>
 */
public record ResearchSettings(boolean cheatSheet, boolean wardedStone, boolean mirrors, boolean hardNodes,
                               boolean wuss, int difficulty, int aspectCap, boolean aspectInference) {
    public static final ResearchSettings DEFAULTS = new ResearchSettings(false, true, true, true, false, 0, 100, true);

    /** The original seven facts, with the port's aspect inference at its default. */
    public ResearchSettings(boolean cheatSheet, boolean wardedStone, boolean mirrors, boolean hardNodes,
                            boolean wuss, int difficulty, int aspectCap) {
        this(cheatSheet, wardedStone, mirrors, hardNodes, wuss, difficulty, aspectCap, true);
    }

    /** Original GuiResearchBrowser:644. Other integer values keep the original note path. */
    public boolean purchases(boolean secondary) { return difficulty == -1 || difficulty == 0 && secondary; }

    public ResearchSettings withFeatures(boolean wards, boolean allowMirrors) {
        return new ResearchSettings(cheatSheet, wards, allowMirrors, hardNodes, wuss, difficulty, aspectCap, aspectInference);
    }

    /** Keeps original Config:390's getInt(8) fallback for malformed research_difficulty.
     * The normal documented modes are -1..1; other integers, including that original fallback,
     * deliberately take the note path in purchases rather than introducing a new mode.
     */
    public static ResearchSettings read(Properties properties) {
        return new ResearchSettings(bool(properties, "allow_cheat_sheet", false), bool(properties, "allow_warded_stone", true, false),
                bool(properties, "allow_mirrors", true), bool(properties, "hard_mode_nodes", true),
                bool(properties, "wuss_mode", false), integer(properties, "research_difficulty", 0, 8),
                integer(properties, "aspect_total_cap", 100, 100), bool(properties, "aspect_inference", true));
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
