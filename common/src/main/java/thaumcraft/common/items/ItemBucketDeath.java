package thaumcraft.common.items;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import thaumcraft.common.lib.items.EquipmentRules;

/**
 * The Bucket of Death (original ItemBucketDeath). Pouring it fills the spot with the fluid of death, the original's infusion fluid, and
 * leaves an empty bucket behind.
 *
 * <p>The fluid of death itself, a block and a fluid of its own, is not part of this port; the bucket looks it up by its registry name and
 * keeps its contents when the fluid is missing.
 */
public final class ItemBucketDeath extends Item {
    /** The registry names the fluid of death may carry. */
    private static final String[] DEATH_FLUID_IDS = {"thaumcraft:death_fluid", "thaumcraft:block_fluid_death", "thaumcraft:fluid_death"};

    public ItemBucketDeath(Properties properties) {
        super(properties.stacksTo(EquipmentRules.BUCKET_DEATH_MAX_STACK_SIZE));
    }

    /** Original ItemBucketDeath.func_77659_a: the bucket is poured where the player looks. */
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        BlockHitResult hit = level.clip(new ClipContext(player.getEyePosition(), player.getEyePosition().add(player.getLookAngle().scale(5.0D)),
                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        if (hit.getType() != HitResult.Type.BLOCK) return InteractionResultHolder.pass(stack);
        BlockPos target = hit.getBlockPos().relative(hit.getDirection());
        if (!level.mayInteract(player, target)) return InteractionResultHolder.pass(stack);
        if (!pour(level, target)) return InteractionResultHolder.pass(stack);
        if (!player.getAbilities().instabuild) return InteractionResultHolder.success(new ItemStack(Items.BUCKET));
        return InteractionResultHolder.success(stack);
    }

    /**
     * Original ItemBucketDeath.tryPlaceContainedLiquid: the spot has to be empty or made of a material that is not solid, and the fluid
     * of death is placed there.
     */
    public static boolean pour(Level level, BlockPos pos) {
        var state = level.getBlockState(pos);
        boolean air = state.isAir();
        boolean solid = state.isSolidRender(level, pos);
        if (!EquipmentRules.bucketDeathCanPour(air, solid, !state.getFluidState().isEmpty())) return false;
        var fluid = deathFluid();
        if (fluid == null) return false;
        level.setBlock(pos, fluid.defaultBlockState(), 3);
        return true;
    }

    private static net.minecraft.world.level.block.Block deathFluid() {
        for (String id : DEATH_FLUID_IDS) {
            var block = BuiltInRegistries.BLOCK.getOptional(ResourceLocation.parse(id));
            if (block.isPresent()) return block.get();
        }
        return null;
    }
}
