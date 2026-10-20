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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Original BlockCustomPlant:62-68,119-143,174-182,194-211,224-229. */
public final class BlockCustomPlant extends BushBlock {
    public static final MapCodec<BlockCustomPlant> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.BOOL.fieldOf("cinderpearl").forGetter(BlockCustomPlant::isCinderpearl), propertiesCodec()).apply(instance, BlockCustomPlant::new));
    private static final VoxelShape SHAPE = Block.box(1.6, 0, 1.6, 14.4, 12.8, 14.4);
    private static ClientEffects clientEffects;
    private final boolean cinderpearl;
    public BlockCustomPlant(boolean cinderpearl, Properties properties) { super(properties); this.cinderpearl = cinderpearl; }
    public boolean isCinderpearl() { return cinderpearl; }
    @Override protected MapCodec<BlockCustomPlant> codec() { return CODEC; }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return SHAPE; }
    @Override protected boolean mayPlaceOn(BlockState soil, BlockGetter level, BlockPos pos) {
        // Forge1.7 first accepts the inherited BlockBush soils, then the
        // Desert plant type adds sand (both old metadata variants).
        return soil.is(Blocks.GRASS_BLOCK) || soil.is(Blocks.DIRT) || soil.is(Blocks.COARSE_DIRT) || soil.is(Blocks.PODZOL) || soil.is(Blocks.FARMLAND)
                || cinderpearl && (soil.is(Blocks.SAND) || soil.is(Blocks.RED_SAND));
    }
    @Override protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return mayPlaceOn(level.getBlockState(pos.below()), level, pos.below());
    }
    @Override protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.canSurvive(level, pos)) level.destroyBlock(pos, true);
    }
    @Override public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (clientEffects != null) clientEffects.tick(level, pos, cinderpearl, random);
    }
    public interface ClientEffects { void tick(Level level, BlockPos pos, boolean cinderpearl, RandomSource random); }
    public static void setClientEffects(ClientEffects effects) { clientEffects = effects; }
    /** Placement itself was permissive; ordinary stay/random ticks still enforce soil. */
    public static final class PlantItem extends BlockItem {
        public PlantItem(Block block, Item.Properties properties) { super(block, properties); }
        @Override protected boolean mustSurvive() { return false; }
    }
}
