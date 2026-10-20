package thaumcraft.common.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * Original BlockCosmeticStoneSlab: the arcane and the eldritch half block.
 * <p>
 * The original slab had two kinds, the arcane stone and the eldritch stone, and took the texture of the
 * cosmetic solid. The native port already has the slab of the arcane stone bricks, so this block serves the
 * original two kinds.
 * <p>
 * Oracle: {@code thaumcraft/common/blocks/BlockCosmeticStoneSlab.java}, lines 37-89.
 */
public final class BlockCosmeticStoneSlab extends SlabBlock {
    public static final MapCodec<BlockCosmeticStoneSlab> CODEC = simpleCodec(BlockCosmeticStoneSlab::new);
    /** The two original slab kinds: zero is arcane and one is eldritch. */
    public static final IntegerProperty VARIANT = IntegerProperty.create("variant", 0, 1);

    public BlockCosmeticStoneSlab(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(VARIANT, 0));
    }

    @Override public MapCodec<? extends SlabBlock> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(VARIANT);
    }

    // BlockCosmeticStoneSlab:44 applies to both single AND double blocks.
    @Override protected int getLightBlock(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getValue(WATERLOGGED) ? super.getLightBlock(state, level, pos) : 0;
    }
    @Override protected boolean useShapeForLightOcclusion(BlockState state) { return state.getValue(WATERLOGGED); }
    @Override protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) { return !state.getValue(WATERLOGGED); }
}
