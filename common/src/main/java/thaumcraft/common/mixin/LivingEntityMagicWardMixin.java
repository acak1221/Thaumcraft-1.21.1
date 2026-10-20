package thaumcraft.common.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import thaumcraft.common.lib.MagicWard;

/**
 * The original's priority-1 pass of ISpecialArmor ran ahead of the ordinary armor calculation, on the damage as it came: the worn magic
 * wards come off first, then vanilla's own armor and magic absorption see what is left.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMagicWardMixin {
    @WrapMethod(method = "getDamageAfterArmorAbsorb(Lnet/minecraft/world/damagesource/DamageSource;F)F", require = 1, allow = 1)
    private float thaumcraft$magicWard(DamageSource source, float damage, Operation<Float> original) {
        return original.call(source, MagicWard.apply((LivingEntity) (Object) this, source, damage));
    }
}
