package thaumcraft.common.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
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
import thaumcraft.common.config.ThaumcraftHungryChest;
import thaumcraft.common.tiles.TileChestHungry;

/** Original standalone Hungry Chest: no double-chest state or blocked-lid rule. */
public final class BlockChestHungry extends BaseEntityBlock {
    public static final MapCodec<BlockChestHungry> CODEC = simpleCodec(BlockChestHungry::new);
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    private static final VoxelShape OUTLINE = box(1, 0, 1, 15, 14, 15);
    private static final VoxelShape COLLISION = box(1, 0, 1, 15, 15, 15);

    public BlockChestHungry(Properties properties) {
        super(properties.strength(2.5F).sound(SoundType.WOOD).noOcclusion());
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.DOWN));
    }

    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(FACING); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        var player = context.getPlayer();
        if (player == null) return defaultBlockState().setValue(FACING, context.getNearestLookingDirection().getOpposite());
        BlockPos pos = context.getClickedPos();
        // Original piston orientation: nearby height wins, otherwise yaw only.
        // The legacy posY + 1.82 - yOffset becomes modern feetY + 1.82.
        if (Math.abs((float) player.getX() - pos.getX()) < 2 && Math.abs((float) player.getZ() - pos.getZ()) < 2) {
            double height = player.getY() + 1.82D;
            if (height - pos.getY() > 2) return defaultBlockState().setValue(FACING, Direction.UP);
            if (pos.getY() - height > 0) return defaultBlockState().setValue(FACING, Direction.DOWN);
        }
        Direction facing = switch (Mth.floor((double) (player.getYRot() * 4.0F / 360.0F) + .5D) & 3) {
            case 0 -> Direction.NORTH;
            case 1 -> Direction.EAST;
            case 2 -> Direction.SOUTH;
            default -> Direction.WEST;
        };
        return defaultBlockState().setValue(FACING, facing);
    }
    @Override protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }
    @Override protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new TileChestHungry(pos, state); }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.ENTITYBLOCK_ANIMATED; }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return OUTLINE; }
    @Override protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return COLLISION; }
    @Override protected VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) { return Shapes.empty(); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ThaumcraftHungryChest.HUNGRY_CHEST_BLOCK_ENTITY.get(), TileChestHungry::tick);
    }

    @Override protected boolean hasAnalogOutputSignal(BlockState state) { return true; }
    @Override protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof TileChestHungry chest
                ? AbstractContainerMenu.getRedstoneSignalFromContainer(chest) : 0;
    }

    @Override protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        // BlockChestHungry:145-165: native contact only, never a radius query,
        // attraction, pickup-delay check or player/thrower ownership restriction.
        if (level instanceof ServerLevel && entity instanceof ItemEntity item && item.isAlive()
                && level.getBlockEntity(pos) instanceof TileChestHungry chest) chest.eatItem(item);
    }

    @Override protected ItemInteractionResult useItemOn(ItemStack held, BlockState state, Level level, BlockPos pos,
                                                       Player player, InteractionHand hand, BlockHitResult hit) {
        InteractionResult result = open(level, pos, player, hand, held, hit);
        if (result == InteractionResult.FAIL) return ItemInteractionResult.FAIL;
        return result.consumesAction() ? ItemInteractionResult.sidedSuccess(level.isClientSide)
                : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return open(level, pos, player, InteractionHand.MAIN_HAND, player.getMainHandItem(), hit);
    }
    private InteractionResult open(Level level, BlockPos pos, Player player, InteractionHand hand, ItemStack held, BlockHitResult hit) {
        if (!level.hasChunkAt(pos) || !(level.getBlockEntity(pos) instanceof TileChestHungry chest)) return InteractionResult.PASS;
        if (player.level() != level || chest.getLevel() != level || player.getItemInHand(hand) != held || !chest.canOpen(player)
                || !player.mayUseItemAt(pos, hit.getDirection(), held)) return InteractionResult.FAIL;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(level instanceof ServerLevel server) || !(player instanceof ServerPlayer sender)) return InteractionResult.FAIL;
        if (!server.getServer().isSameThread()) throw new IllegalStateException("Hungry Chest interaction requires the server thread");
        // Vanilla GENERIC_9x3 needs neither an extended payload nor a new menu type.
        sender.openMenu(chest);
        return InteractionResult.CONSUME;
    }

    @Override protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        if (!replacement.is(this) && level.getBlockEntity(pos) instanceof TileChestHungry chest) chest.evacuate();
        super.onRemove(state, level, pos, replacement, moving);
    }
    // BaseEntityBlock.triggerEvent already forwards both event id and parameter
    // to the current block entity; do not swallow the legacy lid/user events.
}
