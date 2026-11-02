package thaumcraft.common.items.wands;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import thaumcraft.common.blocks.BlockArcaneDoor;
import thaumcraft.common.blocks.BlockArcanePressurePlate;
import thaumcraft.common.config.ThaumcraftEldritchBlocks;
import thaumcraft.common.config.ThaumcraftSettings;
import thaumcraft.common.lib.world.EldritchBlockRules;
import thaumcraft.common.lib.utils.DeferredItemDrops;
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
    private static final ThreadLocal<Boolean> reclaiming = new ThreadLocal<>();
    private WandWardedStone() {}

    /**
     * The removal itself. The original runs it on the server and only swings the arm on the client.
     *
     * @return {@link InteractionResult#SUCCESS} when a block was taken back, {@link InteractionResult#PASS}
     *         when the click was not a warded stone block at all
     */
    public static InteractionResult tryRemove(UseOnContext context) {
        if (Boolean.TRUE.equals(reclaiming.get())) return InteractionResult.FAIL;
        reclaiming.set(true);
        try {
            return removeClaimed(context);
        } finally {
            reclaiming.remove();
        }
    }

    private static InteractionResult removeClaimed(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide) return InteractionResult.PASS;
        if (!(level instanceof ServerLevel server)) return InteractionResult.PASS;
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
        var output = prepareOutput(server, pos, drop);
        if (output != null && !output.ready()) return InteractionResult.FAIL;
        // Original ItemWandCasting:392 and :402-405 - both halves of the door are marked removable.
        boolean wasSafe = owned != null && owned.safeToRemove;
        TileOwned other = otherHalf != null && level.getBlockEntity(otherHalf) instanceof TileOwned tile ? tile : null;
        boolean otherWasSafe = other != null && other.safeToRemove;
        if (owned != null) {
            owned.safeToRemove = true;
            owned.setChanged();
        }
        if (other != null) {
            other.safeToRemove = true;
            other.setChanged();
        }
        // Original ItemWandCasting:396 - the block itself is removed without its own drops.
        if (!level.destroyBlock(pos, false)) {
            if (owned != null && level.getBlockEntity(pos) == owned) owned.safeToRemove = wasSafe;
            if (other != null && level.getBlockEntity(otherHalf) == other) other.safeToRemove = otherWasSafe;
            return InteractionResult.FAIL;
        }
        // Only the removed physical block can own this output. Native ADD sees
        // neither a reusable source nor an unpaid, unretained item.
        if (output != null) output.commitRaw();
        level.levelEvent(2001, pos, Block.getId(state));
        if (output != null) DeferredItemDrops.tick(server);
        return InteractionResult.SUCCESS;
    }

    private static DeferredItemDrops.PreparedBatch prepareOutput(ServerLevel level, BlockPos pos, ItemStack drop) {
        if (drop.isEmpty()) return null;
        // Sample exactly the native Block.popResource position and item motion
        // before removal; retries retain those values rather than rolling again.
        double halfHeight = EntityType.ITEM.getHeight() / 2.0D;
        double x = pos.getX() + .5D + Mth.nextDouble(level.random, -.25D, .25D);
        double y = pos.getY() + .5D + Mth.nextDouble(level.random, -.25D, .25D) - halfHeight;
        double z = pos.getZ() + .5D + Mth.nextDouble(level.random, -.25D, .25D);
        if (!level.getGameRules().getBoolean(GameRules.RULE_DOBLOCKDROPS)) return null;
        ItemEntity sampled = new ItemEntity(level, x, y, z, drop);
        return DeferredItemDrops.prepareDirectedBatch(level, pos, java.util.List.of(
                new DeferredItemDrops.DirectedDrop(drop, sampled.position(), sampled.getDeltaMovement(), 10)));
    }
}
