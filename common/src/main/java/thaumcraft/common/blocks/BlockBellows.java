package thaumcraft.common.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.common.config.ThaumcraftBellows;
import thaumcraft.common.tiles.TileBellows;

/** WoodenDevice0: original static .1.. .9 collision, six-way item-placement orientation. */
public final class BlockBellows extends BaseEntityBlock {
    public static final MapCodec<BlockBellows> CODEC = simpleCodec(BlockBellows::new);
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    private static final VoxelShape SHAPE = Block.box(1.6, 0, 1.6, 14.4, 16, 14.4);
    public BlockBellows(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.DOWN));
    }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(FACING); }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return SHAPE; }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.ENTITYBLOCK_ANIMATED; }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getClickedFace().getOpposite());
    }
    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        if (level.isClientSide || !(level.getBlockEntity(pos) instanceof TileBellows bellows)) return;
        BlockPos target = pos.relative(state.getValue(FACING));
        bellows.setOnVanillaFurnace(level.hasChunkAt(target) && level.getBlockState(target).is(Blocks.FURNACE));
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new TileBellows(pos, state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ThaumcraftBellows.BELLOWS_TILE.get(), TileBellows::tick);
    }
}
