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
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.tiles.TileArcaneBoreBase;

/** Original woodenDevice4: stockless full collision/outline cube, no menu/ticker.
 * Configured resistance10 maps to native effective6, not resistance10. */
public final class BlockArcaneBoreBase extends BaseEntityBlock {
    public static final MapCodec<BlockArcaneBoreBase> CODEC = simpleCodec(BlockArcaneBoreBase::new);
    public static final DirectionProperty NOZZLE = BlockStateProperties.FACING;

    public BlockArcaneBoreBase(Properties properties) {
        super(properties.strength(2.5F, 6.0F).sound(SoundType.WOOD).noOcclusion());
        registerDefaultState(stateDefinition.any().setValue(NOZZLE, Direction.NORTH));
    }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(NOZZLE); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        // Original yaw quadrants0=NORTH,1=EAST,2=SOUTH,3=WEST, independently of clicked face.
        return defaultBlockState().setValue(NOZZLE, context.getHorizontalDirection().getOpposite());
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new TileArcaneBoreBase(pos, state); }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.ENTITYBLOCK_ANIMATED; }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return Shapes.block(); }
    @Override protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return Shapes.block(); }
    @Override protected VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) { return Shapes.empty(); }
    // Native wire connectivity consults isSignalSource, unlike the original
    // Forge canConnectRedstone hook. Connect, but NEVER generate either signal.
    @Override protected boolean isSignalSource(BlockState state) { return true; }
    @Override protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction side) { return 0; }
    @Override protected int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction side) { return 0; }
    @Override protected ItemInteractionResult useItemOn(ItemStack held, BlockState state, Level level, BlockPos pos,
                                                       Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(held.getItem() instanceof ItemWandCasting)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        return itemResult(tryWandInteraction(new UseOnContext(player, hand, hit)));
    }
    public static InteractionResult tryWandInteraction(UseOnContext context) {
        if (context.getPlayer() == null || !(context.getItemInHand().getItem() instanceof ItemWandCasting)) return InteractionResult.PASS;
        Level level = context.getLevel(); BlockPos pos = context.getClickedPos();
        if (!level.isInWorldBounds(pos) || !level.hasChunkAt(pos)
                || !(level.getBlockEntity(pos) instanceof TileArcaneBoreBase base)) return InteractionResult.PASS;
        return base.onWandUse(context); // Zero vis; root adds this to the existing wand useOn chain.
    }
    static ItemInteractionResult itemResult(InteractionResult result) {
        return switch (result) {
            case SUCCESS -> ItemInteractionResult.SUCCESS; case CONSUME -> ItemInteractionResult.CONSUME;
            case CONSUME_PARTIAL -> ItemInteractionResult.CONSUME_PARTIAL; case FAIL -> ItemInteractionResult.FAIL;
            default -> ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        };
    }
}
