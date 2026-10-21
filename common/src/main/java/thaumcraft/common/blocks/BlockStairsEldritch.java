package thaumcraft.common.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.common.config.ThaumcraftEldritchBlocks;
import thaumcraft.common.lib.world.EldritchBlockRules;

/**
 * Original BlockCosmeticStairs with the eldritch reference: the eldritch tile
 * stairs used by the Outer Lands room generator.
 */
public final class BlockStairsEldritch extends StairBlock {
    public static final MapCodec<BlockStairsEldritch> CODEC = simpleCodec(BlockStairsEldritch::new);

    public BlockStairsEldritch(Properties properties) {
        super(ThaumcraftEldritchBlocks.COSMETIC_SOLID.get().defaultBlockState()
                .setValue(BlockCosmeticSolid.VARIANT, EldritchBlockRules.COSMETIC_ELDRITCH_TILE), properties);
    }

    @Override public MapCodec<? extends StairBlock> codec() { return CODEC; }

    // BlockCosmeticStairs:32 forces the light opacity to zero for the dry stair.
    @Override protected int getLightBlock(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getValue(WATERLOGGED) ? super.getLightBlock(state, level, pos) : 0;
    }
    @Override protected boolean useShapeForLightOcclusion(BlockState state) { return state.getValue(WATERLOGGED); }
    @Override protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) { return !state.getValue(WATERLOGGED); }

    @Override public float getExplosionResistance() {
        // Vanilla1.7 BlockStairs copies the hardness and the blast resistance of the block it
        // copies the texture of: the cosmetic solid family, the hardness two and the effective
        // resistance six of the setter value ten (10 * 3 / 5).
        return 6.0F;
    }
}
