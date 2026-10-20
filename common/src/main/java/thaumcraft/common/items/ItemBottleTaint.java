package thaumcraft.common.items;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import thaumcraft.api.IScribeTools;
import thaumcraft.common.config.ThaumcraftInfusion;
import thaumcraft.common.config.ThaumcraftTaint;
import thaumcraft.common.lib.items.EquipmentRules;

/**
 * The Bottle of Taint (original ItemBottleTaint). Thrown at a spot, it poisons every living thing around it with taint and seeds the
 * ground with taint fibres.
 *
 * <p>The original throws an EntityBottleTaint, which is not part of this port. The bottle therefore bursts where the thrower looks
 * instead of where the flying bottle would have landed; the burst itself - the poison and the taint fibres - is the original's.
 */
public final class ItemBottleTaint extends Item implements IScribeTools {
    public ItemBottleTaint(Properties properties) {
        super(properties.stacksTo(EquipmentRules.BOTTLE_TAINT_MAX_STACK_SIZE));
    }

    /** Original ItemBottleTaint.func_77659_a: the bottle is thrown and breaks. */
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 0.5F,
                0.4F / (level.random.nextFloat() * 0.4F + 0.8F));
        if (!level.isClientSide) {
            burst(level, impact(level, player), player);
            if (!player.getAbilities().instabuild) stack.shrink(1);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    /** The spot the bottle bursts at: the block the thrower looks at, or the thrower's own position. */
    private static BlockPos impact(Level level, Player player) {
        BlockHitResult hit = level.clip(new ClipContext(player.getEyePosition(), player.getEyePosition().add(player.getLookAngle().scale(4.0D)),
                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        if (hit.getType() == HitResult.Type.BLOCK) return hit.getBlockPos();
        return player.blockPosition();
    }

    /** Original EntityBottleTaint.onUpdate: the taint of the broken bottle. */
    public static void burst(Level level, BlockPos pos, Entity source) {
        if (level.isClientSide) return;
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class,
                new net.minecraft.world.phys.AABB(pos).inflate(EquipmentRules.BOTTLE_TAINT_SPREAD),
                entity -> entity.isAlive() && !(entity instanceof Player && ((Player) entity).getAbilities().instabuild))) {
            if (living.hasEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ThaumcraftInfusion.FLUX_TAINT.get()))) continue;
            living.addEffect(new MobEffectInstance(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ThaumcraftInfusion.FLUX_TAINT.get()), EquipmentRules.BOTTLE_TAINT_POISON_TICKS, 0, false, true));
        }
        if (!(level instanceof ServerLevel server)) return;
        for (int i = 0; i < EquipmentRules.BOTTLE_TAINT_PATCHES; i++) {
            int x = pos.getX() + (int) ((server.random.nextFloat() - server.random.nextFloat()) * EquipmentRules.BOTTLE_TAINT_SPREAD);
            int z = pos.getZ() + (int) ((server.random.nextFloat() - server.random.nextFloat()) * EquipmentRules.BOTTLE_TAINT_SPREAD);
            BlockPos target = new BlockPos(x, pos.getY(), z);
            if (!level.isLoaded(target) || !level.getBlockState(target.below()).isSolidRender(level, target.below())) continue;
            if (!level.getBlockState(target).canBeReplaced()) continue;
            server.setBlock(target, ThaumcraftTaint.FIBRES.get().defaultBlockState(), net.minecraft.world.level.block.Block.UPDATE_ALL);
        }
    }
}
