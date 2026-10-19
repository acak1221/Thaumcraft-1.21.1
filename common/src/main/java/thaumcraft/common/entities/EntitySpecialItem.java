package thaumcraft.common.entities;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;

/** TC4's floating, fire-resistant result entity, with native item persistence. */
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
        return !source.is(DamageTypeTags.IS_FIRE) && super.hurt(source, amount);
    }
}
