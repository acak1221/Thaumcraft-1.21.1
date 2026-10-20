package thaumcraft.common.mixin;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** The 1.9+ attack cooldown counter, so a sweep of full-strength blows does not spend the cooldown of the blow that follows. */
@Mixin(LivingEntity.class)
public interface LivingEntityAttackStrengthAccess {
    @Accessor("attackStrengthTicker")
    int thaumcraft$getAttackStrengthTicker();

    @Accessor("attackStrengthTicker")
    void thaumcraft$setAttackStrengthTicker(int ticks);
}
