package thaumcraft.common.items;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import thaumcraft.common.blocks.BlockFluidDeath;
import thaumcraft.common.blocks.BlockFluidPure;
import thaumcraft.common.lib.items.EquipmentRules;

/**
 * The Bucket of Death (original ItemBucketDeath). Pouring it fills the spot with the fluid of death, the original's infusion fluid, and
 * leaves an empty bucket behind.
 *
 * <p>The fluid of death is the native BlockFluidDeath of this port, so the bucket holds its source block and pours it into the spot the
 * player looks at.
 */
public final class ItemBucketDeath extends Item {
    private static final ThreadLocal<Boolean> POURING = new ThreadLocal<>();
    public ItemBucketDeath(Properties properties) {
        super(properties.stacksTo(EquipmentRules.BUCKET_DEATH_MAX_STACK_SIZE));
    }

    /** Original ItemBucketDeath.func_77659_a: the bucket is poured where the player looks. */
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (POURING.get() != null || player.isSpectator() || player.level() != level || stack.isEmpty() || stack.getItem() != this)
            return InteractionResultHolder.fail(stack);
        BlockHitResult hit = fullHit(level, player);
        if (hit.getType() != HitResult.Type.BLOCK) return InteractionResultHolder.pass(stack);
        // Original ItemBucketDeath.func_77659_a: the fluid goes into the block next to the one that was hit.
        BlockPos target = hit.getBlockPos().relative(hit.getDirection());
        if (!loaded(level, target) || !level.mayInteract(player, target)
                || !player.mayUseItemAt(target, hit.getDirection(), stack)
                || !canPour(level.getBlockState(target))) return InteractionResultHolder.pass(stack);
        if (level.isClientSide) return InteractionResultHolder.success(stack);
        if (!(level instanceof ServerLevel server) || !(player instanceof ServerPlayer)
                || !server.getServer().isSameThread() || player.getItemInHand(hand) != stack) return InteractionResultHolder.fail(stack);
        POURING.set(true);
        try {
            // Debit before destroy/drop callbacks can reenter the same use.
            // Original admission consumes the bucket even if setBlock refuses.
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
                player.setItemInHand(hand, new ItemStack(Items.BUCKET));
            }
            pourClaimed(server, target);
            return InteractionResultHolder.consume(player.getItemInHand(hand));
        } finally { POURING.remove(); }
    }

    /**
     * Original ItemBucketDeath.tryPlaceContainedLiquid: the spot has to be empty or made of a material that is not solid, and the fluid
     * of death is placed there. The bucket holds the source block of the death fluid, so the poured fluid spreads like the original.
     */
    public static boolean pour(Level level, BlockPos pos) {
        if (POURING.get() != null || !(level instanceof ServerLevel server) || !server.getServer().isSameThread()
                || !loaded(level, pos) || !canPour(level.getBlockState(pos))) return false;
        POURING.set(true);
        try { pourClaimed(server, pos); return true; }
        finally { POURING.remove(); }
    }

    private static boolean loaded(Level level, BlockPos pos) {
        return level.isInWorldBounds(pos) && level.getWorldBorder().isWithinBounds(pos) && level.hasChunkAt(pos);
    }

    private static boolean canPour(BlockState state) {
        // Material.isSolid is independent of opacity/full-cube rendering.
        return EquipmentRules.bucketDeathCanPour(state.isAir(), BlockFluidPure.legacySolid(state), state.liquid());
    }

    private static void pourClaimed(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        // Original ItemBucketDeath.tryPlaceContainedLiquid: a spot that is neither solid nor a liquid is emptied first.
        if (EquipmentRules.bucketDeathClearsSpot(state.isAir(), BlockFluidPure.legacySolid(state), state.liquid())) {
            level.destroyBlock(pos, true);
        }
        // Original ItemBucketDeath:103 - the death fluid is poured with the metadata three, the cell that
        // still holds all four quanta. The original finite fluid keeps the quanta of a cell in its metadata,
        // so the metadata three IS the source cell, the native level zero.
        // A removal callback's replacement owns the cell; do not erase it.
        BlockState after = level.getBlockState(pos);
        if (loaded(level, pos) && (after == state || after.isAir()))
            level.setBlock(pos, BlockFluidDeath.bucketCellState(), Block.UPDATE_ALL);
    }

    /**
     * The original Item.rayTrace of the bucket: the source-only hit of the eye ray, the same hit the native
     * empty bucket uses.
     */
    public static BlockHitResult fullHit(Level level, Player player) {
        return getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
    }
}
