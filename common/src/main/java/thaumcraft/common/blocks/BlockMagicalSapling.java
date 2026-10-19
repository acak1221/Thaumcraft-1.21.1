package thaumcraft.common.blocks;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.common.lib.world.WorldGenGreatwoodTrees;
import thaumcraft.common.lib.world.WorldGenSilverwoodTrees;

/** BlockCustomPlant:134-171, sapling variants ONLY. Not BonemealableBlock in TC4. */
public final class BlockMagicalSapling extends BushBlock {
    public static final MapCodec<BlockMagicalSapling> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.BOOL.fieldOf("silverwood").forGetter(BlockMagicalSapling::isSilverwood), propertiesCodec())
            .apply(instance, BlockMagicalSapling::new));
    private static final VoxelShape SHAPE = Block.box(1.6, 0, 1.6, 14.4, 12.8, 14.4);
    private final boolean silverwood;

    public BlockMagicalSapling(boolean silverwood, Properties properties) {
        super(properties);
        this.silverwood = silverwood;
    }

    @Override protected MapCodec<BlockMagicalSapling> codec() { return CODEC; }
    public boolean isSilverwood() { return silverwood; }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return SHAPE; }

    @Override protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        // TC4's placement check returns true, but BlockBush's stay check still
        // requires soil and either light>=8 or sky visibility.
        return super.canSurvive(state, level, pos) && (level.getRawBrightness(pos, 0) >= 8 || level.canSeeSky(pos));
    }

    @Override protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.canSurvive(level, pos)) {
            level.destroyBlock(pos, true);
            return;
        }
        if (level.getMaxLocalRawBrightness(pos.above()) >= 9 && random.nextInt(silverwood ? 50 : 25) == 0) grow(level, pos, random);
    }

    /** Direct growth entry point also keeps the original elemental-hoe contract possible. */
    public boolean grow(ServerLevel level, BlockPos pos, RandomSource random) {
        BlockState sapling = level.getBlockState(pos);
        if (!sapling.is(this)) return false;
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        boolean grown = silverwood ? new WorldGenSilverwoodTrees(true, 7, 5).generate(level, random, pos)
                : new WorldGenGreatwoodTrees(true).generate(level, random, pos, false);
        if (!grown) level.setBlock(pos, sapling, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        return grown;
    }

    /** Preserve BlockCustomPlant:130-132's permissive placement, not permissive survival. */
    public static final class SaplingItem extends BlockItem {
        public SaplingItem(Block block, Item.Properties properties) { super(block, properties); }
        @Override protected boolean mustSurvive() { return false; }
    }
}
