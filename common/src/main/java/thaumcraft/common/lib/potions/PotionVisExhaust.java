package thaumcraft.common.lib.potions;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/** Original api.potions.PotionVisExhaust:27-49: passive; only wand pricing uses it. */
public final class PotionVisExhaust extends MobEffect {
    public PotionVisExhaust() { super(MobEffectCategory.HARMFUL, 0x664477); }
    @Override public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) { return false; }
}
