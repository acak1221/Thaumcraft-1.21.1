package thaumcraft.common.lib.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
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
    private DeathBucketActions() {}

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
        if (!(world instanceof ServerLevel level)) return InteractionResultHolder.fail(held);
        // The original event removed the cell first and only then decided what the player gets.
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        // The Forge ALLOW branch of the original ItemBucket keeps the empty bucket in creative mode and
        // gives the result away only when the bucket was really spent.
        if (!player.getAbilities().instabuild) {
            ItemStack filled = new ItemStack(ThaumcraftLegacyItems.BUCKET_DEATH.get());
            held.shrink(1);
            if (held.isEmpty()) player.setItemInHand(hand, filled);
            else if (!player.getInventory().add(filled)) DeferredItemDrops.enqueueBucketReturn(level, player, filled);
            player.getInventory().setChanged();
        }
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }
}
