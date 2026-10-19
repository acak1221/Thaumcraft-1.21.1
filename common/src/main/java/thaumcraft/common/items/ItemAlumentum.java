package thaumcraft.common.items;

import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileItem;
import net.minecraft.world.level.Level;
import thaumcraft.common.entities.projectile.EntityAlumentum;

/** TC4 ItemResource:251-259, plus its original dedicated dispenser behavior. */
public final class ItemAlumentum extends Item implements ProjectileItem {
    public ItemAlumentum(Properties properties) { super(properties); }

    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.isEmpty()) return InteractionResultHolder.fail(stack);
        if (!level.isClientSide) {
            var projectile = new EntityAlumentum(level, player);
            projectile.setItem(stack);
            if (!level.addFreshEntity(projectile)) return InteractionResultHolder.fail(stack);
            if (!player.getAbilities().instabuild) stack.shrink(1);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ARROW_SHOOT,
                    SoundSource.PLAYERS, .3F, .4F / (level.random.nextFloat() * .4F + .8F));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override public Projectile asProjectile(Level level, Position position, ItemStack stack, Direction direction) {
        var projectile = new EntityAlumentum(level, position.x(), position.y(), position.z());
        projectile.setItem(stack);
        return projectile;
    }
    @Override public DispenseConfig createDispenseConfig() {
        // Original BehaviorProjectileDispense defaults: power1.1, uncertainty6,
        // direction.y+0.1; event1009 is the original Alumentum launch sound.
        return DispenseConfig.builder().power(1.1F).uncertainty(6).overrideDispenseEvent(1009).build();
    }
    @Override public void shoot(Projectile projectile, double x, double y, double z, float power, float uncertainty) {
        // Modern ProjectileItem's default no longer adds this old dispenser lift.
        projectile.shoot(x, y + .1, z, power, uncertainty);
    }
}
