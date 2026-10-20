package thaumcraft.common.lib.essentia;

import java.util.function.IntUnaryOperator;
import net.minecraft.core.Direction;

/** The numbers of the Essentia Reservoir (original TileEssentiaReservoir, BlockEssentiaReservoir) without a world. */
public final class ReservoirRules {
    /** 256 units in total over all aspects; suction 24 while it is not full; one unit drawn every fifth tick. */
    public static final int CAPACITY = 256, SUCTION = 24, PULL_INTERVAL = 5;
    /** A reservoir that held at least {@link #BURST_UNIT} units bursts when removed: explosion power 1, up to 50 tries in a 9-block cube, flux level 8. */
    public static final int BURST_UNIT = 16, BURST_TRIES = 50, BURST_SPREAD = 5, FLUX_LEVEL = 8;
    public static final float BURST_POWER = 1.0F;
    /** The creak on the client: a one in {@code 500 - units} chance per tick. */
    public static final int CREAK_BOUND = 500;

    private ReservoirRules() {}

    public static int space(int units) { return CAPACITY - units; }

    public static int suction(int units) { return units < CAPACITY ? SUCTION : 0; }

    /** Original getComparatorInputOverride: {@code floor(units / 256 * 14)} in floats, plus one when anything is stored. */
    public static int comparator(int units) {
        float ratio = (float) units / (float) CAPACITY;
        return (int) Math.floor(ratio * 14.0F) + (units > 0 ? 1 : 0);
    }

    /** Original breakBlock: {@code units / 16} sets how many flux blocks follow the first (units 0..15 burst nothing). */
    public static int sprays(int units) { return units / BURST_UNIT; }

    /** One axis of the burst offset: {@code nextInt(5) - nextInt(5)}, the first draw taken first. */
    public static int burstOffset(IntUnaryOperator nextInt) {
        int plus = nextInt.applyAsInt(BURST_SPREAD);
        return plus - nextInt.applyAsInt(BURST_SPREAD);
    }

    /** Goo below the reservoir's own level, gas at or above it. */
    public static boolean gooAt(int y, int originY) { return y < originY; }

    /** The loop stops after the placement made with {@code placedBefore} already placed once that many exceed the sprays (sprays + 1 in all). */
    public static boolean burstDone(int placedBefore, int sprays) { return placedBefore >= sprays; }

    /** A wand turns the opening toward the clicked side while sneaking and the opposite side otherwise; a placed block opens toward what was clicked. */
    public static Direction wandFacing(Direction clicked, boolean sneaking) { return sneaking ? clicked : clicked.getOpposite(); }
    public static Direction placedFacing(Direction clicked) { return clicked.getOpposite(); }

    /** The creak sounds for a roll in [0, 500 - units). */
    public static boolean creaks(int roll) { return roll == 0; }
    public static int creakBound(int units) { return CREAK_BOUND - units; }
}
