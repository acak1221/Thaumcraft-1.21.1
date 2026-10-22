package thaumcraft.common.lib.utils;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * The picking geometry of the original TC4 4.2.3.5 {@code thaumcraft.common.lib.utils.EntityUtils}.
 *
 * <p>Every expectation is the value of the original oracle file
 * {@code OpenTC4/src/main/java/thaumcraft/common/lib/utils/EntityUtils.java} and of the 1.7.10
 * vanilla helpers it calls.</p>
 *
 * <p>These tests are AUTHORED BUT NOT EXECUTED: the wave that produced them may not run Gradle
 * (the shared Loom cache is owned by the leader).</p>
 */
class EntityUtilsParityTest {

    /** The native registries must exist before the class of the helper is loaded. */
    @BeforeAll static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    // --------------------------------------------------- the collision border of 1.7.10

    /**
     * Original {@code EntityUtils.getPointedEntity}: {@code Math.max(0.8F, entity.getCollisionBorderSize())}.
     *
     * <p>1.7.10 {@code Entity.getCollisionBorderSize()} is {@code (float)this.width * 0.5F}
     * (oracle {@code EntityUtils.java:79}), so an ordinary 0.6 wide mob answers 0.3 and the
     * original's own floor of 0.8 wins.</p>
     */
    @Test void thePickPaddingIsTheOriginalCollisionBorder() {
        // Oracle EntityUtils:79 - Math.max(0.8F, entity.getCollisionBorderSize()) and the border is
        // (float)this.width * 0.5F.
        assertEquals(0.8F, EntityUtils.pickPadding(0.6f), "an ordinary mob keeps the 0.8 floor");
        assertEquals(0.8F, EntityUtils.pickPadding(0.0f));
        assertEquals(0.8F, EntityUtils.pickPadding(1.6f), "a 1.6 wide mob has a border of exactly 0.8");
        assertEquals(8.0F, EntityUtils.pickPadding(16.0f), "a 16 wide entity has a border of 8");
    }

    // ------------------------------------------------------------- the minimum range

    /**
     * Original {@code EntityUtils.getPointedEntity}: {@code !((double) entity.getDistanceToEntity(entityplayer) < minrange)}.
     *
     * <p>{@code getDistanceToEntity} is the LINEAR distance, so the candidate is refused when
     * {@code distance < minrange}. Comparing the SQUARED distance with the range instead would
     * refuse every candidate between the square root of the range and the range itself: with a
     * range of four blocks a candidate three blocks away must be refused, and the squared
     * comparison {@code 9 < 4} would have kept it.</p>
     */
    @Test void theMinimumRangeUsesTheLinearDistance() {
        assertTrue(EntityUtils.belowMinRange(9.0, 4.0), "three blocks are below a four block minimum");
        assertFalse(EntityUtils.belowMinRange(16.0, 4.0), "four blocks are not below a four block minimum");
        assertFalse(EntityUtils.belowMinRange(25.0, 4.0));
        assertTrue(EntityUtils.belowMinRange(0.16, 0.5), "four tenths of a block are below a half block minimum");
        assertFalse(EntityUtils.belowMinRange(0.25, 0.5), "half a block is not below a half block minimum");
    }

    // ------------------------------------------------------------- the visible points

    /**
     * Original {@code EntityUtils.isVisibleTo}: the target point is
     * {@code ent2.boundingBox.minY + (double)(ent2.height / 2.0F)} (oracle {@code EntityUtils.java:199}).
     *
     * <p>The anchor is the BOTTOM of the box. The top of the box would move the target a whole
     * entity height too high: for a 1.8 high player the original answers 0.9 above the feet and
     * the wrong anchor answers 2.7.</p>
     */
    @Test void theVisibleTargetIsTheMidBodyOfTheBox() {
        double[] point = EntityUtils.midBody(1.5, 64.0, 1.8, -2.5);
        assertArrayEquals(new double[]{1.5, 64.9, -2.5}, point, 1.0E-9);
        assertEquals(64.9, point[1], 1.0E-9);
    }

    /**
     * Original {@code EntityUtils.isVisibleTo}: the apex is
     * {@code ent.boundingBox.minY + (double)ent.getEyeHeight()} (oracle {@code EntityUtils.java:200}).
     */
    @Test void theVisibleApexIsTheEyeHeightAboveTheFeet() {
        double[] point = EntityUtils.eyePoint(1.5, 64.0, 1.62, -2.5);
        assertArrayEquals(new double[]{1.5, 65.62, -2.5}, point, 1.0E-9);
        assertEquals(65.62, point[1], 1.0E-9);
    }
}
