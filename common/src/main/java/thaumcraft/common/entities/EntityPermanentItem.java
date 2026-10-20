package thaumcraft.common.entities;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import thaumcraft.common.config.ThaumcraftProjectiles;
import thaumcraft.common.lib.entities.ProjectileRules;

/**
 * TC4 EntityPermanentItem, the item of the eldritch pedestal rooms.
 * <p>
 * The original is an {@code EntitySpecialItem} that never dies of old age: its
 * age counter is restarted five ticks before the vanilla item lifetime, so the
 * item stays in the world forever. It keeps the special item buoyancy, the
 * fire immunity and the random launch of the special item.
 * <p>
 * Declared native adaptations: the native item entity keeps its age private
 * and its lifetime is a fixed constant, so the endless lifetime is expressed
 * with the native unlimited lifetime flag instead of restarting the age; the
 * native special item is final, so the buoyancy, the fire immunity and the
 * 0.25 block size are repeated here.
 */
public final class EntityPermanentItem extends ItemEntity {
    public EntityPermanentItem(EntityType<? extends EntityPermanentItem> type, Level level) {
        super(type, level);
        setUnlimitedLifetime();
    }

    public EntityPermanentItem(Level level) {
        this(ThaumcraftProjectiles.PERMANENT_ITEM.get(), level);
    }

    public EntityPermanentItem(Level level, double x, double y, double z) {
        this(level);
        setPos(x, y, z);
    }

    public EntityPermanentItem(Level level, double x, double y, double z, ItemStack stack) {
        this(level, x, y, z);
        setItem(stack);
        setYRot((float) (Math.random() * 360D));
        setDeltaMovement((float) (Math.random() * .2D - .1D), .2F, (float) (Math.random() * .2D - .1D));
    }

    @Override public void tick() {
        // The original restarts its age five ticks before the vanilla lifetime, so the item never dies.
        if (getAge() != Short.MIN_VALUE && ProjectileRules.itemNeverDespawns(getAge())) setUnlimitedLifetime();
        // Original EntitySpecialItem buoyancy: damp the rise and cancel the item gravity.
        if (getDeltaMovement().y > 0) setDeltaMovement(getDeltaMovement().multiply(1, 0.9F, 1));
        super.tick();
    }

    @Override public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        // The saved age is restored by the native item entity; the original item is endless again.
        setUnlimitedLifetime();
    }

    @Override protected void applyGravity() {}

    @Override public boolean hurt(DamageSource source, float amount) {
        return !source.is(DamageTypeTags.IS_FIRE) && super.hurt(source, amount);
    }
}
