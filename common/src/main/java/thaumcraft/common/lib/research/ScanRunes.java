package thaumcraft.common.lib.research;

import net.minecraft.util.RandomSource;

/**
 * The rune particles of the original thaumometer scan.
 *
 * <p>The original draws a rune glyph over the scanned target every tick of the scan. Oracle
 * {@code thaumcraft/common/items/relics/ItemThaumometer.java:92} draws it over the pointed
 * entity, {@code :103} over a node and {@code :128} over a block; all three go through the
 * client proxy helper {@code Thaumcraft.proxy.blockRunes} (oracle
 * {@code thaumcraft/client/ClientProxy.java:918-922}), which creates exactly ONE particle
 * {@code FXBlockRunes} (oracle {@code thaumcraft/client/fx/particles/FXBlockRunes.java:17-98})
 * at {@code (x + 0.5, y + 0.5, z + 0.5)} of the given point.</p>
 *
 * <p>The original scan runs on the client, so the colour of the rune was drawn from the
 * <em>client</em> world random ({@code world.field_73012_v.nextFloat()}). The native scan is
 * server owned, so the server sends the target and the client draws the colour from its own
 * random: the server world random that generates the research puzzles stays untouched, exactly
 * as the original left it.</p>
 */
public final class ScanRunes {
    /** Oracle ItemThaumometer:103,128 - the duration of the rune of a block or of a node. */
    public static final int BLOCK_DURATION = 15;
    /** Oracle ItemThaumometer:92,103,128 - the gravity of every scan rune. */
    public static final float GRAVITY = 0.03F;
    /** Oracle ItemThaumometer:103 - the rune of a block sits a quarter of a block above it. */
    public static final double BLOCK_HEIGHT = 0.25D;
    /** Oracle ItemThaumometer:92 - the duration of the rune of an entity is fifteen per block of height. */
    public static final int HEIGHT_DURATION = 15;
    /** Oracle ItemThaumometer:92 - the rune of an entity is centred on its body. */
    public static final double ENTITY_OFFSET = 0.5D;

    private ScanRunes() {}

    /** One rune of the original scan. */
    public record Rune(double x, double y, double z, int duration, float gravity) {
        /** The particle of the original ClientProxy.blockRunes:919 sits half a block above the point. */
        public double particleX() { return x + 0.5D; }
        public double particleY() { return y + 0.5D; }
        public double particleZ() { return z + 0.5D; }
    }

    /** Oracle ItemThaumometer:103,128 - the rune over a block or over a node. */
    public static Rune block(int x, int y, int z) {
        return new Rune(x, y + BLOCK_HEIGHT, z, BLOCK_DURATION, GRAVITY);
    }

    /**
     * Oracle ItemThaumometer:92 - the rune over the pointed entity.
     *
     * <p>The original reads the height field {@code field_70131_O} of the entity for the duration
     * and its eye height {@code func_70047_e()} for the position: the rune sits half of the eye
     * height above the feet of the entity.</p>
     */
    public static Rune entity(double x, double y, double z, double eyeHeight, double height) {
        return new Rune(x - ENTITY_OFFSET, y + eyeHeight / 2.0D, z - ENTITY_OFFSET,
                (int) (height * HEIGHT_DURATION), GRAVITY);
    }

    /**
     * Oracle ItemThaumometer:92,103,128 - the colour of the rune:
     * {@code 0.3 + rand * 0.7}, {@code 0.0} and {@code 0.3 + rand * 0.7}.
     *
     * <p>The original drew both values from the client world random; the caller of the native
     * port passes the client random, so the server world random is never shifted by a scan.</p>
     */
    public static float[] colour(RandomSource random) {
        return new float[] {0.3F + random.nextFloat() * 0.7F, 0.0F, 0.3F + random.nextFloat() * 0.7F};
    }
}
