package thaumcraft.common.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import java.util.List;
import thaumcraft.common.config.ThaumcraftLegacyTiles;
import thaumcraft.common.tiles.TileWandPedestal;

/**
 * Original BlockStoneDevice metadata 5 and 8: the wand pedestal and the focus block above it.
 * <p>
 * The pedestal is a single block with three steps. It holds one wand or vis amulet and fills it from the
 * nodes around it. The focus is the block that may be put on top of the pedestal.
 * <p>
 * Oracle: {@code thaumcraft/common/blocks/BlockStoneDevice.java} of the legacy port and
 * {@code thaumcraft/common/tiles/TileWandPedestal.java}, lines 46-398.
 */
public final class BlockWandPedestal extends BaseEntityBlock {
    public static final MapCodec<BlockWandPedestal> CODEC = simpleCodec(BlockWandPedestal::new);
    /** Whether the block is the focus that sits on top of a pedestal. */
    public static final BooleanProperty FOCUS = BooleanProperty.create("focus");
    /** The three steps of the pedestal. */
    private static final VoxelShape PEDESTAL_SHAPE = Shapes.or(
            Shapes.box(0, 0, 0, 16, 4, 16),
            Shapes.box(2, 4, 2, 14, 8, 14),
            Shapes.box(4, 8, 4, 12, 16, 12));

    public BlockWandPedestal(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FOCUS, false));
    }

    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FOCUS);
    }

    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        // The focus of the original has no tile entity.
        if (state.getValue(FOCUS)) return null;
        return new TileWandPedestal(pos, state);
    }

    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (state.getValue(FOCUS)) return null;
        return createTickerHelper(type, ThaumcraftLegacyTiles.WAND_PEDESTAL_BLOCK_ENTITY.get(), TileWandPedestal::tick);
    }

    /** Whether the block is the focus that sits on top of a pedestal. */
    public boolean isFocus() { return defaultBlockState().getValue(FOCUS); }

    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(FOCUS) ? Shapes.block() : PEDESTAL_SHAPE;
    }
    @Override protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    /**
     * The pedestal takes the wand out of the player's hand and gives it back.
     */
    @Override protected ItemInteractionResult useItemOn(ItemStack held, BlockState state, Level level, BlockPos pos,
                                                       Player player, InteractionHand hand, BlockHitResult hit) {
        if (state.getValue(FOCUS)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!(level.getBlockEntity(pos) instanceof TileWandPedestal pedestal)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!player.canInteractWithBlock(pos, 0) || !level.mayInteract(player, pos)) return ItemInteractionResult.FAIL;
        if (level.isClientSide) return ItemInteractionResult.SUCCESS;
        if (!pedestal.isEmpty()) {
            ItemStack stored = pedestal.removeItemNoUpdate(0);
            if (stored.isEmpty()) return ItemInteractionResult.FAIL;
            if (!player.getInventory().add(stored)) player.drop(stored, false);
            pedestal.setChanged();
            return ItemInteractionResult.CONSUME;
        }
        if (held.isEmpty() || !pedestal.canPlaceItem(0, held)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        ItemStack stored = held.copyWithCount(1);
        held.shrink(1);
        if (held.isEmpty()) player.setItemInHand(hand, ItemStack.EMPTY);
        pedestal.setItem(0, stored);
        player.getInventory().setChanged();
        return ItemInteractionResult.CONSUME;
    }

    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (state.getValue(FOCUS)) return InteractionResult.PASS;
        return useItemOn(player.getMainHandItem(), state, level, pos, player, InteractionHand.MAIN_HAND, hit).consumesAction()
                ? InteractionResult.CONSUME : InteractionResult.PASS;
    }

    /** The pedestal drops its wand when it is broken. */
    @Override protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        if (!state.getValue(FOCUS) && !replacement.is(this) && level instanceof net.minecraft.server.level.ServerLevel server
                && level.getBlockEntity(pos) instanceof TileWandPedestal pedestal) {
            ItemStack stack = pedestal.removeItemNoUpdate(0);
            if (!stack.isEmpty()) thaumcraft.common.lib.utils.DeferredItemDrops.enqueue(server, pos, List.of(stack));
        }
        super.onRemove(state, level, pos, replacement, moving);
    }
}
