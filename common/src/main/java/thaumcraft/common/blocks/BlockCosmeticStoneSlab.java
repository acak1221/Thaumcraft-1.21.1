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
 * Original BlockCosmeticStoneSlab: the arcane and the eldritch stone slabs.
 * The original metadata became a flattened block variant.
 */
public final class BlockCosmeticStoneSlab extends SlabBlock {
    public static final MapCodec<BlockCosmeticStoneSlab> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.intRange(0, 1).fieldOf("variant").forGetter(BlockCosmeticStoneSlab::variant),
            propertiesCodec()).apply(instance, BlockCosmeticStoneSlab::new));

    private final int variant;

    public BlockCosmeticStoneSlab(int variant, Properties properties) {
        super(properties);
        if (variant != EldritchBlockRules.SLAB_STONE_ARCANE && variant != EldritchBlockRules.SLAB_STONE_ELDRITCH) {
            throw new IllegalArgumentException("Unknown original cosmetic stone slab variant " + variant);
        }
        this.variant = variant;
    }

    @Override public MapCodec<? extends SlabBlock> codec() { return CODEC; }
    public int variant() { return variant; }

    // BlockCosmeticStoneSlab:44 applies to both the single and the double block.
    // Waterlogging has no 1.7 counterpart and keeps the native wet-state lighting.
    @Override protected int getLightBlock(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getValue(WATERLOGGED) ? super.getLightBlock(state, level, pos) : 0;
    }
    @Override protected boolean useShapeForLightOcclusion(BlockState state) { return state.getValue(WATERLOGGED); }
    @Override protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) { return !state.getValue(WATERLOGGED); }
}
