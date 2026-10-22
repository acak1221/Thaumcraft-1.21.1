package thaumcraft.common.config;

import java.util.Properties;

/**
 * The regeneration options of the original.
 *
 * <p>Oracle {@code thaumcraft/common/config/Config.java:378-386}:</p>
 * <pre>
 * Property regKey = config.get(CATEGORY_REGEN, "regen_key", "DEFAULT");
 * regKey.comment = "This key is used to keep track of which chunk have been generated/regenerated. ...";
 * regenKey = regKey.getString();
 * regenAura = config.get(CATEGORY_REGEN, "aura_nodes", false).getBoolean(false);
 * regenStructure = config.get(CATEGORY_REGEN, "structures", false).getBoolean(false);
 * regenCinnibar = config.get(CATEGORY_REGEN, "cinnibar_ore", false).getBoolean(false);
 * regenAmber = config.get(CATEGORY_REGEN, "amber_ore", false).getBoolean(false);
 * regenInfusedStone = config.get(CATEGORY_REGEN, "infused_stone", false).getBoolean(false);
 * regenTrees = config.get(CATEGORY_REGEN, "trees", false).getBoolean(false);
 * </pre>
 *
 * <p>Every option of the regeneration category is <em>off</em> in the original, so the chunk
 * regeneration of the original never touches a chunk until a player switches one of them on.
 * The port keeps that default.</p>
 */
public record OriginalRegenSettings(boolean auraNodes, boolean structures, boolean cinnibarOre, boolean amberOre,
                                     boolean infusedStone, boolean trees, String regenKey) {
    /** The default of the original: every regeneration option is off and the key is DEFAULT. */
    public static final OriginalRegenSettings DEFAULTS =
            new OriginalRegenSettings(false, false, false, false, false, false, "DEFAULT");

    public static OriginalRegenSettings read(Properties values) {
        return new OriginalRegenSettings(flag(values, "aura_nodes"), flag(values, "structures"),
                flag(values, "cinnibar_ore"), flag(values, "amber_ore"), flag(values, "infused_stone"),
                flag(values, "trees"), key(values, "regen_key"));
    }

    /**
     * Oracle EventHandlerWorld:125 - the chunk is only queued when one of the six regeneration
     * options is on.
     */
    public boolean anyRegeneration() {
        return auraNodes || structures || cinnibarOre || amberOre || infusedStone || trees;
    }

    private static boolean flag(Properties values, String key) {
        String value = values.getProperty(key, "false").trim();
        if (value.equalsIgnoreCase("true")) return true;
        if (value.equalsIgnoreCase("false")) return false;
        throw new IllegalArgumentException("Invalid original regeneration option " + key);
    }

    private static String key(Properties values, String name) {
        String value = values.getProperty(name, "DEFAULT").trim();
        if (value.isEmpty()) throw new IllegalArgumentException("The original regeneration key is not empty");
        return value;
    }
}
