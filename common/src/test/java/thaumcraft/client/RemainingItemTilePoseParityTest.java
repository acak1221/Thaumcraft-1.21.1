package thaumcraft.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;
import thaumcraft.client.renderers.item.ThaumometerArmPose;
import thaumcraft.client.renderers.item.BoneBowPose;
import thaumcraft.client.renderers.item.WandUsePose;
import thaumcraft.client.renderers.tile.ThaumatoriumPose;

/** Pure original-value rules; never initializes a block, world or renderer. */
class RemainingItemTilePoseParityTest {
    @Test void boneBowUsesTheOriginalOuterAndInnerScalesAndTurns() {
        // Oracle ItemBowBoneRenderer:50-64,84.
        assertEquals(2.6666667F, BoneBowPose.OUTER_SCALE);
        assertEquals(.625F, BoneBowPose.INNER_SCALE);
        assertEquals(1.5F, BoneBowPose.ICON_SCALE);
        assertEquals(-20, BoneBowPose.OUTER_ROLL);
        assertEquals(90, BoneBowPose.OUTER_PITCH);
        assertEquals(-60, BoneBowPose.SECOND_ROLL);
    }
    @Test void scannerInterpolatesArmThenTakesOneTenthOfViewDifference() {
        // Oracle ItemThaumometerRenderer:101-104, not interpolated player view.
        assertEquals(2, ThaumometerArmPose.lagDegrees(40, 10, 30, .5F));
        assertEquals(-4, ThaumometerArmPose.lagDegrees(-20, 10, 30, .5F));
        assertEquals(0, ThaumometerArmPose.lagDegrees(30, 30, 30, .75F));
    }

    @Test void originalThaumatoriumObjAndPreviewOrientations() {
        // Oracle TileThaumatoriumRenderer:53-73 and :81-99.
        assertEquals(-90, ThaumatoriumPose.MODEL_PITCH);
        assertEquals(270, ThaumatoriumPose.modelRoll(Direction.NORTH));
        assertEquals(90, ThaumatoriumPose.modelRoll(Direction.SOUTH));
        assertEquals(180, ThaumatoriumPose.modelRoll(Direction.EAST));
        assertEquals(0, ThaumatoriumPose.modelRoll(Direction.WEST));
        assertEquals(180, ThaumatoriumPose.previewYaw(Direction.NORTH));
        assertEquals(0, ThaumatoriumPose.previewYaw(Direction.SOUTH));
        assertEquals(90, ThaumatoriumPose.previewYaw(Direction.EAST));
        assertEquals(270, ThaumatoriumPose.previewYaw(Direction.WEST));
        assertEquals(1.325F, ThaumatoriumPose.PREVIEW_Y);
        assertEquals(.75F, ThaumatoriumPose.PREVIEW_SCALE);
    }

    @Test void previewCyclesEveryFortyPlayerTicksWithoutPartialInterpolation() {
        // Oracle TileThaumatoriumRenderer:78-79.
        assertEquals(-1, ThaumatoriumPose.previewIndex(80, 0));
        assertEquals(0, ThaumatoriumPose.previewIndex(39, 3));
        assertEquals(1, ThaumatoriumPose.previewIndex(40, 3));
        assertEquals(2, ThaumatoriumPose.previewIndex(119, 3));
        assertEquals(0, ThaumatoriumPose.previewIndex(120, 3));
    }

    @Test void wandUsePoseRaisesSixtyDegreesOverThreeTicksAboutOneBlockPivot() {
        // Oracle ItemWandRenderer:89-100,112.
        assertEquals(1, WandUsePose.PIVOT_Y);
        assertEquals(33, WandUsePose.THIRD_PERSON_ROLL);
        assertEquals(10, WandUsePose.FIRST_PERSON_PITCH);
        assertEquals(10, WandUsePose.FIRST_PERSON_ROLL);
        assertEquals(30, WandUsePose.raiseDegrees(WandUsePose.useTicks(1, .5F)));
        assertEquals(60, WandUsePose.raiseDegrees(WandUsePose.useTicks(30, .5F)));
    }
}
