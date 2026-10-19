package thaumcraft.common.blocks;

import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.api.crafting.IInfusionStabiliser;

/** Original BlockCandle, plain tallow variant. Dye variants remain a separate content unit. */
public final class BlockTallowCandle extends Block implements IInfusionStabiliser {
    public static final MapCodec<BlockTallowCandle> CODEC = simpleCodec(BlockTallowCandle::new);
    private static final VoxelShape SHAPE = box(6, 0, 6, 10, 8, 10);

    public BlockTallowCandle(Properties properties) {
        // Original hardness .1, cloth sound, no collision, light int(.95*15)=14.
        super(properties.strength(.1F).sound(SoundType.WOOL).noCollission().noOcclusion().lightLevel(state -> 14));
    }

    @Override protected MapCodec<? extends Block> codec() { return CODEC; }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return SHAPE; }
    @Override protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return Shapes.empty(); }
    @Override protected VoxelShape getBlockSupportShape(BlockState state, BlockGetter level, BlockPos pos) { return Shapes.empty(); }
    @Override protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        return !level.isOutsideBuildHeight(below) && level.hasChunkAt(below)
                && level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().canSurvive(context.getLevel(), context.getClickedPos()) ? defaultBlockState() : null;
    }
    @Override protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor,
                                                LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return direction == Direction.DOWN && !state.canSurvive(level, pos) ? Blocks.AIR.defaultBlockState() : state;
    }
    @Override protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, BlockPos neighborPos, boolean moving) {
        if (!level.isClientSide && !state.canSurvive(level, pos)) level.destroyBlock(pos, true);
    }
    @Override protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) { return List.of(new ItemStack(this)); }
    @Override public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        level.addParticle(ParticleTypes.SMOKE, pos.getX() + .5, pos.getY() + .7, pos.getZ() + .5, 0, 0, 0);
        level.addParticle(ParticleTypes.FLAME, pos.getX() + .5, pos.getY() + .7, pos.getZ() + .5, 0, 0, 0);
    }
    @Override public boolean canStabaliseInfusion(Level level, BlockPos pos) { return true; }
}
