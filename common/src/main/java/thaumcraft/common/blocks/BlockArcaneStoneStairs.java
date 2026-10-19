package thaumcraft.common.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.common.config.ThaumcraftMasonry;

/** Native stair placement/corners, with the original non-world-aware resistance delegation. */
public final class BlockArcaneStoneStairs extends StairBlock {
    public static final MapCodec<BlockArcaneStoneStairs> CODEC = simpleCodec(BlockArcaneStoneStairs::new);

    public BlockArcaneStoneStairs(Properties properties) {
        super(ThaumcraftMasonry.ARCANE_STONE_BRICKS.get().defaultBlockState(), properties);
    }

    @Override public MapCodec<? extends StairBlock> codec() { return CODEC; }

    // BlockCosmeticStairs:32 forces opacity0. Keep that dry legacy behavior,
    // including direct skylight, rather than native shape-based light sealing.
    // Waterlogging has no 1.7 counterpart and keeps native wet-state lighting.
    @Override protected int getLightBlock(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getValue(WATERLOGGED) ? super.getLightBlock(state, level, pos) : 0;
    }
    @Override protected boolean useShapeForLightOcclusion(BlockState state) { return state.getValue(WATERLOGGED); }
    @Override protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) { return !state.getValue(WATERLOGGED); }

    @Override public float getExplosionResistance() {
        // Vanilla1.7 BlockStairs.getExplosionResistance(Entity) delegates to
        // the reference's base setResistance10 (10*3/5 = effective6);
        // BlockCosmeticSolid's world-aware
        // metadata6/7 resistance20 override does not apply to a stair.
        // Modern StairBlock would otherwise delegate to our flattened bricks20.
        return 6.0F;
    }
}
