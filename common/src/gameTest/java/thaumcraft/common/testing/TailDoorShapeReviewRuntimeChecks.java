package thaumcraft.common.testing;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.common.blocks.BlockArcaneDoor;
import thaumcraft.common.config.ThaumcraftEldritchBlocks;

/** Both installed halves use the original combined door metadata for their plate. */
public final class TailDoorShapeReviewRuntimeChecks {
    private TailDoorShapeReviewRuntimeChecks() {}

    public static void bothDoorHalvesKeepCanonicalOpenAndClosedPlanes(GameTestHelper test) {
        var level = test.getLevel();
        BlockPos lowerPos = test.absolutePos(new BlockPos(2, 2, 2)).atY(230);
        BlockPos upperPos = lowerPos.above();
        Map<BlockPos, BlockState> saved = new LinkedHashMap<>();
        List<String> failures = new ArrayList<>();
        for (BlockPos pos : new BlockPos[]{lowerPos, upperPos}) {
            test.assertTrue(level.hasChunkAt(pos) && level.getBlockEntity(pos) == null, "door shape fixture replaces no foreign block entity");
            saved.put(pos, level.getBlockState(pos));
        }
        try {
            var door = ThaumcraftEldritchBlocks.ARCANE_DOOR.get();
            // Original orientations0..3 have the closed plate at minimumX,
            // minimumZ, maximumX, maximumZ. Opening rotates one quarter turn,
            // reversed by the original hinge bit; upper bit8 does not enter it.
            Direction[] originalFacingOrder = {Direction.EAST, Direction.SOUTH, Direction.WEST, Direction.NORTH};
            VoxelShape[] originalPlanes = {
                    Block.box(0, 0, 0, 3, 16, 16), Block.box(0, 0, 0, 16, 16, 3),
                    Block.box(13, 0, 0, 16, 16, 16), Block.box(0, 0, 13, 16, 16, 16)
            };
            for (int orientation = 0; orientation < originalFacingOrder.length; orientation++) {
                Direction facing = originalFacingOrder[orientation];
                for (boolean flipped : new boolean[]{false, true}) for (boolean open : new boolean[]{false, true}) {
                    BlockState lower = door.defaultBlockState().setValue(BlockArcaneDoor.FACING, facing)
                            .setValue(BlockArcaneDoor.FLIP, flipped).setValue(BlockArcaneDoor.OPEN, open)
                            .setValue(BlockArcaneDoor.HALF, Half.BOTTOM);
                    BlockState upper = lower.setValue(BlockArcaneDoor.HALF, Half.TOP);
                    int flags = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
                    level.setBlock(lowerPos, lower, flags);
                    level.setBlock(upperPos, upper, flags);
                    test.assertTrue(level.getBlockState(lowerPos) == lower && level.getBlockState(upperPos) == upper,
                            "the actual registered pair retains its tested state");
                    VoxelShape lowerSelection = lower.getShape(level, lowerPos, CollisionContext.empty());
                    VoxelShape upperSelection = upper.getShape(level, upperPos, CollisionContext.empty());
                    VoxelShape lowerCollision = lower.getCollisionShape(level, lowerPos, CollisionContext.empty());
                    VoxelShape upperCollision = upper.getCollisionShape(level, upperPos, CollisionContext.empty());
                    VoxelShape expected = originalPlanes[Math.floorMod(orientation + (open ? flipped ? -1 : 1 : 0), 4)];
                    String name = facing + "/flip=" + flipped + "/open=" + open;
                    if (different(lowerSelection, upperSelection) || different(lowerCollision, upperCollision))
                        failures.add(name + " halves differ: lower=" + lowerSelection.bounds() + ", upper=" + upperSelection.bounds());
                    if (different(lowerSelection, expected) || different(lowerCollision, expected)
                            || different(upperSelection, expected) || different(upperCollision, expected))
                        failures.add(name + " canonical=" + expected.bounds() + ", lower selection/collision="
                                + lowerSelection.bounds() + "/" + lowerCollision.bounds() + ", upper selection/collision="
                                + upperSelection.bounds() + "/" + upperCollision.bounds());
                }
            }
        } finally {
            saved.forEach((pos, state) -> level.setBlock(pos, state, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE));
        }
        test.assertTrue(failures.isEmpty(), "registered arcane door shape/collision planes: " + failures);
        test.succeed();
    }

    private static boolean different(VoxelShape first, VoxelShape second) {
        return Shapes.joinIsNotEmpty(first, second, BooleanOp.NOT_SAME);
    }
}
