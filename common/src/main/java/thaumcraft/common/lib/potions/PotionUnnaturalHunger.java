package thaumcraft.common.lib.potions;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/** Original hunger is exhaustion every tick, not the vanilla Hunger effect's cadence. */
public final class PotionUnnaturalHunger extends MobEffect {
    public PotionUnnaturalHunger() { super(MobEffectCategory.HARMFUL, 0x446633); }
    @Override public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) { return true; }
    @Override public boolean applyEffectTick(LivingEntity target, int amplifier) {
        if (!target.level().isClientSide && target instanceof Player player) player.causeFoodExhaustion(.025F * (amplifier + 1));
        return true;
    }
}
