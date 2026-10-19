package thaumcraft.common.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.api.visnet.VisNetHandler;
import thaumcraft.common.config.ThaumcraftNodeDevices;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.tiles.TileVisRelay;

/** Original metalDevice14: direct clicked-side facing, half-cell axial body,
 * non-air opposite support, no redstone/source state or GUI. */
public final class BlockVisRelay extends BaseEntityBlock {
    public static final MapCodec<BlockVisRelay> CODEC = simpleCodec(BlockVisRelay::new);
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final BooleanProperty LINKED = BooleanProperty.create("linked");
    public BlockVisRelay(Properties properties) {
        super(properties.noOcclusion().lightLevel(state -> state.getValue(LINKED) ? 10 : 2));
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.UP).setValue(LINKED, false));
    }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(FACING, LINKED); }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new TileVisRelay(pos, state); }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.ENTITYBLOCK_ANIMATED; }
    @Override protected VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) { return Shapes.empty(); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        if (!context.getLevel().isClientSide && !VisNetHandler.isNativeServerLevel(context.getLevel())) return null;
        BlockState state = defaultBlockState().setValue(FACING, context.getClickedFace()); // NOT the opposite.
        BlockPos support = context.getClickedPos().relative(context.getClickedFace().getOpposite());
        return context.getLevel().isInWorldBounds(support) && context.getLevel().hasChunkAt(support)
                && !context.getLevel().getBlockState(support).isAir() ? state : null;
    }
    @Override protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos support = pos.relative(state.getValue(FACING).getOpposite());
        return !level.isOutsideBuildHeight(support) && (!level.hasChunkAt(support) || !level.getBlockState(support).isAir());
    }
    private static VoxelShape shape(Direction facing) {
        return switch (facing) {
            case UP -> box(5, 0, 5, 11, 8, 11); case DOWN -> box(5, 8, 5, 11, 16, 11);
            case EAST -> box(0, 5, 5, 8, 11, 11); case WEST -> box(8, 5, 5, 16, 11, 11);
            case SOUTH -> box(5, 5, 0, 11, 11, 8); case NORTH -> box(5, 5, 8, 11, 11, 16);
        };
    }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return shape(state.getValue(FACING)); }
    @Override protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return shape(state.getValue(FACING)); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ThaumcraftNodeDevices.VIS_RELAY_BLOCK_ENTITY.get(), TileVisRelay::tick);
    }
    @Override protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block changed, BlockPos from, boolean moving) {
        if (level.isClientSide || !VisNetHandler.isNativeServerLevel(level) || !level.isInWorldBounds(pos) || !level.hasChunkAt(pos)
                || level.getBlockState(pos) != state) return;
        if (level.getBlockEntity(pos) instanceof TileVisRelay relay) relay.dropIfUnsupported();
    }
    @Override protected ItemInteractionResult useItemOn(ItemStack held, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && !VisNetHandler.isNativeServerLevel(level)) return ItemInteractionResult.FAIL;
        if (!pos.equals(hit.getBlockPos()) || !level.hasChunkAt(pos) || level.getBlockState(pos) != state
                || player.getItemInHand(hand) != held || !(level.getBlockEntity(pos) instanceof TileVisRelay relay)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        return itemResult(relay.interact(new UseOnContext(player, hand, hit)));
    }
    /** Root wires this into the existing wand useOn chain for sneaking use. */
    public static InteractionResult tryWandInteraction(UseOnContext context) {
        if (!(context.getItemInHand().getItem() instanceof ItemWandCasting) || context.getPlayer() == null) return InteractionResult.PASS;
        Level level = context.getLevel(); BlockPos pos = context.getClickedPos();
        if (!level.isClientSide && !VisNetHandler.isNativeServerLevel(level)) return InteractionResult.FAIL;
        if (!level.isInWorldBounds(pos) || !level.hasChunkAt(pos) || !(level.getBlockEntity(pos) instanceof TileVisRelay relay)) return InteractionResult.PASS;
        return relay.interact(context);
    }
    private static ItemInteractionResult itemResult(InteractionResult result) {
        return switch (result) {
            case SUCCESS -> ItemInteractionResult.SUCCESS; case CONSUME -> ItemInteractionResult.CONSUME;
            case CONSUME_PARTIAL -> ItemInteractionResult.CONSUME_PARTIAL; case FAIL -> ItemInteractionResult.FAIL;
            default -> ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        };
    }
}
