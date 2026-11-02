package thaumcraft.common.testing;

import java.util.ArrayList;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.common.blocks.BlockArcaneFurnace;
import thaumcraft.common.blocks.BlockArcaneFurnaceNozzle;
import thaumcraft.common.blocks.BlockWandPedestal;
import thaumcraft.common.config.ThaumcraftLegacyTiles;

/** Actual registered state shapes, against normalized bounds in the pinned original. */
public final class TailBlockShapeRuntimeChecks {
    private TailBlockShapeRuntimeChecks() {}

    public static void furnaceCoreNozzlesAndPedestalKeepCanonicalBounds(GameTestHelper test) {
        try (var f = new TubeVariantsRuntimeChecks.Fixture(test)) {
            f.open();
            var failures = new ArrayList<String>();
            check(f, ThaumcraftLegacyTiles.ARCANE_FURNACE.get().defaultBlockState()
                    .setValue(BlockArcaneFurnace.VARIANT, 0), Block.box(0, 0, 0, 16, 4, 16), "core", failures);
            for (Direction facing : Direction.Plane.HORIZONTAL) {
                VoxelShape expected = switch (facing) {
                    case WEST -> Block.box(0, 0, 0, 8, 16, 16);
                    case EAST -> Block.box(8, 0, 0, 16, 16, 16);
                    case NORTH -> Block.box(0, 0, 0, 16, 16, 8);
                    default -> Block.box(0, 0, 8, 16, 16, 16);
                };
                check(f, ThaumcraftLegacyTiles.ARCANE_FURNACE_NOZZLE.get().defaultBlockState()
                        .setValue(BlockArcaneFurnaceNozzle.FACING, facing), expected, "nozzle " + facing, failures);
            }
            check(f, ThaumcraftLegacyTiles.WAND_PEDESTAL.get().defaultBlockState(), Shapes.or(
                    Block.box(0, 0, 0, 16, 4, 16), Block.box(2, 4, 2, 14, 8, 14),
                    Block.box(4, 8, 4, 12, 16, 12)), "pedestal", failures);
            check(f, ThaumcraftLegacyTiles.WAND_PEDESTAL.get().defaultBlockState()
                    .setValue(BlockWandPedestal.FOCUS, true), Shapes.block(), "focus control", failures);
            test.assertTrue(failures.isEmpty(), "registered selection and collision shapes preserve original extents: " + failures);
        }
        test.succeed();
    }

    private static void check(TubeVariantsRuntimeChecks.Fixture f, BlockState state, VoxelShape expected,
                              String name, ArrayList<String> failures) {
        f.level.removeBlockEntity(f.base);
        f.level.setBlock(f.base, state, TubeVariantsRuntimeChecks.QUIET);
        VoxelShape selection = state.getShape(f.level, f.base);
        VoxelShape collision = state.getCollisionShape(f.level, f.base, CollisionContext.empty());
        if (Shapes.joinIsNotEmpty(selection, expected, BooleanOp.NOT_SAME)
                || Shapes.joinIsNotEmpty(collision, expected, BooleanOp.NOT_SAME)) {
            failures.add(name + " selection=" + selection.bounds() + ", collision=" + collision.bounds()
                    + ", expected=" + expected.bounds());
        }
    }
}
