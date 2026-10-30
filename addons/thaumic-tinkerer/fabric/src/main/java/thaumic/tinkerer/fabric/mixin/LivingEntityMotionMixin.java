/**
 * This file is new in the Minecraft 1.21.1 port of Thaumic Tinkerer for Thaumcraft 4 Reload (no 1.7.10 counterpart, see
 * addons/thaumic-tinkerer/NOTICE.md). Thaumic Tinkerer was created by Vazkii; the port is distributed under a Creative
 * Commons Attribution-NonCommercial-ShareAlike 3.0 License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 */
package thaumic.tinkerer.fabric.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalFloatRef;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import thaumic.tinkerer.common.hooks.TinkererEvents;

/**
 * Posts {@link TinkererEvents#LIVING_JUMP} and {@link TinkererEvents#LIVING_FALL}.
 *
 * <p>The jump: {@code jumpFromGround} returns early when the jump power is zero (no jump, no event); the {@code TAIL}
 * injection matches only the last return, after the vertical motion and the sprint push were applied and
 * {@code hasImpulse} was set.</p>
 *
 * <p>The fall: the head of {@code LivingEntity.causeFallDamage}, before it calls {@code Entity.causeFallDamage} (which
 * passes the fall on to the passengers) and works the damage out from the two floats. The arguments are replaced by what the
 * listeners left in the {@code FallData}; a cancel returns {@code false}, as an entity that is immune to falls does.
 * Players pass through {@code Player.causeFallDamage} first (it awards the fall statistic and refuses it for flying
 * players) and reach this method through {@code super}.</p>
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMotionMixin {
    @Inject(method = "jumpFromGround", at = @At("TAIL"))
    private void thaumictinkerer$jumped(CallbackInfo ci) {
        TinkererEvents.postLivingJump((LivingEntity) (Object) this);
    }

    @Inject(method = "causeFallDamage", at = @At("HEAD"), cancellable = true)
    private void thaumictinkerer$fall(float fallDistance, float multiplier, DamageSource source,
            CallbackInfoReturnable<Boolean> cir, @Local(ordinal = 0, argsOnly = true) LocalFloatRef distance,
            @Local(ordinal = 1, argsOnly = true) LocalFloatRef damageMultiplier) {
        TinkererEvents.FallData fall = new TinkererEvents.FallData(distance.get(), damageMultiplier.get());
        if (TinkererEvents.postLivingFall((LivingEntity) (Object) this, fall)) {
            cir.setReturnValue(false);
            return;
        }
        distance.set(fall.getDistance());
        damageMultiplier.set(fall.getDamageMultiplier());
    }
}
