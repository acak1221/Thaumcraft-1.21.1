package thaumcraft.common.entities;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;

/**
 * TC4's floating result entity, with native item persistence.
 *
 * <p>The original 4.2.3.5 {@code EntitySpecialItem} (oracle
 * {@code thaumcraft/common/entities/EntitySpecialItem.java:8-49}) is a 0.25 block item entity
 * whose {@code onUpdate} damps a rising motion by 0.9 and adds 0.04 before the vanilla item
 * gravity, so the item hovers exactly where it was created. Its
 * {@code attackEntityFrom} refuses every <em>explosion</em> ({@code DamageSource.isExplosion},
 * oracle line 45) and takes every other damage like a vanilla item entity.
 */
public final class EntitySpecialItem extends ItemEntity {
    public EntitySpecialItem(EntityType<? extends ItemEntity> type, Level level) { super(type, level); }

    @Override public void tick() {
        // Original EntitySpecialItem adds 0.04 before vanilla's 0.04 gravity.
        // Cancel the modern gravity rather than relying on its float precision.
        if (getDeltaMovement().y > 0) setDeltaMovement(getDeltaMovement().multiply(1, 0.9F, 1));
        super.tick();
    }

    @Override protected void applyGravity() {}

    @Override public boolean hurt(DamageSource source, float amount) {
        // The original refuses explosions only (oracle EntitySpecialItem.java:44-49);
        // it is not fire proof and it dies like any other item entity.
        return !source.is(DamageTypeTags.IS_EXPLOSION) && super.hurt(source, amount);
    }
}
