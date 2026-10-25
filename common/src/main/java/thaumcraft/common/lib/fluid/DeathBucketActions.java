package thaumcraft.common.lib.fluid;

import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;
import thaumcraft.common.blocks.BlockFluidDeath;
import thaumcraft.common.config.ThaumcraftLegacyItems;
import thaumcraft.common.lib.utils.DeferredItemDrops;

/**
 * The original refill of the bucket of death (oracle
 * {@code thaumcraft/common/lib/events/EventHandlerWorld.java:212-223}, the {@code FillBucketEvent} handler).
 *
 * <p>The original posted the Forge {@code FillBucketEvent} for every empty bucket that was used, and the TC4
 * handler answered the death fluid: a cell whose original metadata is three (the source cell, the cell the
 * original bucket of death pours) was removed and the result was the bucket of death. The pure fluid answered
 * the same event with the bucket of the pure fluid.</p>
 *
 * <p>The modern native bucket has no such event: the empty bucket only knows the fluids whose {@code Fluid}
 * carries a bucket item. The death fluid has none (its {@code getBucket} is the air, exactly as in the
 * original), so the native pickup would remove the source cell and hand out nothing. This class restores the
 * original event, including its creative rule: in creative mode the original removed the cell but kept the
 * empty bucket and gave nothing away.</p>
 */
public final class DeathBucketActions {
    private static final Set<UUID> USING = new HashSet<>();
    private static final Map<ServerLevel, Set<BlockPos>> CELLS = new IdentityHashMap<>();
    private DeathBucketActions() {}

    private static boolean canonical(ServerLevel level) {
        return level.getServer().isSameThread() && level.getServer().getLevel(level.dimension()) == level;
    }
    private static boolean actor(ServerLevel level, Player player) {
        return canonical(level) && player instanceof ServerPlayer server
                && server.server.getPlayerList().getPlayer(player.getUUID()) == player && player.level() == level
                && !player.isRemoved() && player.isAlive() && !player.isSpectator();
    }
    private static boolean loaded(ServerLevel level, BlockPos pos) {
        return !level.isOutsideBuildHeight(pos) && level.getWorldBorder().isWithinBounds(pos) && level.hasChunkAt(pos);
    }
    private static boolean claim(ServerLevel level, BlockPos pos) {
        return CELLS.computeIfAbsent(level, ignored -> new HashSet<>()).add(pos.immutable());
    }
    private static void release(ServerLevel level, BlockPos pos) {
        var cells = CELLS.get(level);
        if (cells != null) { cells.remove(pos); if (cells.isEmpty()) CELLS.remove(level); }
    }

    /** Native BucketPickup shares the same source claim as the empty-bucket interaction. */
    public static ItemStack capture(@Nullable Player player, LevelAccessor world, BlockPos pos, BlockState expected) {
        if (!(world instanceof ServerLevel level) || !canonical(level) || !loaded(level, pos)
                || !BlockFluidDeath.isBucketCell(expected) || level.getBlockState(pos) != expected
                || player != null && !actor(level, player) || !claim(level, pos)) return ItemStack.EMPTY;
        try {
            if (level.getBlockState(pos) != expected) return ItemStack.EMPTY;
            boolean changed = level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            return changed && level.getBlockState(pos).isAir()
                    ? new ItemStack(ThaumcraftLegacyItems.BUCKET_DEATH.get()) : ItemStack.EMPTY;
        } finally { release(level, pos); }
    }

    /**
     * The original refill of the bucket of death.
     *
     * @return null when the interaction is none of the original's, so every other bucket keeps its own path
     */
    public static InteractionResultHolder<ItemStack> fill(Level world, Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (!held.is(Items.BUCKET)) return null;
        BlockHitResult hit = thaumcraft.common.items.ItemBucketDeath.fullHit(world, player);
        if (hit.getType() != HitResult.Type.BLOCK) return null;
        BlockPos pos = hit.getBlockPos();
        if (!world.hasChunkAt(pos)) return null;
        if (!BlockFluidDeath.isBucketCell(world.getBlockState(pos))) return null;
        if (player.isSpectator() || !world.mayInteract(player, pos) || !player.mayUseItemAt(pos, hit.getDirection(), held)) {
            return InteractionResultHolder.fail(held);
        }
        if (world.isClientSide) return InteractionResultHolder.success(held);
        if (!(world instanceof ServerLevel level) || !actor(level, player) || !USING.add(player.getUUID())) return InteractionResultHolder.fail(held);
        ItemStack expected = held.copy();
        try {
            if (!loaded(level, pos) || player.getItemInHand(hand) != held || !ItemStack.matches(held, expected)) return InteractionResultHolder.fail(held);
            // Reserve the physical input before native removal notifications can change the hand or reenter.
            boolean creative = player.getAbilities().instabuild;
            ItemStack paid = creative ? ItemStack.EMPTY : held.copyWithCount(1);
            ItemStack remainder = held;
            if (!creative) {
                remainder = held.copy(); remainder.shrink(1); held.shrink(1); player.setItemInHand(hand, remainder);
            }
            ItemStack filled = capture(player, level, pos, level.getBlockState(pos));
            if (filled.isEmpty()) {
                if (!paid.isEmpty()) credit(level, player, hand, remainder, paid);
                return InteractionResultHolder.fail(player.getItemInHand(hand));
            }
            // Original Forge ALLOW creative removes the source, keeps its empty bucket and produces no filled item.
            if (!creative) credit(level, player, hand, remainder, filled);
            return InteractionResultHolder.consume(player.getItemInHand(hand));
        } finally { USING.remove(player.getUUID()); }
    }

    private static void credit(ServerLevel level, Player player, InteractionHand hand, ItemStack remainder, ItemStack output) {
        if (remainder.isEmpty() && player.getItemInHand(hand) == remainder) { player.setItemInHand(hand, output); return; }
        if (!player.getInventory().add(output)) DeferredItemDrops.enqueueBucketReturn(level, player, output);
        player.getInventory().setChanged();
    }
}
