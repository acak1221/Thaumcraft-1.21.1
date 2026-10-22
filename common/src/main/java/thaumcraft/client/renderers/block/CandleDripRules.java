package thaumcraft.client.renderers.block;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * The geometry of the original tallow candle.
 *
 * <p>Oracle {@code thaumcraft/client/renderers/block/BlockCandleRenderer.java:43-66}
 * draws the world candle as the wax body, one to five deterministic wax drips and
 * the wick stub. The number and the shape of the drips come from a
 * {@link Random} seeded with {@code x + y * z}, so they are a property of the block
 * position and not of the time: the same candle always looks the same. A JSON model
 * of Minecraft 1.21.1 cannot read the position, so the drips are computed here and
 * the client model of the loaders emits them.
 *
 * <p>The class is a pure rule: it has no world, no renderer and no random source of
 * the game, so every number can be checked in a unit test.
 */
public final class CandleDripRules {
    /** {@code W1} of the original {@code BlockRenderer}, one sixteenth of a block. */
    public static final float W1 = 0.0625F;
    /** {@code W5} of the original {@code BlockRenderer}. */
    public static final float W5 = 0.3125F;
    /** {@code W6} of the original {@code BlockRenderer}. */
    public static final float W6 = 0.375F;
    /** {@code W8} of the original {@code BlockRenderer}, the half block. */
    public static final float W8 = 0.5F;
    /** {@code W10} of the original {@code BlockRenderer}. */
    public static final float W10 = 0.625F;
    /** {@code W11} of the original {@code BlockRenderer}. */
    public static final float W11 = 0.6875F;
    /** The original wax body, {@code BlockCandleRenderer.java:45}. */
    public static final Box BODY = new Box(W6, 0.0F, W6, W10, W8, W10);
    /** The original wick stub, {@code BlockCandleRenderer.java:63-66}. */
    public static final Box STUB = new Box(0.475F, W8, 0.475F, 0.525F, W10, 0.525F);
    /** The original lower bound of the drip count, {@code 1 + nextInt(5)}. */
    public static final int MIN_DRIPS = 1;
    /** The original upper bound of the drip count, {@code 1 + nextInt(5)}. */
    public static final int MAX_DRIPS = 5;

    private CandleDripRules() {}

    /** An axis aligned box of the original block space, in blocks. */
    public record Box(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {}

    /** The body, the deterministic drips in draw order and the stub of one candle. */
    public record Candle(Box body, List<Box> drips, Box stub) {}

    /**
     * The candle of one block position.
     *
     * <p>The order of the random calls is the order of the original:
     * {@code nextInt(5)} for the count, then for every drip {@code nextBoolean()},
     * {@code nextInt(2)} and {@code nextInt(3)}. The drip is computed from the
     * original bounds expressions of {@code BlockCandleRenderer.java:48-62}.
     *
     * @param x the block x of the world
     * @param y the block y of the world
     * @param z the block z of the world
     * @return the candle of that position
     */
    public static Candle candle(int x, int y, int z) {
        Random random = new Random(x + y * z);
        int count = MIN_DRIPS + random.nextInt(MAX_DRIPS);
        List<Box> drips = new ArrayList<>(count);
        for (int drip = 0; drip < count; ++drip) {
            boolean side = random.nextBoolean();
            int loc = 2 + random.nextInt(2);
            float height = W1 * (1 + random.nextInt(3));
            if (drip % 2 == 0) {
                drips.add(new Box(W5 + W1 * loc, 0.0F, side ? W5 : W10,
                        W6 + W1 * loc, height, side ? W6 : W11));
            } else {
                drips.add(new Box(side ? W5 : W10, 0.0F, W5 + W1 * loc,
                        side ? W6 : W11, height, W6 + W1 * loc));
            }
        }
        return new Candle(BODY, List.copyOf(drips), STUB);
    }
}
