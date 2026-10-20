package thaumcraft.common.lib.potions;

import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import thaumcraft.api.damagesource.DamageSourceThaumcraft;
import thaumcraft.api.entities.ITaintedMob;

/** Only original PotionFluxTaint:52-64; no invented taint ecology or vanilla-poison substitute. */
public final class PotionFluxTaint extends MobEffect {
    /** The original taint damage source (oracle DamageSourceThaumcraft:19), published by the public API. */
    public static final ResourceKey<DamageType> TAINT = DamageSourceThaumcraft.TAINT;

    public PotionFluxTaint() { super(MobEffectCategory.HARMFUL, 0x663377); }

    @Override public boolean applyEffectTick(LivingEntity target, int amplifier) {
        if (target.level().isClientSide) return true;
        if (target instanceof ITaintedMob) {
            target.heal(1);
        } else if (!target.getType().is(EntityTypeTags.UNDEAD)) {
            // Both original non-undead branches deal one, including below one
            // heart and players. This is not vanilla poison's nonlethal floor.
            // Oracle PotionFluxTaint:56/:58 - the damage source is the published taint source.
            target.hurt(DamageSourceThaumcraft.taint(target.level()), 1);
        }
        return true;
    }

    @Override public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        int period = 40 >> amplifier;
        return period <= 0 || duration % period == 0;
    }
}
