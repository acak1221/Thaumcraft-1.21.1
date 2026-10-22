package thaumcraft.tiles;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import thaumcraft.common.tiles.TileJarNode;

/**
 * The client event nine of the original node jar.
 *
 * <p>Oracle {@code thaumcraft/common/tiles/TileJarNode.java:204-219}: the client answers the block
 * event nine with a burst of block sparkles over the whole jar, thirty six of them with five particles
 * each and the special random grey colour of the original, and it starts the one second animation
 * timer.</p>
 */
class JarNodeClientEventParityTest {
    @Test void theEventIdIsTheOriginalOne() {
        assertEquals(9, TileJarNode.ANIMATE_EVENT);
    }

    @Test void theSparkleCountIsTheOriginalOne() {
        // Oracle TileJarNode.java:210: blockSparkle(world, x, y, z, -9999, 5).
        assertEquals(5, TileJarNode.SPARKLE_COUNT);
    }

    @Test void theAnimationLastsOneSecond() {
        // Oracle TileJarNode.java:214: animate = System.currentTimeMillis() + 1000L.
        assertEquals(1000L, TileJarNode.ANIMATE_MILLIS);
    }

    @Test void theBurstCoversThirtySixBlocks() {
        // Oracle TileJarNode.java:206-213: yy from -1 to 2, xx from -1 to 1, zz from -1 to 1.
        assertEquals(3, TileJarNode.SPARKLE_X);
        assertEquals(4, TileJarNode.SPARKLE_Y);
        assertEquals(3, TileJarNode.SPARKLE_Z);
        assertEquals(36, TileJarNode.sparkleCount());
    }
}
