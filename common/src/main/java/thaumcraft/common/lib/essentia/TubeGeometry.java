package thaumcraft.common.lib.essentia;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.api.aspects.IEssentiaTransport;
import thaumcraft.common.tiles.TileTube;
import thaumcraft.common.tiles.TileTubeBuffer;

/** Original TC4 shapes, implemented with native AABBs (no CodeChicken/donor classes). */
public final class TubeGeometry {
    private static final AABB CORE = new AABB(.34375, .34375, .34375, .65625, .65625, .65625);
    private static final AABB BUFFER_CORE = new AABB(.25, .25, .25, .75, .75, .75);
    private static final AABB[] ARMS = {
            new AABB(.42F, 0, .42F, .58F, .5, .58F),
            new AABB(.42F, .5, .42F, .58F, 1, .58F),
            new AABB(.42F, .42F, 0, .58F, .58F, .5),
            new AABB(.42F, .42F, .5, .58F, .58F, 1),
            new AABB(0, .42F, .42F, .5, .58F, .58F),
            new AABB(.5, .42F, .42F, 1, .58F, .58F)
    };

    private TubeGeometry() {}

    public record IndexedHit(int index, BlockHitResult hit) {}

    /** Raw neighbor presence: closed arms AND a valve's excluded stem remain indexed. */
    public static boolean hasTransportNeighbor(BlockGetter level, BlockPos pos, Direction face) {
        if (face == null || level instanceof Level world && !world.hasChunkAt(pos.relative(face))) return false;
        BlockEntity tile = level.getBlockEntity(pos.relative(face));
        return tile instanceof IEssentiaTransport && !tile.isRemoved()
                && (!(level instanceof Level world) || tile.getLevel() == world
                        && world.getBlockState(tile.getBlockPos()) == tile.getBlockState());
    }

    /** TileTube:372-393: direction ids 0..5, then core6, with strict nearest-distance ties. */
    public static IndexedHit trace(BlockGetter level, BlockPos pos, Vec3 start, Vec3 end) {
        IndexedHit nearest = null;
        double distance = Double.POSITIVE_INFINITY;
        for (int index = 0; index <= 6; index++) {
            if (index < 6 && !hasTransportNeighbor(level, pos, Direction.from3DDataValue(index))) continue;
            BlockHitResult hit = AABB.clip(List.of(index == 6 ? core(level, pos) : ARMS[index]), start, end, pos);
            if (hit == null) continue;
            double candidate = start.distanceToSqr(hit.getLocation());
            if (candidate < distance) {
                nearest = new IndexedHit(index, hit);
                distance = candidate;
            }
        }
        return nearest;
    }

    /** The server computes its own hit at the player's actual native reach, never client subHit/reach+1. */
    public static IndexedHit retrace(Player player, TileTube tube) {
        return retrace(player, (BlockEntity) tube);
    }

    public static IndexedHit retrace(Player player, TileTubeBuffer buffer) {
        return retrace(player, (BlockEntity) buffer);
    }

    private static IndexedHit retrace(Player player, BlockEntity tube) {
        Level level = player.level();
        BlockPos pos = tube.getBlockPos();
        if (tube.getLevel() != level || !level.hasChunkAt(pos) || level.getBlockEntity(pos) != tube || tube.isRemoved()
                || level.getBlockState(pos) != tube.getBlockState()) return null;
        Vec3 start = player.getEyePosition(1), end = start.add(player.getViewVector(1).scale(player.blockInteractionRange()));
        BlockHitResult first = level.clip(new ClipContext(start, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        if (first.getType() != HitResult.Type.BLOCK || !first.getBlockPos().equals(pos)) return null;
        return trace(level, pos, start, end);
    }

    /** Ordinary hand activation uses the block's native outline, NOT the much
     * narrower indexed tool parts. Keep reach/occlusion without adding a new
     * requirement to hit an arm/core when the original whole block was usable. */
    public static boolean hitsBlock(Player player, TileTube tube) {
        Level level = player.level();
        BlockPos pos = tube.getBlockPos();
        if (tube.getLevel() != level || !level.hasChunkAt(pos) || level.getBlockEntity(pos) != tube || tube.isRemoved()
                || level.getBlockState(pos) != tube.getBlockState()) return false;
        Vec3 start = player.getEyePosition(1), end = start.add(player.getViewVector(1).scale(player.blockInteractionRange()));
        BlockHitResult first = level.clip(new ClipContext(start, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        return first.getType() == HitResult.Type.BLOCK && first.getBlockPos().equals(pos);
    }

    /** Wand/resonator tools use this union. It is NOT the collision shape. */
    public static VoxelShape indexedShape(BlockGetter level, BlockPos pos) {
        VoxelShape shape = Shapes.create(core(level, pos));
        for (Direction face : Direction.values()) {
            if (hasTransportNeighbor(level, pos, face)) shape = Shapes.or(shape, Shapes.create(ARMS[face.get3DDataValue()]));
        }
        return shape;
    }

    private static AABB core(BlockGetter level, BlockPos pos) { return level.getBlockEntity(pos) instanceof TileTubeBuffer ? BUFFER_CORE : CORE; }

    /** Original ordinary buffer outline is compact; collision still uses the
     * separately audited thin expanded ONE box shared by original metadata4. */
    public static VoxelShape bufferSelectionShape() { return Shapes.create(BUFFER_CORE); }

    /** BlockTube:136-179: ordinary selection starts X6..10, Y/Z5..11 sixteenths. */
    public static VoxelShape selectionShape(BlockGetter level, BlockPos pos) {
        return expandedBox(level, pos, 5.0 / 16, 11.0 / 16);
    }

    /** BlockTube:199-240: ONE expanded bounding box, never an arm union. */
    public static VoxelShape collisionShape(BlockGetter level, BlockPos pos) {
        return expandedBox(level, pos, 6.0 / 16, 10.0 / 16);
    }

    private static VoxelShape expandedBox(BlockGetter level, BlockPos pos, double minYZ, double maxYZ) {
        double minX = 6.0 / 16, maxX = 10.0 / 16, minY = minYZ, maxY = maxYZ, minZ = minYZ, maxZ = maxYZ;
        for (Direction face : Direction.values()) {
            if (!neighborConnects(level, pos, face)) continue;
            switch (face) {
                case DOWN -> minY = 0;
                case UP -> maxY = 1;
                case NORTH -> minZ = 0;
                case SOUTH -> maxZ = 1;
                case WEST -> minX = 0;
                case EAST -> maxX = 1;
            }
        }
        return Shapes.create(new AABB(minX, minY, minZ, maxX, maxY, maxZ));
    }

    private static boolean neighborConnects(BlockGetter level, BlockPos pos, Direction face) {
        if (level instanceof Level world) {
            if (!world.hasChunkAt(pos)) return false;
            BlockEntity origin = world.getBlockEntity(pos);
            return origin != null && EssentiaTransfers.connectableNeighbor(origin, face) != null;
        }
        BlockEntity neighbor = level.getBlockEntity(pos.relative(face));
        return neighbor instanceof IEssentiaTransport transport && !neighbor.isRemoved() && transport.isConnectable(face.getOpposite());
    }
}
