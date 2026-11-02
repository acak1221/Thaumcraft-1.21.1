package thaumcraft.common.entities.golems;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BeetrootBlock;
import net.minecraft.world.level.block.CactusBlock;
import net.minecraft.world.level.block.CarrotBlock;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.CaveVines;
import net.minecraft.world.level.block.CaveVinesBlock;
import net.minecraft.world.level.block.CaveVinesPlantBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.PitcherCropBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.TorchflowerCropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.PotatoBlock;
import net.minecraft.world.level.block.PumpkinBlock;
import net.minecraft.world.level.block.SugarCaneBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

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
                || block == Blocks.POTATOES && type == PotatoBlock.class
                || block == Blocks.BEETROOTS && type == BeetrootBlock.class
                || block == Blocks.TORCHFLOWER_CROP && type == TorchflowerCropBlock.class) return ((CropBlock) block).isMaxAge(state);
        // Native torchflower growth replaces the final crop stage with the actual flower.
        if (block == Blocks.TORCHFLOWER && type == FlowerBlock.class) return true;
        if (block == Blocks.PITCHER_CROP && type == PitcherCropBlock.class) {
            if (state.getValue(PitcherCropBlock.AGE) != PitcherCropBlock.MAX_AGE
                    || state.getValue(DoublePlantBlock.HALF) != DoubleBlockHalf.LOWER) return false;
            BlockPos upper = pos.above();
            if (level.isOutsideBuildHeight(upper) || !level.getWorldBorder().isWithinBounds(upper) || !level.hasChunkAt(upper)) return false;
            BlockState other = level.getBlockState(upper);
            return other.is(Blocks.PITCHER_CROP) && other.getValue(DoublePlantBlock.HALF) == DoubleBlockHalf.UPPER
                    && other.getValue(PitcherCropBlock.AGE) == PitcherCropBlock.MAX_AGE;
        }
        if (block == Blocks.SWEET_BERRY_BUSH && type == SweetBerryBushBlock.class)
            return state.getValue(SweetBerryBushBlock.AGE) == SweetBerryBushBlock.MAX_AGE;
        if (berryHarvest(state)) return state.getValue(CaveVines.BERRIES);
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

    /** These finite native plants are picked rather than uprooted; Order must not replant around them. */
    static boolean berryHarvest(BlockState state) {
        if (state == null) return false;
        Block block = state.getBlock();
        return block == Blocks.SWEET_BERRY_BUSH && block.getClass() == SweetBerryBushBlock.class
                || block == Blocks.CAVE_VINES && block.getClass() == CaveVinesBlock.class
                || block == Blocks.CAVE_VINES_PLANT && block.getClass() == CaveVinesPlantBlock.class;
    }

    static BlockState pickedBerries(BlockState state) {
        return state.is(Blocks.SWEET_BERRY_BUSH) ? state.setValue(SweetBerryBushBlock.AGE, 1)
                : state.setValue(CaveVines.BERRIES, false);
    }
}
