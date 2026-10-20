package thaumcraft.common.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.common.config.ThaumcraftEldritchBlocks;

/**
 * Original BlockCosmeticStairs with the silverwood reference: the silverwood
 * stairs, metadata seven of the original wooden device family.
 */
public final class BlockStairsSilverwood extends StairBlock {
    public static final MapCodec<BlockStairsSilverwood> CODEC = simpleCodec(BlockStairsSilverwood::new);

    public BlockStairsSilverwood(Properties properties) {
        super(ThaumcraftEldritchBlocks.WOODEN_DEVICE.get().defaultBlockState()
                .setValue(BlockWoodenDevice.VARIANT, 7), properties);
    }

    @Override public MapCodec<? extends StairBlock> codec() { return CODEC; }

    // BlockCosmeticStairs:32 forces the light opacity to zero for the dry stair.
    @Override protected int getLightBlock(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getValue(WATERLOGGED) ? super.getLightBlock(state, level, pos) : 0;
    }
    @Override protected boolean useShapeForLightOcclusion(BlockState state) { return state.getValue(WATERLOGGED); }
    @Override protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) { return !state.getValue(WATERLOGGED); }

    @Override public float getExplosionResistance() {
        // The original stair delegates to the wooden device base resistance of ten.
        return 10.0F;
    }
}
