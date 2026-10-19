package thaumcraft.common.items.golems;

/**
 * Original ItemGolemUpgrade metadata 0..5 (Air, Earth, Fire, Water, Order, Entropy).
 * Every id stays part of the data model, but only a type with an implemented and
 * verified effect may be stored, crafted or installed; the others are refused rather
 * than registered as inert variants. Original ItemGolemUpgrade:31-77.
 */
public enum GolemUpgrade {
    AIR(0, "air", true),
    EARTH(1, "earth", true),
    FIRE(2, "fire", true),
    WATER(3, "water", true),
    ORDER(4, "order", true),
    ENTROPY(5, "entropy", true);

    /** Original byte for an empty slot. */
    public static final int EMPTY = -1;
    private static final GolemUpgrade[] VALUES = values();

    public final int id;
    public final String key;
    private final boolean ported;

    GolemUpgrade(int id, String key, boolean ported) {
        this.id = id;
        this.key = key;
        this.ported = ported;
    }

    /** True only when this port has a real, verified effect for the type. */
    public boolean isSupported() { return ported; }

    public static GolemUpgrade byId(int id) { return id >= 0 && id < VALUES.length ? VALUES[id] : null; }

    /** An id the port can store, craft and apply. {@link #EMPTY} is not an upgrade. */
    public static boolean isSupported(int id) {
        GolemUpgrade upgrade = byId(id);
        return upgrade != null && upgrade.ported;
    }
}
