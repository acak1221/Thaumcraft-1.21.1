package thaumcraft.common.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.common.config.ThaumcraftArcaneBore;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.lib.bore.ArcaneBoreMath;
import thaumcraft.common.lib.bore.BoreWork;
import thaumcraft.common.tiles.TileArcaneBore;
import thaumcraft.common.tiles.TileArcaneBoreBase;

/** Original woodenDevice5. Logical mining direction controls the two-cell shape;
 * attachment controls base position, independently of mining/nozzle direction. */
public final class BlockArcaneBore extends BaseEntityBlock {
    public static final MapCodec<BlockArcaneBore> CODEC = simpleCodec(BlockArcaneBore::new);
    public static final DirectionProperty MINING = BlockStateProperties.FACING;
    public static final DirectionProperty ATTACHMENT = DirectionProperty.create("attachment", Direction.UP, Direction.DOWN);

    public BlockArcaneBore(Properties properties) {
        // dynamicShape keeps native large-collision neighbour traversal enabled.
        super(properties.strength(2.5F, 6.0F).sound(SoundType.WOOD).noOcclusion().dynamicShape());
        registerDefaultState(stateDefinition.any().setValue(MINING, Direction.UP).setValue(ATTACHMENT, Direction.UP));
    }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(MINING, ATTACHMENT); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction attachment = context.getClickedFace();
        if (attachment != Direction.UP && attachment != Direction.DOWN) return null;
        BlockPos top = context.getClickedPos();
        if (!knownBase(context.getLevel(), top, attachment)) return null;
        return defaultBlockState().setValue(ATTACHMENT, attachment).setValue(MINING, placementDirection(context));
    }
    private static Direction placementDirection(BlockPlaceContext context) {
        Player player = context.getPlayer();
        if (player == null) return Direction.UP;
        BlockPos pos = context.getClickedPos();
        // Original vanilla1.7.10 BlockPistonBase.func_150071_a, not modern
        // nearest-looking-direction. Its posY-yOffset is native feet/bounds minY.
        if (Math.abs((float) player.getX() - (float) pos.getX()) < 2.0F
                && Math.abs((float) player.getZ() - (float) pos.getZ()) < 2.0F) {
            double placementHeight = player.getBoundingBox().minY + 1.82;
            if (placementHeight - pos.getY() > 2.0) return Direction.UP;
            if (pos.getY() - placementHeight > 0.0) return Direction.DOWN;
        }
        int quadrant = Mth.floor((double) (player.getYRot() * 4.0F / 360.0F) + .5) & 3;
        return switch (quadrant) { case 0 -> Direction.NORTH; case 1 -> Direction.EAST; case 2 -> Direction.SOUTH; default -> Direction.WEST; };
    }
    public static boolean knownBase(LevelReader level, BlockPos top, Direction attachment) {
        if (attachment != Direction.UP && attachment != Direction.DOWN) return false;
        BlockPos basePos = ArcaneBoreMath.basePosition(top, attachment);
        return !level.isOutsideBuildHeight(basePos) && level.hasChunkAt(basePos)
                && level.getBlockState(basePos).is(ThaumcraftArcaneBore.ARCANE_BORE_BASE.get())
                && level.getBlockEntity(basePos) instanceof TileArcaneBoreBase base && base.isCurrent();
    }
    @Override protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos basePos = ArcaneBoreMath.basePosition(pos, state.getValue(ATTACHMENT));
        // Unknown/unloaded support is not evidence of missing support.
        return !level.isOutsideBuildHeight(basePos) && (!level.hasChunkAt(basePos) || knownBase(level, pos, state.getValue(ATTACHMENT)));
    }
    @Override protected BlockState updateShape(BlockState state, Direction side, BlockState neighbor,
                                               LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (side == state.getValue(ATTACHMENT).getOpposite() && !canSurvive(state, level, pos))
            return Blocks.AIR.defaultBlockState(); // Native self-removal; onRemove delegates stock evacuation once.
        return super.updateShape(state, side, neighbor, level, pos, neighborPos);
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new TileArcaneBore(pos, state); }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.ENTITYBLOCK_ANIMATED; }
    private static VoxelShape shape(Direction direction) {
        return Shapes.box(direction.getStepX() < 0 ? -1 : 0, direction.getStepY() < 0 ? -1 : 0, direction.getStepZ() < 0 ? -1 : 0,
                direction.getStepX() > 0 ? 2 : 1, direction.getStepY() > 0 ? 2 : 1, direction.getStepZ() > 0 ? 2 : 1);
    }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return shape(state.getValue(MINING)); }
    @Override protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return shape(state.getValue(MINING)); }
    @Override protected VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) { return Shapes.empty(); }
    @Override protected boolean isSignalSource(BlockState state) { return true; } // Native wire connection; no emitted power.
    @Override protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction side) { return 0; }
    @Override protected int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction side) { return 0; }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ThaumcraftArcaneBore.ARCANE_BORE_BLOCK_ENTITY.get(), TileArcaneBore::tick);
    }
    @Override protected ItemInteractionResult useItemOn(ItemStack held, BlockState state, Level level, BlockPos pos,
                                                       Player player, InteractionHand hand, BlockHitResult hit) {
        if (held.getItem() instanceof ItemWandCasting) return BlockArcaneBoreBase.itemResult(tryWandInteraction(new UseOnContext(player, hand, hit)));
        return BlockArcaneBoreBase.itemResult(open(level, pos, player));
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) { return open(level, pos, player); }
    private InteractionResult open(Level level, BlockPos pos, Player player) {
        if (!level.hasChunkAt(pos) || !(level.getBlockEntity(pos) instanceof TileArcaneBore top)) return InteractionResult.PASS;
        return top.openMenu(player);
    }
    public static InteractionResult tryWandInteraction(UseOnContext context) {
        if (context.getPlayer() == null || !(context.getItemInHand().getItem() instanceof ItemWandCasting)) return InteractionResult.PASS;
        Level level = context.getLevel(); BlockPos pos = context.getClickedPos();
        if (!level.isInWorldBounds(pos) || !level.hasChunkAt(pos)
                || !(level.getBlockEntity(pos) instanceof TileArcaneBore top)) return InteractionResult.PASS;
        return top.onWandUse(context);
    }
    @Override protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        if (!replacement.is(this) && level.getBlockEntity(pos) instanceof TileArcaneBore top && !level.isClientSide)
            BoreWork.evacuate(top); // REAL root native-retirement frame required; no local drops or stack clear.
        super.onRemove(state, level, pos, replacement, moving);
    }
}
