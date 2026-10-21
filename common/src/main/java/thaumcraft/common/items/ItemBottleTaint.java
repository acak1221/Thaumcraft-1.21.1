package thaumcraft.common.items;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import thaumcraft.api.IScribeTools;
import thaumcraft.common.entities.projectile.EntityBottleTaint;
import thaumcraft.common.lib.items.EquipmentRules;

/**
 * The Bottle of Taint (original ItemBottleTaint). Thrown at a spot, it poisons every living thing around it with taint and seeds the
 * ground with taint fibres.
 *
 * <p>The original throws an EntityBottleTaint, which breaks where it lands and taints that spot; the bottle itself is spent by the
 * throw.
 */
public final class ItemBottleTaint extends Item implements IScribeTools {
    public ItemBottleTaint(Properties properties) {
        super(properties.stacksTo(EquipmentRules.BOTTLE_TAINT_MAX_STACK_SIZE));
    }

    /** Original ItemBottleTaint.func_77659_a: the bottle is thrown and breaks. */
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        // Original ItemBottleTaint.func_77659_a: the throw has its own sound.
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 0.5F,
                0.4F / (level.random.nextFloat() * 0.4F + 0.8F));
        if (!level.isClientSide) {
            // Original ItemBottleTaint.func_77659_a: the bottle is thrown as an EntityBottleTaint.
            level.addFreshEntity(new EntityBottleTaint(level, player));
            if (!player.getAbilities().instabuild) stack.shrink(1);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    /**
     * The original burst of the bottle at a spot: the taint splash and the taint fibres
     * of the spot where the thrown bottle broke.
     */
    public static void burst(ServerLevel level, BlockPos pos, ServerPlayer player) {
        EntityBottleTaint bottle = new EntityBottleTaint(level, player);
        bottle.setPos(pos.getX() + .5D, pos.getY(), pos.getZ() + .5D);
        bottle.burst(level);
    }
}
