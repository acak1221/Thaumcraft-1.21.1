package thaumcraft.common.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import thaumcraft.common.items.armor.FortressMasks;
import thaumcraft.common.lib.MagicWard;

/**
 * The original's priority-1 pass of ISpecialArmor ran ahead of the ordinary armor calculation, on the damage as it came: the worn magic
 * wards come off first, then vanilla's own armor and magic absorption see what is left. The same hook is where Forge's LivingHurtEvent
 * stood for the Fortress masks (the raw damage, once the blow is certain to land), and the armor value vanilla's formula is given follows
 * the original's split between the two passes.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMagicWardMixin {
    @WrapMethod(method = "getDamageAfterArmorAbsorb(Lnet/minecraft/world/damagesource/DamageSource;F)F", require = 1, allow = 1)
    private float thaumcraft$magicWard(DamageSource source, float damage, Operation<Float> original) {
        FortressMasks.onHurt((LivingEntity) (Object) this, source, damage);
        float remaining = thaumcraft.common.lib.runic.RunicShield.onHurt((LivingEntity) (Object) this, source, damage);
        return original.call(source, MagicWard.apply((LivingEntity) (Object) this, source, remaining));
    }

    @WrapOperation(method = "getDamageAfterArmorAbsorb(Lnet/minecraft/world/damagesource/DamageSource;F)F", require = 1, allow = 1,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/damagesource/CombatRules;getDamageAfterAbsorb(Lnet/minecraft/world/entity/LivingEntity;FLnet/minecraft/world/damagesource/DamageSource;FF)F"))
    private float thaumcraft$armorValue(LivingEntity entity, float damage, DamageSource source, float armor, float toughness, Operation<Float> original) {
        return original.call(entity, damage, source, Math.max(0F, armor + MagicWard.armorDelta(entity, source)), toughness);
    }
}
