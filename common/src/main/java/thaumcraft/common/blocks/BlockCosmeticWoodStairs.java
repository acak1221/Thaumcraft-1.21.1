package thaumcraft.common.blocks;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Original BlockCosmeticStairs over the Greatwood / Silverwood planks (blockWoodenDevice metadata 6/7): native stair placement and
 * corners, with the original opacity-0 quirk (BlockCosmeticStairs:32) kept for dry stairs, like the arcane stone ones.
 */
public final class BlockCosmeticWoodStairs extends StairBlock {
    public static final MapCodec<BlockCosmeticWoodStairs> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            BlockState.CODEC.fieldOf("base_state").forGetter(stairs -> stairs.parent),
            propertiesCodec()).apply(instance, BlockCosmeticWoodStairs::new));

    private final BlockState parent;

    public BlockCosmeticWoodStairs(BlockState parent, Properties properties) {
        super(parent, properties);
        this.parent = parent;
    }

    @Override public MapCodec<? extends StairBlock> codec() { return CODEC; }

    @Override protected int getLightBlock(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getValue(WATERLOGGED) ? super.getLightBlock(state, level, pos) : 0;
    }
    @Override protected boolean useShapeForLightOcclusion(BlockState state) { return state.getValue(WATERLOGGED); }
    @Override protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) { return !state.getValue(WATERLOGGED); }
}
