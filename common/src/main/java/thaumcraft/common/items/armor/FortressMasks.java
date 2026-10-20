package thaumcraft.common.items.armor;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * The combat effects of the Fortress helm's masks (original EventHandlerRunic.entityHurt, on the raw damage of a blow that will land):
 * Sipping Fiend heals the player who dealt the blow by one point with chance damage / 12; Angry Ghost withers, for 80 ticks, whoever
 * struck its wearer in melee (the direct source must be a living entity, so an arrow does not) with chance damage / 10. The Grinning
 * Devil's effect (less severe warp events) belongs to the warp events, which are not ported.
 */
public final class FortressMasks {
    private FortressMasks() {}

    public static void onHurt(LivingEntity victim, DamageSource source, float damage) {
        if (victim.level().isClientSide || damage <= 0) return;
        if (source.getDirectEntity() instanceof Player leecher
                && ItemFortressArmor.mask(leecher.getItemBySlot(EquipmentSlot.HEAD)) == FortressRules.MASK_FIEND
                && FortressRules.triggers(leecher.getRandom().nextFloat(), damage, FortressRules.FIEND_DIVISOR)) {
            leecher.heal(1.0F);
        }
        if (victim instanceof Player player && source.getDirectEntity() instanceof LivingEntity attacker
                && ItemFortressArmor.mask(player.getItemBySlot(EquipmentSlot.HEAD)) == FortressRules.MASK_GHOST
                && FortressRules.triggers(player.getRandom().nextFloat(), damage, FortressRules.GHOST_DIVISOR)) {
            attacker.addEffect(new MobEffectInstance(MobEffects.WITHER, FortressRules.WITHER_TICKS));
        }
    }
}
