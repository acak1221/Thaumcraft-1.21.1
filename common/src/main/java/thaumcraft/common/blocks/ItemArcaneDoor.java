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
        if (player == null) return InteractionResult.PASS;
        BlockPos lower = pos;
        if (!level.getBlockState(lower.below()).isFaceSturdy(level, lower.below(), Direction.UP)) return InteractionResult.PASS;
        if (!level.getBlockState(lower).isAir() || !level.getBlockState(lower.above()).isAir()) return InteractionResult.PASS;
        Direction facing = player.getDirection().getOpposite();
        boolean flipped = shouldFlip(level, lower, facing);
        placeDoor(level, lower, facing, flipped, player);
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
        Block door = ThaumcraftEldritchBlocks.ARCANE_DOOR.get();
        BlockState lower = door.defaultBlockState()
                .setValue(BlockArcaneDoor.FACING, facing)
                .setValue(BlockArcaneDoor.HALF, Half.BOTTOM)
                .setValue(BlockArcaneDoor.OPEN, Boolean.FALSE)
                .setValue(BlockArcaneDoor.FLIP, flipped);
        level.setBlock(pos, lower, Block.UPDATE_ALL);
        BlockState upper = lower.setValue(BlockArcaneDoor.HALF, Half.TOP);
        level.setBlock(pos.above(), upper, Block.UPDATE_ALL);
        if (level.getBlockEntity(pos) instanceof TileOwned owned) {
            owned.setOwner(player.getGameProfile().getName());
            owned.setChanged();
        }
        if (level.getBlockEntity(pos.above()) instanceof TileOwned owned) {
            owned.setOwner(player.getGameProfile().getName());
            owned.setChanged();
        }
    }
}
