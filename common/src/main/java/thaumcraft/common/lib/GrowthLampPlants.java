package thaumcraft.common.lib;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.CactusBlock;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.SugarCaneBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Original IGrowable / plants / vines and CropUtils maturity, with a native block-tag blacklist. */
public final class GrowthLampPlants {
    public static final net.minecraft.tags.TagKey<net.minecraft.world.level.block.Block> BLACKLIST = net.minecraft.tags.TagKey.create(
            net.minecraft.core.registries.Registries.BLOCK, net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("thaumcraft", "growth_lamp_blacklist"));
    private GrowthLampPlants() {}
    public static boolean eligible(Level level, BlockPos pos, BlockState state) {
        var block = state.getBlock();
        if (state.isAir() || state.is(Blocks.GRASS_BLOCK) || state.is(BLACKLIST)) return false;
        boolean plant = block instanceof BonemealableBlock || block instanceof BushBlock || block instanceof VineBlock
                || block instanceof SugarCaneBlock || block instanceof CactusBlock || state.is(BlockTags.FLOWERS);
        if (!plant) return false;
        if (block instanceof CropBlock crop && crop.isMaxAge(state)
                || block instanceof NetherWartBlock && state.getValue(NetherWartBlock.AGE) >= 3
                || block instanceof CocoaBlock && state.getValue(CocoaBlock.AGE) >= 2) return false;
        // Original default stackedCrops registrations skip every non-bottom segment, even when it is the top.
        if ((block instanceof SugarCaneBlock || block instanceof CactusBlock) && level.getBlockState(pos.below()).getBlock() == block) return false;
        return !(block instanceof BonemealableBlock growable) || block instanceof StemBlock || growable.isValidBonemealTarget(level, pos, state);
    }
}
