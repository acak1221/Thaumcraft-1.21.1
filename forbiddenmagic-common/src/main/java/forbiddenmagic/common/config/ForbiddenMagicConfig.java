package forbiddenmagic.common.config;

/**
 * Original Config.java startup toggles. Only the two flags checkpoint 1's content actually
 * reads are ported; a real config file (the original's Forge Configuration, the sibling
 * forbidden-magic-reforged port's forbiddenmagic-startup.toml) is NOT wired up yet &mdash;
 * these are static defaults matching the original's own defaults until that lands.
 * See docs/addons/forbidden-magic/STATUS.md.
 */
public final class ForbiddenMagicConfig {
    /** Original {@code Config.noLust} default false: Luxuria and its items are enabled. */
    public static boolean noLust = false;

    /** Original {@code Config.silverfishEmeralds} default true; unused until Silverfish drops are ported. */
    public static boolean silverfishEmeralds = true;

    private ForbiddenMagicConfig() {}
}
