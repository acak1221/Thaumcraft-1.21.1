package thaumic.tinkerer.tileblocks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.SharedConstants;
import net.minecraft.core.Direction;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumic.tinkerer.common.block.camo.CamoRules;

/**
 * Test-only code of the port (no 1.7.10 counterpart), CC BY-NC-SA 3.0 like the add-on.
 *
 * <p>What a camouflageable device may imitate and what a right click does: the empty hand clears, a block item sets its
 * default state, other items do nothing, blocks without a plain model are refused.</p>
 */
class CamoRulesTest {
    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void theEmptyHandClearsTheCamouflage() {
        CamoRules.Change change = CamoRules.change(ItemStack.EMPTY, Direction.NORTH);
        assertEquals(CamoRules.Change.Kind.CLEAR, change.kind());
    }

    @Test
    void aBlockItemSetsItsDefaultState() {
        CamoRules.Change change = CamoRules.change(new ItemStack(Items.STONE), Direction.UP);
        assertEquals(CamoRules.Change.Kind.SET, change.kind());
        assertEquals(Blocks.STONE.defaultBlockState(), change.state());
    }

    @Test
    void anItemThatIsNoBlockLeavesTheCamouflageAlone() {
        assertEquals(CamoRules.Change.Kind.NONE, CamoRules.change(new ItemStack(Items.STICK), Direction.NORTH).kind());
        assertEquals(CamoRules.Change.Kind.NONE, CamoRules.change(new ItemStack(Items.WATER_BUCKET), Direction.NORTH).kind());
    }

    @Test
    void onlyBlocksDrawnFromAPlainModelAreValid() {
        assertTrue(CamoRules.isValidCamo(Blocks.STONE.defaultBlockState()));
        assertTrue(CamoRules.isValidCamo(Blocks.GLASS.defaultBlockState()));
        assertTrue(CamoRules.isValidCamo(Blocks.OAK_LOG.defaultBlockState()));
        assertFalse(CamoRules.isValidCamo(Blocks.AIR.defaultBlockState()), "air");
        assertFalse(CamoRules.isValidCamo(Blocks.WATER.defaultBlockState()), "a fluid has no model");
        assertFalse(CamoRules.isValidCamo(Blocks.CHEST.defaultBlockState()), "a block with a block entity has a special renderer");
        assertFalse(CamoRules.isValidCamo(Blocks.FURNACE.defaultBlockState()), "a block with a block entity");
        assertFalse(CamoRules.isValidCamo(Blocks.BARRIER.defaultBlockState()), "an invisible block");
    }

    @Test
    void aBlockItemWhoseBlockIsNotValidDoesNothing() {
        assertEquals(CamoRules.Change.Kind.NONE, CamoRules.change(new ItemStack(Items.CHEST), Direction.NORTH).kind());
        assertEquals(CamoRules.Change.Kind.NONE, CamoRules.change(new ItemStack(Items.BARRIER), Direction.NORTH).kind());
    }

    @Test
    void aDirectionalBlockFacesTheClickedSide() {
        for (Direction side : new Direction[] { Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST }) {
            BlockState state = CamoRules.change(new ItemStack(Items.CARVED_PUMPKIN), side).state();
            assertNotNull(state);
            assertEquals(side, state.getValue(BlockStateProperties.HORIZONTAL_FACING), "clicked side " + side);
        }
        // The top and the bottom keep the default, as the original left the metadata alone.
        BlockState up = CamoRules.change(new ItemStack(Items.CARVED_PUMPKIN), Direction.UP).state();
        assertEquals(Blocks.CARVED_PUMPKIN.defaultBlockState(), up);
    }
}
