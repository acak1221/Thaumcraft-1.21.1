package thaumcraft.common.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import thaumcraft.common.config.ThaumcraftEldritchBlocks;
import thaumcraft.common.tiles.TileOwned;

/**
 * Original ItemArcaneDoor: the arcane door item. It places both halves and
 * records the owner of the door.
 */
public final class ItemArcaneDoor extends Item {
    public ItemArcaneDoor(Properties properties) {
        super(properties.stacksTo(1));
    }

    /** ItemArcaneDoor:38-56 - the item places the two halves of the door. */
    @Override public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        if (player == null || player.isSpectator() || context.getClickedFace() != Direction.UP) return InteractionResult.PASS;
        // Oracle ItemArcaneDoor.onItemUse increments the clicked ground's Y.
        // This is a plain Item: BlockPlaceContext never offsets it for us.
        BlockPos lower = pos.above();
        BlockPos upper = lower.above();
        if (!level.isInWorldBounds(lower) || !level.isInWorldBounds(upper)
                || !level.hasChunkAt(lower) || !level.hasChunkAt(upper)
                || !level.getWorldBorder().isWithinBounds(lower) || !level.getWorldBorder().isWithinBounds(upper)
                || !level.mayInteract(player, lower) || !level.mayInteract(player, upper)
                || !player.mayUseItemAt(lower, Direction.UP, stack) || !player.mayUseItemAt(upper, Direction.UP, stack)) return InteractionResult.FAIL;
        if (!level.getBlockState(lower.below()).isFaceSturdy(level, lower.below(), Direction.UP)) return InteractionResult.PASS;
        if (!level.getBlockState(lower).isAir() || !level.getBlockState(lower.above()).isAir()) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        Direction facing = player.getDirection().getOpposite();
        boolean flipped = shouldFlip(level, lower, facing);
        if (!placeOwnedDoor(level, lower, facing, flipped, player)) return InteractionResult.FAIL;
        if (!player.getAbilities().instabuild) stack.shrink(1);
        return InteractionResult.CONSUME;
    }

    /** ItemArcaneDoor:57-83 - the hinge side follows the neighbouring doors. */
    private static boolean shouldFlip(Level level, BlockPos pos, Direction facing) {
        Direction left = facing.getCounterClockWise();
        Direction right = facing.getClockWise();
        int leftCount = 0;
        int rightCount = 0;
        for (int y = 0; y < 2; y++) {
            if (level.getBlockState(pos.relative(left).above(y)).getBlock() instanceof BlockArcaneDoor) leftCount++;
            if (level.getBlockState(pos.relative(right).above(y)).getBlock() instanceof BlockArcaneDoor) rightCount++;
        }
        if (leftCount > 0 && rightCount == 0) return true;
        return rightCount > leftCount;
    }

    /** ItemArcaneDoor:57-83 - the two halves are placed with the owner. */
    public static void placeDoor(Level level, BlockPos pos, Direction facing, boolean flipped, Player player) {
        placeOwnedDoor(level, pos, facing, flipped, player);
    }

    private static boolean placeOwnedDoor(Level level, BlockPos pos, Direction facing, boolean flipped, Player player) {
        Block door = ThaumcraftEldritchBlocks.ARCANE_DOOR.get();
        BlockState lower = door.defaultBlockState()
                .setValue(BlockArcaneDoor.FACING, facing)
                .setValue(BlockArcaneDoor.HALF, Half.BOTTOM)
                .setValue(BlockArcaneDoor.OPEN, Boolean.FALSE)
                .setValue(BlockArcaneDoor.FLIP, flipped);
        BlockState upper = lower.setValue(BlockArcaneDoor.HALF, Half.TOP);
        // As in the original, publish neighbours only after both halves and
        // their ownership exist. A failed write must not consume the item.
        if (!level.setBlock(pos, lower, Block.UPDATE_CLIENTS)) return false;
        if (!level.getBlockState(pos.above()).isAir() || !level.setBlock(pos.above(), upper, Block.UPDATE_CLIENTS)) {
            if (level.getBlockState(pos) == lower) level.removeBlock(pos, false);
            return false;
        }
        if (level.getBlockEntity(pos) instanceof TileOwned owned) {
            owned.setOwner(player.getGameProfile().getName());
            owned.setChanged();
        }
        if (level.getBlockEntity(pos.above()) instanceof TileOwned owned) {
            owned.setOwner(player.getGameProfile().getName());
            owned.setChanged();
        }
        level.sendBlockUpdated(pos, lower, lower, Block.UPDATE_CLIENTS);
        level.sendBlockUpdated(pos.above(), upper, upper, Block.UPDATE_CLIENTS);
        level.updateNeighborsAt(pos, door);
        level.updateNeighborsAt(pos.above(), door);
        return true;
    }
}
