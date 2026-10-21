package thaumcraft.common.items.golems;

/**
 * Pure rules of the Order upgrade's Guard target switches (original EntityGolemBase:800-831, 1150-1180, GuiGolem:167-189,
 * ContainerGolem:92). The original keeps four <em>negative</em> bits in the entity's {@code toggles} byte (bit 1 Monsters,
 * 2 Animals, 3 Players, 4 Creepers; a set bit forbids the category) and a fresh Guard has every bit clear, so an Order Guard
 * attacks monsters, animals, players and creepers until the owner switches a category off.
 *
 * <p>The mask of this port is the original's byte without its other cores' bits: a set bit forbids the category, a clear bit
 * allows it. The default forbids nothing, exactly like a fresh original Guard.
 *
 * <p>Menu buttons keep the original ids ({@code 50 + toggle index}): 51 Monsters, 52 Animals, 53 Players, 54 Creepers.
 */
public final class GolemGuardTargets {
    public static final int MONSTERS = 1;
    public static final int ANIMALS = 2;
    public static final int PLAYERS = 4;
    public static final int CREEPERS = 8;
    public static final int ALL = MONSTERS | ANIMALS | PLAYERS | CREEPERS;
    /** Original ContainerGolem button {@code 50 + index}: the first switch (Monsters) is toggles[1]. */
    public static final int FIRST_BUTTON = 51;
    public static final int BUTTON_COUNT = 4;
    /** The original's fresh Guard: every switch clear, so every category may be attacked. */
    public static final int DEFAULT = 0;

    private GolemGuardTargets() {}

    public static boolean valid(int mask) { return (mask & ~ALL) == 0; }

    public static boolean isDefault(int mask) { return mask == DEFAULT; }

    /** Whether the category may be attacked: the original's {@code canAttackX()} with a clear toggle bit. */
    public static boolean allows(int mask, int category) { return isCategory(category) && (mask & category) == 0; }

    /** Whether the category is switched off: the original's {@code canAttackX()} with a set toggle bit. */
    public static boolean forbids(int mask, int category) { return isCategory(category) && (mask & category) != 0; }

    public static int toggle(int mask, int category) {
        return isCategory(category) ? mask ^ category : mask;
    }

    /** Whether the value is exactly one of the four categories the original's switches control. */
    public static boolean isCategory(int category) { return Integer.bitCount(category) == 1 && (category & ALL) == category; }

    /** The category a menu button controls, or 0 for any other button id. */
    public static int categoryOfButton(int button) {
        return button >= FIRST_BUTTON && button < FIRST_BUTTON + BUTTON_COUNT ? 1 << (button - FIRST_BUTTON) : 0;
    }

    public static int buttonOf(int category) {
        return isCategory(category) ? FIRST_BUTTON + Integer.numberOfTrailingZeros(category) : -1;
    }

    /** The switches exist only on a Guard core that carries an Order upgrade (original GuiGolem:167). */
    public static boolean configurable(int core, int orderUpgrades) { return core == GolemData.GUARD && orderUpgrades > 0; }

    /** A non-default mask is configuration this golem cannot honour without its Guard core and Order upgrade. */
    public static boolean acceptable(int mask, int core, int orderUpgrades) {
        return valid(mask) && (isDefault(mask) || configurable(core, orderUpgrades));
    }

    /** Original {@code toggles} byte for a mask: a set bit forbids the category (bits 1..4, others clear). */
    public static byte toOriginalToggles(int mask) { return (byte) ((mask & ALL) << 1); }

    /** Inverse of {@link #toOriginalToggles}; the original's other toggle bits (Fill/Use/Entropy) are not Guard state. */
    public static int fromOriginalToggles(byte toggles) { return (toggles >> 1) & ALL; }
}
