package thaumcraft.common.lib.potions;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/** Original bright light can ignite; darkness can heal. Both rolls keep their original conditional ordering. */
public final class PotionSunScorned extends MobEffect {
    public PotionSunScorned() { super(MobEffectCategory.HARMFUL, 16308330); }
    @Override public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) { return duration % 40 == 0; }
    @Override public boolean applyEffectTick(LivingEntity target, int amplifier) {
        if (!target.level().isClientSide) {
            float light = target.getLightLevelDependentMagicValue();
            if (light > .5F && target.level().random.nextFloat() * 30F < (light - .4F) * 2F && target.level().canSeeSky(target.blockPosition())) target.igniteForSeconds(4);
            else if (light < .25F && target.level().random.nextFloat() > light * 2F) target.heal(1);
        }
        return true;
    }
}
