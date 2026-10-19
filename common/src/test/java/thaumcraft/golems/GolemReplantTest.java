package thaumcraft.golems;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import java.util.List;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumcraft.common.entities.golems.GolemReplant;

/** Pure rules of the Order replanting (original AIHarvestCrops:185-245): cells, seed family, drop push. Items and blocks only; no world. */
class GolemReplantTest {
    @BeforeAll static void bootstrapNativeRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test void theNineCellsAreTheHarvestedColumnFirstThenItsEightNeighboursInTheOriginalOrder() {
        BlockPos at = new BlockPos(10, 64, -3);
        // Original xm = {0,0,1,1,-1,0,-1,-1,1}, zm = {0,1,0,1,0,-1,-1,1,-1}, always one block BELOW the crop.
        List<BlockPos> expected = List.of(new BlockPos(10, 63, -3), new BlockPos(10, 63, -2), new BlockPos(11, 63, -3),
                new BlockPos(11, 63, -2), new BlockPos(9, 63, -3), new BlockPos(10, 63, -4), new BlockPos(9, 63, -4),
                new BlockPos(9, 63, -2), new BlockPos(11, 63, -4));
        assertEquals(9, GolemReplant.CELLS);
        for (int index = 0; index < GolemReplant.CELLS; index++) assertEquals(expected.get(index), GolemReplant.ground(at, index), "cell " + index);
        assertEquals(9, new HashSet<>(expected).size(), "nine distinct cells: the full 3x3 below the column");
    }

    @Test void theSeedFamilyIsTheVanillaPlantablesAndNothingElse() {
        assertSame(Blocks.WHEAT, GolemReplant.seedBlock(new ItemStack(Items.WHEAT_SEEDS)));
        assertSame(Blocks.BEETROOTS, GolemReplant.seedBlock(new ItemStack(Items.BEETROOT_SEEDS)));
        assertSame(Blocks.MELON_STEM, GolemReplant.seedBlock(new ItemStack(Items.MELON_SEEDS)));
        assertSame(Blocks.PUMPKIN_STEM, GolemReplant.seedBlock(new ItemStack(Items.PUMPKIN_SEEDS)));
        assertSame(Blocks.CARROTS, GolemReplant.seedBlock(new ItemStack(Items.CARROT)));
        assertSame(Blocks.POTATOES, GolemReplant.seedBlock(new ItemStack(Items.POTATO)));
        assertSame(Blocks.NETHER_WART, GolemReplant.seedBlock(new ItemStack(Items.NETHER_WART)));
        for (var other : List.of(Items.COCOA_BEANS, Items.WHEAT, Items.SUGAR_CANE, Items.CACTUS, Items.MELON_SLICE, Items.PUMPKIN,
                Items.TORCHFLOWER_SEEDS, Items.BONE_MEAL, Items.APPLE, Items.DIRT)) {
            assertNull(GolemReplant.seedBlock(new ItemStack(other)), other.toString());
        }
        assertNull(GolemReplant.seedBlock(ItemStack.EMPTY));
        assertNull(GolemReplant.seedBlock(null));
    }

    @Test void aFreshDropIsPushedTowardTheGolemWithAFixedLift() {
        Vec3 golem = new Vec3(100, 64, 100);
        // Drop 3 east and 4 south on the same level: unit vector (0.6, 0, 0.8) -> (-0.15, 0.075, -0.2).
        Vec3 push = GolemReplant.nudge(golem, golem.add(3, 0, 4));
        assertEquals(-0.15, push.x, 1e-12);
        assertEquals(0.075, push.y, 0.0);
        assertEquals(-0.2, push.z, 1e-12);
        Vec3 behind = GolemReplant.nudge(golem, golem.add(-5, 0, 0));
        assertEquals(0.25, behind.x, 1e-12, "a drop west of the golem is pushed east, toward it");
        assertEquals(0.0, behind.z, 1e-12);
    }

    @Test void theDirectionIsNormalisedInThreeDimensionsLikeTheOriginal() {
        // 5 north and 12 up: the 3D length is 13, so the horizontal share is 5/13, not 1.
        Vec3 push = GolemReplant.nudge(Vec3.ZERO, new Vec3(0, 12, -5));
        assertEquals(5.0 / 13.0 / 4.0, push.z, 1e-12);
        assertEquals(0.0, push.x, 1e-12);
        assertEquals(0.075, push.y, 0.0, "the vertical push does not depend on the offset");
    }

    @Test void aDropExactlyOnTheGolemIsNeitherPushedSidewaysNorProducesNaN() {
        Vec3 push = GolemReplant.nudge(new Vec3(1, 2, 3), new Vec3(1, 2, 3));
        assertEquals(0.0, push.x, 1e-12);
        assertEquals(0.0, push.z, 1e-12);
        assertEquals(0.075, push.y, 0.0);
        assertFalse(Double.isNaN(push.x) || Double.isNaN(push.z));
    }

    @Test void theSearchBoxAndFreshnessMatchTheOriginalConstants() {
        assertEquals(6.0, GolemReplant.RANGE, 0.0);
        assertEquals(2, GolemReplant.FRESH_TICKS);
    }
}
