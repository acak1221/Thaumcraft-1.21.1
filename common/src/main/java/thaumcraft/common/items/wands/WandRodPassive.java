package thaumcraft.common.items.wands;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntUnaryOperator;

/**
 * Original WandRodPrimalOnUpdate (ConfigItems:298-313). An elemental rod (EARTH, FIRE, WATER, ORDER, ENTROPY or AIR, wand or staff) has a
 * fixed aspect: every 200th tick of the player carrying the wand, if the wand holds less than a tenth of its capacity of that aspect, one
 * whole vis (100 centivis) of it is added, up to the capacity. The primal staff ({@code new WandRodPrimalOnUpdate()}) has no fixed
 * aspect: every 50th tick it adds one whole vis to ONE aspect picked at random among the primals that are below a tenth of the capacity.
 * The wand is looked at on every tick of the carrier, once per stack.
 */
public final class WandRodPassive {
    /** Ticks between two recharges of a fixed-aspect rod, counted on the carrying player's age. */
    public static final int PERIOD = 200;
    /** Ticks between two recharges of the primal staff. */
    public static final int PRIMAL_PERIOD = 50;
    /** One whole vis in centivis, original {@code addVis(..., 1, true)}. */
    public static final int STEP = 100;

    private WandRodPassive() {}

    /** The level below which the rod refills its aspect: a tenth of the capacity, integer division like the original. */
    public static int threshold(int capacity) { return capacity / 10; }

    /**
     * The wand after this tick of its carrier: the same instance when nothing happens.
     *
     * @param pick {@code nextInt(bound)} of the carrier's random source, asked only by the primal staff and only when a primal is low
     */
    public static WandData tick(WandData wand, int playerTick, IntUnaryOperator pick) {
        String aspect = WandMaterials.rod(wand.rod()).passive();
        if (aspect == null) return wand;
        if (WandMaterials.ANY_PRIMAL.equals(aspect)) {
            if (playerTick % PRIMAL_PERIOD != 0) return wand;
            List<String> low = new ArrayList<>();
            for (String primal : WandData.PRIMALS) if (wand.amount(primal) < threshold(wand.capacity())) low.add(primal);
            if (low.isEmpty()) return wand;
            String chosen = low.get(pick.applyAsInt(low.size()));
            return wand.withAmount(chosen, wand.amount(chosen) + STEP);
        }
        if (playerTick % PERIOD != 0 || wand.amount(aspect) >= threshold(wand.capacity())) return wand;
        return wand.withAmount(aspect, wand.amount(aspect) + STEP);
    }
}
