package thaumcraft.common.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * Original BlockCosmeticWoodSlab: the greatwood and the silverwood half block.
 * <p>
 * The original slab had two kinds, the greatwood planks and the silverwood planks, and took the texture of the
 * wooden device. The native port has the greatwood and the silverwood stairs but not their half blocks.
 * <p>
 * Oracle: {@code thaumcraft/common/blocks/BlockCosmeticWoodSlab.java}, lines 37-89.
 */
public final class BlockCosmeticWoodSlab extends SlabBlock {
    public static final MapCodec<BlockCosmeticWoodSlab> CODEC = simpleCodec(BlockCosmeticWoodSlab::new);
    /** The two original slab kinds: zero is greatwood and one is silverwood. */
    public static final IntegerProperty VARIANT = IntegerProperty.create("variant", 0, 1);

    public BlockCosmeticWoodSlab(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(VARIANT, 0));
    }

    @Override public MapCodec<? extends SlabBlock> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(VARIANT);
    }

    // BlockCosmeticWoodSlab:44 applies to both single AND double blocks.
    @Override protected int getLightBlock(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getValue(WATERLOGGED) ? super.getLightBlock(state, level, pos) : 0;
    }
    @Override protected boolean useShapeForLightOcclusion(BlockState state) { return state.getValue(WATERLOGGED); }
    @Override protected boolean propagatesSkylightDown(BlockState level, BlockGetter reader, BlockPos pos) { return !level.getValue(WATERLOGGED); }
}
