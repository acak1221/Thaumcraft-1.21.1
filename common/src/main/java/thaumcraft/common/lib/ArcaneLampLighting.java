package thaumcraft.common.lib;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import thaumcraft.common.config.ThaumcraftLamps;
import thaumcraft.common.tiles.TileArcaneBore;
import thaumcraft.common.tiles.TileArcaneBoreBase;
import thaumcraft.common.tiles.TileArcaneLamp;

/** Original TileArcaneBore's post-success tunnel light; no effect belongs to a refused or unwitnessed removal. */
public final class ArcaneLampLighting {
    private ArcaneLampLighting() {}
    public static BlockPos boreCandidate(BlockPos top, Direction direction, int draw) {
        if (draw < 0 || draw >= 32) throw new IllegalArgumentException("Expected original nextInt(32) result");
        int distance = draw * 2, pattern = draw % 4;
        BlockPos target = top.relative(direction, 1 + distance);
        int side = pattern == 0 ? 3 : pattern == 2 ? -3 : 0;
        target = direction.getStepX() != 0 ? target.offset(0, 0, side) : target.offset(side, 0, 0);
        return pattern == 3 && direction.getStepY() == 0 ? target.below(2) : target;
    }
    public static boolean afterBoreDig(TileArcaneBore top, TileArcaneBoreBase base) {
        if (!(top.getLevel() instanceof ServerLevel level) || !level.getServer().isSameThread() || !top.isCurrent()
                || base == null || base.getLevel() != level || !base.isCurrent()) return false;
        for (Direction face : new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST}) {
            BlockPos at = base.getBlockPos().relative(face);
            if (!level.hasChunkAt(at) || !(level.getBlockEntity(at) instanceof TileArcaneLamp)) continue;
            BlockPos target = boreCandidate(top.getBlockPos(), top.miningDirection(), level.random.nextInt(32));
            if (level.isOutsideBuildHeight(target) || !level.getWorldBorder().isWithinBounds(target) || !level.hasChunkAt(target)) return false;
            var state = level.getBlockState(target);
            if (!state.isAir() || state.is(ThaumcraftLamps.ARCANE_LIGHT.get()) || level.getRawBrightness(target, 0) >= 15) return false;
            return level.setBlock(target, ThaumcraftLamps.ARCANE_LIGHT.get().defaultBlockState(), 3);
        }
        return false;
    }
}
