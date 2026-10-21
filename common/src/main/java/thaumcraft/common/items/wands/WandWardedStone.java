package thaumcraft.common.items.wands;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import thaumcraft.common.blocks.BlockArcaneDoor;
import thaumcraft.common.blocks.BlockArcanePressurePlate;
import thaumcraft.common.config.ThaumcraftEldritchBlocks;
import thaumcraft.common.config.ThaumcraftSettings;
import thaumcraft.common.lib.world.EldritchBlockRules;
import thaumcraft.common.tiles.TileOwned;

/**
 * Original {@code thaumcraft/common/items/wands/ItemWandCasting.java:390-415}: a wand click takes the
 * warded stone blocks back and gives them to the player as an item.
 *
 * <p>While the "warded stone" option is on, the arcane pressure plate, the warded glass and the arcane
 * door have no mining progress at all (original {@code BlockWoodenDevice:197},
 * {@code BlockCosmeticOpaque:256} and {@code BlockArcaneDoor:68}), so this wand click is the only way to
 * move them again. The rules themselves live in {@link WandWardedStoneRules}.</p>
 */
public final class WandWardedStone {
    private WandWardedStone() {}

    /**
     * The removal itself. The original runs it on the server and only swings the arm on the client.
     *
     * @return {@link InteractionResult#SUCCESS} when a block was taken back, {@link InteractionResult#PASS}
     *         when the click was not a warded stone block at all
     */
    public static InteractionResult tryRemove(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide) return InteractionResult.PASS;
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;
        BlockPos pos = context.getClickedPos();
        if (!level.hasChunkAt(pos)) return InteractionResult.PASS;
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();
        boolean wardedStone = ThaumcraftSettings.current().wardedStone();
        TileOwned owned = level.getBlockEntity(pos) instanceof TileOwned tile ? tile : null;
        String owner = owned == null ? "" : owned.getOwner();
        String name = player.getGameProfile().getName();
        ItemStack drop = ItemStack.EMPTY;
        BlockPos otherHalf = null;
        if (block == ThaumcraftEldritchBlocks.ARCANE_PRESSURE_PLATE.get()
                && BlockArcanePressurePlate.variant(state) == EldritchBlockRules.DEVICE_WOOD_PRESSURE_PLATE) {
            // Original ItemWandCasting:390 - the wooden device metadata two, the arcane pressure plate.
            if (!WandWardedStoneRules.mayRemove(wardedStone, owner, name)) return InteractionResult.PASS;
            drop = new ItemStack(ThaumcraftEldritchBlocks.ARCANE_PRESSURE_PLATE_ITEM.get());
        } else if (block == ThaumcraftEldritchBlocks.COSMETIC_OPAQUE_WARDED.get()) {
            // Original ItemWandCasting:390 - the cosmetic opaque metadata two, the warded glass.
            if (!WandWardedStoneRules.mayRemove(wardedStone, owner, name)) return InteractionResult.PASS;
            drop = new ItemStack(ThaumcraftEldritchBlocks.WARDED_GLASS_ITEM.get());
        } else if (block == ThaumcraftEldritchBlocks.ARCANE_DOOR.get()) {
            // Original ItemWandCasting:400-415 - the arcane door and its other half.
            if (!WandWardedStoneRules.mayRemove(wardedStone, owner, name)) return InteractionResult.PASS;
            boolean upper = state.getValue(BlockArcaneDoor.HALF) == Half.TOP;
            otherHalf = upper ? pos.below() : pos.above();
            if (WandWardedStoneRules.dropsDoorItem(wardedStone, upper)) {
                drop = new ItemStack(ThaumcraftEldritchBlocks.ARCANE_DOOR_ITEM.get());
            }
        } else {
            return InteractionResult.PASS;
        }
        // Original ItemWandCasting:392 and :402-405 - both halves of the door are marked removable.
        if (owned != null) {
            owned.safeToRemove = true;
            owned.setChanged();
        }
        if (otherHalf != null && level.getBlockEntity(otherHalf) instanceof TileOwned other) {
            other.safeToRemove = true;
            other.setChanged();
        }
        // Original ItemWandCasting:393-395 - the item appears at the centre of the block.
        if (!drop.isEmpty()) Block.popResource(level, pos, drop);
        level.levelEvent(2001, pos, Block.getId(state));
        // Original ItemWandCasting:396 - the block itself is removed without its own drops.
        level.destroyBlock(pos, false);
        return InteractionResult.SUCCESS;
    }
}
