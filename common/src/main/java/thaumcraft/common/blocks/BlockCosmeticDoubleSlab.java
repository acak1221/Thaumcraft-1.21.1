package thaumcraft.common.blocks;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import thaumcraft.common.config.ThaumcraftEldritchBlocks;
import thaumcraft.common.lib.world.EldritchBlockRules;

/**
 * Original blockCosmeticDoubleSlabWood and blockCosmeticDoubleSlabStone: the double halves of the
 * four cosmetic slabs.
 *
 * <p>The original registered two double slab blocks next to the two single slab blocks, one for the
 * wood and one for the stone, and each of them carried the same metadata as its single slab: the
 * greatwood and the silverwood, the arcane and the eldritch stone. The double slab is a full block
 * (the original BlockSlab with the double flag sets the full bounds, BlockCosmeticWoodSlab:41-45)
 * and it drops the single slab: two wooden slabs (BlockCosmeticWoodSlab:56-58) and one stone slab
 * (BlockCosmeticStoneSlab:56-58). The original never offered the double slab in the creative tab
 * (BlockCosmeticWoodSlab:68-74) and no recipe produces it.
 *
 * <p>In 1.21.1 the double half of a slab is the {@code double} value of the slab type, so the four
 * double slabs are four blocks whose default state is that value.
 *
 * <p>Original sources: {@code thaumcraft/common/config/ConfigBlocks.java}:300-301 (the two blocks)
 * and :348-349 (the registration), {@code thaumcraft/common/blocks/BlockCosmeticWoodSlab.java} and
 * {@code thaumcraft/common/blocks/BlockCosmeticStoneSlab.java}.
 */
public final class BlockCosmeticDoubleSlab extends SlabBlock {
    public static final MapCodec<BlockCosmeticDoubleSlab> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.intRange(0, 3).fieldOf("variant").forGetter(BlockCosmeticDoubleSlab::variant),
            propertiesCodec()).apply(instance, BlockCosmeticDoubleSlab::new));

    private final int variant;

    public BlockCosmeticDoubleSlab(int variant, Properties properties) {
        super(properties);
        if (variant < 0 || variant > 3) throw new IllegalArgumentException("Unknown original cosmetic double slab variant " + variant);
        this.variant = variant;
        registerDefaultState(stateDefinition.any().setValue(TYPE, SlabType.DOUBLE).setValue(WATERLOGGED, Boolean.FALSE));
    }

    @Override public MapCodec<? extends SlabBlock> codec() { return CODEC; }

    /** The original metadata value of this double slab. */
    public int variant() { return variant; }

    /** Whether this double slab belongs to the wooden family. */
    public boolean wooden() { return EldritchBlockRules.doubleSlabIsWooden(variant); }

    /** The original drops the single slab: two wooden ones and one stone one. */
    public int dropCount() { return EldritchBlockRules.doubleSlabDropCount(variant); }

    /** The single slab block of the same family that this double slab drops. */
    public net.minecraft.world.level.block.Block singleSlab() {
        return switch (variant) {
            case EldritchBlockRules.SLAB_DOUBLE_WOOD_GREATWOOD -> ThaumcraftEldritchBlocks.COSMETIC_WOOD_SLAB_GREATWOOD.get();
            case EldritchBlockRules.SLAB_DOUBLE_WOOD_SILVERWOOD -> ThaumcraftEldritchBlocks.COSMETIC_WOOD_SLAB_SILVERWOOD.get();
            case EldritchBlockRules.SLAB_DOUBLE_STONE_ARCANE -> ThaumcraftEldritchBlocks.COSMETIC_STONE_SLAB_ARCANE.get();
            default -> ThaumcraftEldritchBlocks.COSMETIC_STONE_SLAB_ELDRITCH.get();
        };
    }

    /** The double slab is always the full block; the original never placed a half of it. */
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) { return defaultBlockState(); }

    /** The original double slab is a full block and is never replaced by another one. */
    @Override protected boolean canBeReplaced(BlockState state, BlockPlaceContext context) { return false; }

    // BlockCosmeticWoodSlab:44 and BlockCosmeticStoneSlab:44 apply to both the single and the double
    // block. Waterlogging has no 1.7 counterpart and keeps the native wet-state lighting.
    @Override protected int getLightBlock(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getValue(WATERLOGGED) ? super.getLightBlock(state, level, pos) : 0;
    }
    @Override protected boolean useShapeForLightOcclusion(BlockState state) { return state.getValue(WATERLOGGED); }
    @Override protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) { return !state.getValue(WATERLOGGED); }
}
