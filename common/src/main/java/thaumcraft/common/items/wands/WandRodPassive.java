package thaumcraft.common.items.wands;

/**
 * Original WandRodPrimalOnUpdate with a fixed aspect (EARTH, FIRE, WATER, ORDER, ENTROPY or AIR rods, ConfigItems:298-303): every 200th
 * tick of the player carrying the wand, if the wand holds less than a tenth of its capacity of the rod's primal aspect, one whole vis
 * (100 centivis) of it is added, up to the capacity. The wand is looked at on every tick of the carrier, once per stack.
 */
public final class WandRodPassive {
    /** Ticks between two recharges, counted on the carrying player's age. */
    public static final int PERIOD = 200;
    /** One whole vis in centivis, original {@code addVis(..., 1, true)}. */
    public static final int STEP = 100;

    private WandRodPassive() {}

    /** The level below which the rod refills its aspect: a tenth of the capacity, integer division like the original. */
    public static int threshold(int capacity) { return capacity / 10; }

    /** The wand after this tick of its carrier: the same instance when nothing happens. */
    public static WandData tick(WandData wand, int playerTick) {
        String aspect = WandMaterials.rod(wand.rod()).passive();
        if (aspect == null || playerTick % PERIOD != 0 || wand.amount(aspect) >= threshold(wand.capacity())) return wand;
        return wand.withAmount(aspect, wand.amount(aspect) + STEP);
    }
}
