package thaumcraft.common.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.common.config.ThaumcraftEldritchBlocks;
import thaumcraft.common.lib.world.EldritchBlockRules;
import thaumcraft.common.tiles.TileOwned;

/**
 * Original BlockArcaneDoor: the two block arcane door. The original metadata
 * held the orientation in the lowest two bits, the open bit, the upper half
 * bit and the flip bit; those became block state properties.
 */
public final class BlockArcaneDoor extends BaseEntityBlock {
    public static final MapCodec<BlockArcaneDoor> CODEC = simpleCodec(BlockArcaneDoor::new);
    public static final DirectionProperty FACING = DirectionProperty.create("facing", Direction.Plane.HORIZONTAL);
    public static final BooleanProperty OPEN = BooleanProperty.create("open");
    public static final EnumProperty<Half> HALF = EnumProperty.create("half", Half.class);
    /** Original metadata bit sixteen, the mirrored hinge. */
    public static final BooleanProperty FLIP = BooleanProperty.create("flip");
    private static final VoxelShape SOUTH = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 3.0D);
    private static final VoxelShape NORTH = Block.box(0.0D, 0.0D, 13.0D, 16.0D, 16.0D, 16.0D);
    private static final VoxelShape WEST = Block.box(13.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D);
    private static final VoxelShape EAST = Block.box(0.0D, 0.0D, 0.0D, 3.0D, 16.0D, 16.0D);

    public BlockArcaneDoor(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH).setValue(OPEN, Boolean.FALSE)
                .setValue(HALF, Half.BOTTOM).setValue(FLIP, Boolean.FALSE));
    }

    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, OPEN, HALF, FLIP);
    }

    /** BlockArcaneDoor:340-357 - the door always carries the owned block entity. */
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileOwned(thaumcraft.common.config.ThaumcraftEldritchBlocks.OWNED_TILE.get(), pos, state);
    }

    /**
     * Original BlockArcaneDoor:68 - the door is unbreakable while the warded stone
     * option is on and has a hardness of fifteen otherwise. Minecraft 1.21.1 binds the
     * destroy speed to the block state, so the unbreakable state is a mining progress
     * of zero.
     */
    @Override protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        if (thaumcraft.common.config.ThaumcraftSettings.current().wardedStone()) return 0.0F;
        return super.getDestroyProgress(state, player, level, pos);
    }

    /** BlockArcaneDoor:104-152 - the closed door is a thin plate, the open one stands aside. */
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Direction facing = state.getValue(FACING);
        boolean open = state.getValue(OPEN);
        boolean flipped = state.getValue(FLIP);
        boolean upper = state.getValue(HALF) == Half.TOP;
        if (!open) {
            return switch (facing) {
                case SOUTH -> SOUTH;
                case WEST -> WEST;
                case NORTH -> NORTH;
                default -> EAST;
            };
        }
        VoxelShape hinge = flipped ? (upper ? EAST : WEST) : (upper ? WEST : EAST);
        return switch (facing) {
            case SOUTH -> hinge;
            case WEST -> flipped ? NORTH : SOUTH;
            case NORTH -> flipped ? WEST : EAST;
            default -> flipped ? SOUTH : NORTH;
        };
    }

    /** BlockArcaneDoor:256-274 - the owner decides whether the door opens. */
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof TileOwned door)) return InteractionResult.PASS;
        if (!EldritchBlockRules.doorAllows(door.getOwner(), door.getAccessList(), player.getGameProfile().getName())) {
            player.sendSystemMessage(Component.translatable("chat.thaumcraft.arcane_door.refuses"));
            level.playSound(null, pos, SoundEvents.IRON_DOOR_CLOSE, SoundSource.BLOCKS, .66F, 1.0F);
            return InteractionResult.CONSUME;
        }
        toggleDoor(level, pos, !state.getValue(OPEN));
        return InteractionResult.CONSUME;
    }

    /** BlockArcaneDoor:288-305 - the open bit is flipped on the lower half. */
    public static void toggleDoor(Level level, BlockPos pos, boolean open) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof BlockArcaneDoor)) return;
        BlockPos lower = state.getValue(HALF) == Half.TOP ? pos.below() : pos;
        BlockState lowerState = level.getBlockState(lower);
        if (!(lowerState.getBlock() instanceof BlockArcaneDoor) || lowerState.getValue(HALF) != Half.BOTTOM) return;
        if (lowerState.getValue(OPEN) == open) return;
        level.setBlock(lower, lowerState.setValue(OPEN, open), Block.UPDATE_ALL);
        level.setBlock(pos.above(), level.getBlockState(pos.above()).setValue(OPEN, open), Block.UPDATE_ALL);
        level.playSound(null, lower, open ? SoundEvents.IRON_DOOR_OPEN : SoundEvents.IRON_DOOR_CLOSE,
                SoundSource.BLOCKS, 1.0F, level.random.nextFloat() * .1F + .9F);
    }

    /** BlockArcaneDoor:339-357 - the upper half keeps the facing and the open bit of the lower half. */
    @Override protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, BlockPos neighbourPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighbor, neighbourPos, movedByPiston);
        if (state.getValue(HALF) == Half.TOP) {
            if (!(level.getBlockState(pos.below()).getBlock() instanceof BlockArcaneDoor)) {
                level.destroyBlock(pos, false);
            }
            return;
        }
        if (!(level.getBlockState(pos.above()).getBlock() instanceof BlockArcaneDoor)) {
            level.destroyBlock(pos, false);
        }
    }
}
