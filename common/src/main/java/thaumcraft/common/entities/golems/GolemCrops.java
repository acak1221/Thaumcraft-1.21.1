package thaumcraft.common.entities.golems;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CactusBlock;
import net.minecraft.world.level.block.CarrotBlock;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.PotatoBlock;
import net.minecraft.world.level.block.PumpkinBlock;
import net.minecraft.world.level.block.SugarCaneBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Finite ordinary Harvest crops, shared by selection and the root permission bridge. */
final class GolemCrops {
    private GolemCrops() {}

    static boolean ripe(ServerLevel level, BlockPos pos, BlockState state) {
        if (level == null || pos == null || state == null || level.isOutsideBuildHeight(pos)
                || !level.getWorldBorder().isWithinBounds(pos) || !level.hasChunkAt(pos)
                || level.getBlockState(pos) != state) return false;
        Block block = state.getBlock();
        Class<?> type = block.getClass();
        if (block == Blocks.WHEAT && type == CropBlock.class || block == Blocks.CARROTS && type == CarrotBlock.class
                || block == Blocks.POTATOES && type == PotatoBlock.class) return ((CropBlock) block).isMaxAge(state);
        if (block == Blocks.NETHER_WART && type == NetherWartBlock.class) return state.getValue(NetherWartBlock.AGE) == 3;
        if (block == Blocks.COCOA && type == CocoaBlock.class) return state.getValue(CocoaBlock.AGE) == 2;
        if (block == Blocks.MELON && type == Block.class || block == Blocks.PUMPKIN && type == PumpkinBlock.class) return true;
        if (block != Blocks.SUGAR_CANE || type != SugarCaneBlock.class) {
            if (block != Blocks.CACTUS || type != CactusBlock.class) return false;
        }
        // Every non-bottom stacked segment qualifies, not merely the topmost.
        BlockPos below = pos.below();
        return !level.isOutsideBuildHeight(below) && level.getWorldBorder().isWithinBounds(below)
                && level.hasChunkAt(below) && level.getBlockState(below).getBlock() == block;
    }
}
