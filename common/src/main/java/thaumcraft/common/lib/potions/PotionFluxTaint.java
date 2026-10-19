package thaumcraft.common.lib.potions;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import thaumcraft.api.entities.ITaintedMob;

/** Only original PotionFluxTaint:52-64; no invented taint ecology or vanilla-poison substitute. */
public final class PotionFluxTaint extends MobEffect {
    public static final ResourceKey<DamageType> TAINT = ResourceKey.create(Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath("thaumcraft", "taint"));

    public PotionFluxTaint() { super(MobEffectCategory.HARMFUL, 0x663377); }

    @Override public boolean applyEffectTick(LivingEntity target, int amplifier) {
        if (target.level().isClientSide) return true;
        if (target instanceof ITaintedMob) {
            target.heal(1);
        } else if (!target.getType().is(EntityTypeTags.UNDEAD)) {
            // Both original non-undead branches deal one, including below one
            // heart and players. This is not vanilla poison's nonlethal floor.
            target.hurt(new DamageSource(target.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                    .getHolderOrThrow(TAINT)), 1);
        }
        return true;
    }

    @Override public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        int period = 40 >> amplifier;
        return period <= 0 || duration % period == 0;
    }
}
