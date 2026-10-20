package thaumcraft.common.lib.world;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

/** The original six offset draws and source-ignoring ray, bounded by already loaded native chunks. */
public final class HungryNodeTrace {
    private HungryNodeTrace() {}
    public static BlockHitResult randomHit(Level world, BlockPos source, RandomSource random) {
        int x = source.getX() + random.nextInt(16) - random.nextInt(16);
        int y = source.getY() + random.nextInt(16) - random.nextInt(16);
        int z = source.getZ() + random.nextInt(16) - random.nextInt(16);
        BlockPos column = new BlockPos(x, source.getY(), z);
        if (!world.hasChunkAt(column)) return null;
        y = Math.min(y, world.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z));
        return hit(world, source, new BlockPos(x, y, z));
    }
    public static BlockHitResult hit(Level world, BlockPos source, BlockPos target) {
        if (!world.hasChunkAt(source)) return null;
        // OUTLINE matches original selected block ray boxes; SOURCE_ONLY keeps
        // original stopOnLiquid=true source-fluid interception. Skip the source
        // without removing/changing its real block or asking for missing chunks.
        BlockGetter loaded = new BlockGetter() {
            @Override public int getHeight() { return world.getHeight(); }
            @Override public int getMinBuildHeight() { return world.getMinBuildHeight(); }
            @Override public BlockEntity getBlockEntity(BlockPos pos) { return pos.equals(source) || !world.hasChunkAt(pos) ? null : world.getBlockEntity(pos); }
            @Override public BlockState getBlockState(BlockPos pos) {
                return pos.equals(source) ? Blocks.AIR.defaultBlockState()
                        : world.isOutsideBuildHeight(pos) || !world.hasChunkAt(pos) ? Blocks.BEDROCK.defaultBlockState() : world.getBlockState(pos);
            }
            @Override public FluidState getFluidState(BlockPos pos) { return getBlockState(pos).getFluidState(); }
        };
        BlockHitResult result = loaded.clip(new ClipContext(Vec3.atCenterOf(source), Vec3.atCenterOf(target),
                ClipContext.Block.OUTLINE, ClipContext.Fluid.SOURCE_ONLY, CollisionContext.empty()));
        return result.getType() == HitResult.Type.BLOCK && !result.getBlockPos().equals(source)
                && world.hasChunkAt(result.getBlockPos()) && !world.isOutsideBuildHeight(result.getBlockPos()) ? result : null;
    }
}
