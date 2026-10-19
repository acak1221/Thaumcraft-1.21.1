package thaumcraft.common.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Native half/double placement and collision, retaining the original dry slab opacity0 quirk. */
public final class BlockArcaneStoneSlab extends SlabBlock {
    public static final MapCodec<BlockArcaneStoneSlab> CODEC = simpleCodec(BlockArcaneStoneSlab::new);

    public BlockArcaneStoneSlab(Properties properties) { super(properties); }
    @Override public MapCodec<? extends SlabBlock> codec() { return CODEC; }

    // BlockCosmeticStoneSlab:44 applies to both single AND double blocks.
    // Do not make the dry double opaque just because its native shape is a cube.
    // Waterlogging has no 1.7 counterpart and keeps native wet-state lighting.
    @Override protected int getLightBlock(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getValue(WATERLOGGED) ? super.getLightBlock(state, level, pos) : 0;
    }
    @Override protected boolean useShapeForLightOcclusion(BlockState state) { return state.getValue(WATERLOGGED); }
    @Override protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) { return !state.getValue(WATERLOGGED); }
}
