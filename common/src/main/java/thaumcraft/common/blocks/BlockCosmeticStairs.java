package thaumcraft.common.blocks;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Original BlockCosmeticStairs: the stairs that take the texture of another cosmetic block.
 * <p>
 * The original built the eldritch stairs from the eldritch stone of the cosmetic solid, the greatwood and the
 * silverwood stairs from the planks of the wooden device. The native port already has the greatwood and the
 * silverwood stairs, so this block serves the eldritch stairs.
 * <p>
 * Oracle: {@code thaumcraft/common/blocks/BlockCosmeticStairs.java}, lines 23-43.
 */
public final class BlockCosmeticStairs extends StairBlock {
    public static final MapCodec<BlockCosmeticStairs> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            BlockState.CODEC.fieldOf("base_state").forGetter(stairs -> stairs.parent),
            propertiesCodec()).apply(instance, BlockCosmeticStairs::new));

    private final BlockState parent;

    public BlockCosmeticStairs(BlockState parent, Properties properties) {
        super(parent, properties);
        this.parent = parent;
    }

    @Override public MapCodec<? extends StairBlock> codec() { return CODEC; }

    /** The block the stairs take their texture from. */
    public BlockState parentState() { return parent; }

    // BlockCosmeticStairs:32 sets the light opacity of the original to zero.
    @Override protected int getLightBlock(BlockState state, BlockGetter level, BlockPos pos) {
        return lightOpacity(state.getValue(WATERLOGGED), super.getLightBlock(state, level, pos));
    }

    /**
     * BlockCosmeticStairs:32 - the original sets the light opacity of the stairs to zero, so a
     * dry stair lets the light through; a waterlogged stair keeps the vanilla behaviour.
     *
     * <p>The rule is a pure function so that it can be asserted without constructing a block: a
     * constructed block would leave an unregistered intrusive holder behind and break the
     * registry freeze of the whole test suite.</p>
     *
     * @param waterlogged    the stair carries water
     * @param vanillaOpacity the opacity the vanilla stair would have
     */
    public static int lightOpacity(boolean waterlogged, int vanillaOpacity) {
        return waterlogged ? vanillaOpacity : 0;
    }
    @Override protected boolean useShapeForLightOcclusion(BlockState state) { return state.getValue(WATERLOGGED); }
    @Override protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) { return !state.getValue(WATERLOGGED); }
}
