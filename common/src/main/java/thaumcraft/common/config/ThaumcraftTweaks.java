package thaumcraft.common.config;

import java.util.Objects;
import java.util.Properties;

/**
 * Small gameplay improvements that differ from TC4 4.2.3.5 on purpose, each one a setting.
 *
 * <p>The ideas come from TC4Tweaks, the 1.7.10 companion mod of TC4 (github.com/Glease/TC4Tweaks,
 * AGPL-3.0). No code of it is used: it patched the 1.7.10 bytecode, and the port re-implements
 * each idea on its own code. Every setting defaults to the TC4Tweaks default and is an accepted
 * deviation (docs/audit/KNOWN_DEVIATIONS.md §8); switched off, the original rule applies.</p>
 */
public final class ThaumcraftTweaks {
    /** What earth shock hurts: the shock orb's burst and the shock field it leaves behind. */
    public enum EarthShockTargets {
        /** The original: every entity, so dropped items and experience orbs are destroyed. */
        ALL,
        /** Every entity except dropped items and experience orbs. */
        EXCEPT_ITEMS_AND_XP,
        /** Living entities only, the TC4Tweaks default. */
        ONLY_LIVING;

        public static EarthShockTargets byId(int id) {
            EarthShockTargets[] values = values();
            return id >= 0 && id < values.length ? values[id] : ONLY_LIVING;
        }
    }

    /**
     * @param moreRandomizedLoot a loot bag, urn or crate enchants every book it hands out and charges
     *                           every vis amulet anew, instead of the original once-per-server-run copy
     * @param earthShock         what earth shock hurts
     * @param infusionKeepsData  infusing armour or a tool keeps the enchantments, name and other data of
     *                           the centre item on the result, unless the recipe sets them itself
     * @param filledToHand       filling a phial or a jar with the last empty one in hand puts the filled
     *                           one into that hand, unless a stack of it already has room
     */
    public record Values(boolean moreRandomizedLoot, EarthShockTargets earthShock, boolean infusionKeepsData,
                         boolean filledToHand) {
        public Values { Objects.requireNonNull(earthShock, "earth shock"); }
    }

    /** The TC4Tweaks defaults. */
    public static final Values DEFAULTS = new Values(true, EarthShockTargets.ONLY_LIVING, true, true);
    /** Every original TC4 4.2.3.5 rule. */
    public static final Values ORIGINAL = new Values(false, EarthShockTargets.ALL, false, false);

    private static volatile Values values = DEFAULTS;

    private ThaumcraftTweaks() {}

    public static Values values() { return values; }
    public static boolean moreRandomizedLoot() { return values.moreRandomizedLoot(); }
    public static EarthShockTargets earthShock() { return values.earthShock(); }
    public static boolean infusionKeepsData() { return values.infusionKeepsData(); }
    public static boolean filledToHand() { return values.filledToHand(); }

    public static void configure(Values input) { values = Objects.requireNonNull(input); }

    /** Reads the settings; a missing or unreadable key keeps its default. */
    public static void load(Properties properties) {
        configure(new Values(
                flag(properties, "more_randomized_loot", DEFAULTS.moreRandomizedLoot()),
                EarthShockTargets.byId(number(properties, "earth_shock_targets", DEFAULTS.earthShock().ordinal())),
                flag(properties, "infusion_keeps_item_data", DEFAULTS.infusionKeepsData()),
                flag(properties, "filled_container_to_hand", DEFAULTS.filledToHand())));
    }

    /** Whether earth shock hurts an entity of this kind under the given setting. */
    public static boolean earthShockHurts(EarthShockTargets targets, boolean living, boolean itemOrExperience) {
        return switch (targets) {
            case ALL -> true;
            case EXCEPT_ITEMS_AND_XP -> !itemOrExperience;
            case ONLY_LIVING -> living;
        };
    }

    /** Whether earth shock hurts this entity under the current setting. */
    public static boolean earthShockHurts(net.minecraft.world.entity.Entity entity) {
        return earthShockHurts(values.earthShock(), entity instanceof net.minecraft.world.entity.LivingEntity,
                entity instanceof net.minecraft.world.entity.item.ItemEntity
                        || entity instanceof net.minecraft.world.entity.ExperienceOrb);
    }

    /** Applies other settings until closed, then the previous ones again. For tests of either rule. */
    public static Scope override(Values replacement) {
        Values previous = values;
        configure(replacement);
        return new Scope(previous);
    }

    public static final class Scope implements AutoCloseable {
        private final Values previous;
        private Scope(Values previous) { this.previous = previous; }
        @Override public void close() { configure(previous); }
    }

    private static boolean flag(Properties properties, String key, boolean fallback) {
        String raw = properties.getProperty(key);
        if (raw == null) return fallback;
        raw = raw.strip();
        if (raw.equalsIgnoreCase("true")) return true;
        if (raw.equalsIgnoreCase("false")) return false;
        return fallback;
    }

    private static int number(Properties properties, String key, int fallback) {
        String raw = properties.getProperty(key);
        if (raw == null) return fallback;
        try { return Integer.parseInt(raw.strip()); }
        catch (NumberFormatException invalid) { return fallback; }
    }
}
