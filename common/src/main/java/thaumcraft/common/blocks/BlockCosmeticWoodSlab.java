package thaumcraft.common.blocks;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.common.lib.world.EldritchBlockRules;

/**
 * Original BlockCosmeticWoodSlab: the greatwood and the silverwood slabs.
 * The original metadata became a flattened block variant.
 */
public final class BlockCosmeticWoodSlab extends SlabBlock {
    public static final MapCodec<BlockCosmeticWoodSlab> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.intRange(0, 1).fieldOf("variant").forGetter(BlockCosmeticWoodSlab::variant),
            propertiesCodec()).apply(instance, BlockCosmeticWoodSlab::new));

    private final int variant;

    public BlockCosmeticWoodSlab(int variant, Properties properties) {
        super(properties);
        if (variant != EldritchBlockRules.SLAB_WOOD_GREATWOOD && variant != EldritchBlockRules.SLAB_WOOD_SILVERWOOD) {
            throw new IllegalArgumentException("Unknown original cosmetic wood slab variant " + variant);
        }
        this.variant = variant;
    }

    @Override public MapCodec<? extends SlabBlock> codec() { return CODEC; }
    public int variant() { return variant; }

    // BlockCosmeticWoodSlab mirrors the stone slab and keeps the original dry opacity zero.
    @Override protected int getLightBlock(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getValue(WATERLOGGED) ? super.getLightBlock(state, level, pos) : 0;
    }
    @Override protected boolean useShapeForLightOcclusion(BlockState state) { return state.getValue(WATERLOGGED); }
    @Override protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) { return !state.getValue(WATERLOGGED); }
}
