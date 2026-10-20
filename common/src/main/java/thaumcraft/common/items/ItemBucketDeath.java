package thaumcraft.common.items;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import thaumcraft.common.config.ThaumcraftLegacyTiles;
import thaumcraft.common.lib.items.EquipmentRules;

/**
 * The Bucket of Death (original ItemBucketDeath). Pouring it fills the spot with the fluid of death, the original's infusion fluid, and
 * leaves an empty bucket behind.
 *
 * <p>The fluid of death is the native BlockFluidDeath of this port, so the bucket holds its source block and pours it into the spot the
 * player looks at.
 */
public final class ItemBucketDeath extends Item {
    public ItemBucketDeath(Properties properties) {
        super(properties.stacksTo(EquipmentRules.BUCKET_DEATH_MAX_STACK_SIZE));
    }

    /** Original ItemBucketDeath.func_77659_a: the bucket is poured where the player looks. */
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        BlockHitResult hit = level.clip(new ClipContext(player.getEyePosition(), player.getEyePosition().add(player.getLookAngle().scale(5.0D)),
                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        if (hit.getType() != HitResult.Type.BLOCK) return InteractionResultHolder.pass(stack);
        // Original ItemBucketDeath.func_77659_a: the fluid goes into the block next to the one that was hit.
        BlockPos target = hit.getBlockPos().relative(hit.getDirection());
        if (!level.mayInteract(player, target)) return InteractionResultHolder.pass(stack);
        if (!pour(level, target)) return InteractionResultHolder.pass(stack);
        // Original ItemBucketDeath.func_77659_a: the bucket is empty afterwards, in creative mode it stays full.
        if (!player.getAbilities().instabuild) return InteractionResultHolder.success(new ItemStack(Items.BUCKET));
        return InteractionResultHolder.success(stack);
    }

    /**
     * Original ItemBucketDeath.tryPlaceContainedLiquid: the spot has to be empty or made of a material that is not solid, and the fluid
     * of death is placed there. The bucket holds the source block of the death fluid, so the poured fluid spreads like the original.
     */
    public static boolean pour(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        boolean air = state.isAir();
        boolean solid = state.isSolidRender(level, pos);
        boolean liquid = !state.getFluidState().isEmpty();
        if (!EquipmentRules.bucketDeathCanPour(air, solid, liquid)) return false;
        // Original ItemBucketDeath.tryPlaceContainedLiquid: a spot that is neither solid nor a liquid is emptied first.
        if (!level.isClientSide && EquipmentRules.bucketDeathClearsSpot(air, solid, liquid)) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
        level.setBlock(pos, ThaumcraftLegacyTiles.DEATH_FLUID.get().defaultBlockState(), Block.UPDATE_ALL);
        return true;
    }
}
