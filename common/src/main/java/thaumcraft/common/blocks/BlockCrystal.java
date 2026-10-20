package thaumcraft.common.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.api.crafting.IInfusionStabiliser;
import thaumcraft.common.config.ThaumcraftCrystalClusters;
import thaumcraft.common.tiles.TileCrystal;

/** Original BlockCrystal0..6: full physical cube, six solid-face attachments, orientation owned by its inert tile. */
public final class BlockCrystal extends BaseEntityBlock implements IInfusionStabiliser {
    public static final MapCodec<BlockCrystal> CODEC = simpleCodec(BlockCrystal::new);
    public BlockCrystal(Properties properties) { super(properties); }
    @FunctionalInterface public interface ClientEffects { void sparkle(Level level, BlockPos position, int metadata, RandomSource random); }
    private static ClientEffects clientEffects = (level, position, metadata, random) -> {};
    public static void setClientEffects(ClientEffects effects) { clientEffects = java.util.Objects.requireNonNull(effects); }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override public BlockEntity newBlockEntity(BlockPos position, BlockState state) { return new TileCrystal(position, state); }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.ENTITYBLOCK_ANIMATED; }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos position, CollisionContext context) { return Shapes.block(); }
    @Override protected VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos position, CollisionContext context) { return Shapes.block(); }
    // Original isOpaqueCube/isNormalCube are false, so a cluster cannot be another cluster's solid support.
    @Override protected VoxelShape getBlockSupportShape(BlockState state, BlockGetter world, BlockPos position) { return Shapes.empty(); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        return supported(context.getLevel(), context.getClickedPos(), context.getClickedFace()) ? defaultBlockState() : null;
    }
    @Override protected boolean canSurvive(BlockState state, LevelReader world, BlockPos position) { return hasAnySupport(world, position); }
    public static boolean supported(LevelReader world, BlockPos position, Direction orientation) {
        BlockPos support = position.relative(orientation.getOpposite());
        return world.hasChunkAt(support) && world.getBlockState(support).isFaceSturdy(world, support, orientation);
    }
    public static boolean hasAnySupport(LevelReader world, BlockPos position) {
        for (Direction direction : Direction.values()) if (supported(world, position, direction)) return true;
        return false;
    }
    @Override protected void neighborChanged(BlockState state, Level world, BlockPos position, Block changed, BlockPos from, boolean moved) {
        super.neighborChanged(state, world, position, changed, from, moved);
        if (world.isClientSide || !world.getBlockState(position).is(this)) return;
        if (!hasAnySupport(world, position) || !(world.getBlockEntity(position) instanceof TileCrystal tile)
                || tile.orientation >= 0 && tile.orientation <= 5 && !supported(world, position, Direction.from3DDataValue(tile.orientation)))
            world.destroyBlock(position, true);
    }
    @Override public void animateTick(BlockState state, Level world, BlockPos position, RandomSource random) {
        clientEffects.sparkle(world, position, ThaumcraftCrystalClusters.metadata(state), random);
    }
    @Override public boolean canStabaliseInfusion(Level level, BlockPos pos) { return true; }
}
