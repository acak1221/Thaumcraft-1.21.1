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
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import thaumic.tinkerer.common.hooks.TinkererEvents;

/**
 * Posts {@link TinkererEvents#LIVING_DAMAGE}. {@code LivingEntity.hurt} first returns {@code false} for everything the
 * entity ignores (invulnerable to the source, client side, already dying, fire against Fire Resistance); the first thing
 * it does after those four checks is {@code this.isSleeping()}, and the injection sits right before that call. At that point
 * the amount is what the entity was hurt with, still whole: nothing has blocked, scaled or compared it yet. A listener's
 * result replaces the argument (the later code reads the same slot), and a result of zero returns {@code false} from
 * {@code hurt} as the first checks do, so there is no damage, no hurt animation, no knockback and no invulnerability frame.
 *
 * <p>A hit that the invulnerability frames will refuse anyway (frames running, the source not bypassing the cooldown, the
 * amount not above {@code lastHurt}) is not posted: {@link TinkererEvents#framesRefuse} is vanilla's own test, read before the
 * event, so a listener with a side effect is not charged for a hit the game would throw away.</p>
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityHurtMixin {
    @Shadow
    protected float lastHurt;

    @Inject(method = "hurt", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;isSleeping()Z"),
            cancellable = true)
    private void thaumictinkerer$livingDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir,
            @Local(argsOnly = true) LocalFloatRef damage) {
        float incoming = damage.get();
        if (!(incoming > 0.0F)) return; // a hit of no damage (a snowball) has nothing to change
        LivingEntity victim = (LivingEntity) (Object) this;
        if (TinkererEvents.framesRefuse(victim, source, incoming, this.lastHurt)) return; // vanilla refuses it at its own frame test further down
        float result = TinkererEvents.postLivingDamage(victim, source, incoming);
        if (result <= 0.0F) {
            cir.setReturnValue(false);
        } else {
            damage.set(result);
        }
    }
}
