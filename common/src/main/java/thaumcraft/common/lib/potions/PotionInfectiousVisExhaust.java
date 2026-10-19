package thaumcraft.common.lib.potions;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import thaumcraft.common.config.ThaumcraftFlux;

/** Original PotionInfectiousVisExhaust:52-67; not an invented generic poison effect. */
public final class PotionInfectiousVisExhaust extends MobEffect {
    public PotionInfectiousVisExhaust() { super(MobEffectCategory.HARMFUL, 0x665577); }
    @Override public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) { return duration % 40 == 0; }
    @Override public boolean applyEffectTick(LivingEntity source, int amplifier) {
        if (!source.level().isClientSide) {
            for (LivingEntity target : source.level().getEntitiesOfClass(LivingEntity.class, source.getBoundingBox().inflate(4))) {
                if (target.hasEffect(ThaumcraftFlux.infectiousVisExhaust())) continue;
                target.addEffect(amplifier > 0
                        ? new MobEffectInstance(ThaumcraftFlux.infectiousVisExhaust(), 6000, amplifier - 1, false, true)
                        : new MobEffectInstance(ThaumcraftFlux.visExhaust(), 6000, 0, false, true));
            }
        }
        return true;
    }
}
