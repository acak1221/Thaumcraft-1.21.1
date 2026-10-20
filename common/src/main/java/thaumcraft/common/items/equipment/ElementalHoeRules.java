package thaumcraft.common.items.equipment;

/**
 * The numbers of the Hoe of Growth (original ItemElementalHoe): a click that tills nothing tries bone meal on the clicked block (one use),
 * and failing that a magical sapling: a greatwood sapling grows into its tree for 5 uses while at least 20 remain, a silverwood sapling for
 * 25 while at least 150 remain.
 */
public final class ElementalHoeRules {
    /** Tilling reaches this far on each side of the clicked block, in its own layer (a 3 x 3 square). */
    public static final int TILL_RADIUS = 1;
    /** Uses a successful bone meal costs, original {@code damageItem(1)}. */
    public static final int BONEMEAL_WEAR = 1;
    /** Uses that must remain, and uses paid, for a greatwood and a silverwood sapling (originals 20 / 5 and 150 / 25). */
    public static final int GREATWOOD_MARGIN = 20, GREATWOOD_WEAR = 5, SILVERWOOD_MARGIN = 150, SILVERWOOD_WEAR = 25;

    private ElementalHoeRules() {}

    public static int margin(boolean silverwood) { return silverwood ? SILVERWOOD_MARGIN : GREATWOOD_MARGIN; }

    public static int wear(boolean silverwood) { return silverwood ? SILVERWOOD_WEAR : GREATWOOD_WEAR; }

    /** Original {@code getItemDamage() + margin <= getMaxDamage()}: the margin may be used up exactly. */
    public static boolean mayGrow(int damage, int maxDamage, boolean silverwood) { return damage + margin(silverwood) <= maxDamage; }
}
