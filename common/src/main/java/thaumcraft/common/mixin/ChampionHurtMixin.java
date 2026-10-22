package thaumcraft.common.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import thaumcraft.common.lib.mods.ChampionHooks;

/**
 * The original hurt trigger of the champion modifiers. The 1.7 original answered the
 * living hurt event of the victim: a champion that was hurt answered the event with
 * its own defending modifier, and a champion that did the hurting answered the same
 * event with its attacking modifier. The native port stands in the same place, before
 * the armour of the victim, and keeps the order of the original event.
 */
// Player overrides actuallyHurt without calling LivingEntity's implementation.
// Wrap each implementation, not hurt() as well, to dispatch accepted hits once.
@Mixin({LivingEntity.class, Player.class})
public abstract class ChampionHurtMixin {
    /** The original living hurt event of the champion modifiers. */
    @WrapMethod(method = "actuallyHurt(Lnet/minecraft/world/damagesource/DamageSource;F)V")
    private void thaumcraft$championHurt(DamageSource source, float amount, Operation<Void> original) {
        LivingEntity victim = (LivingEntity) (Object) this;
        float remaining = ChampionHooks.damageChampion(victim, source, amount);
        if (remaining > 0.0F) {
            Entity attacker = source == null ? null : source.getEntity();
            if (attacker != null) ChampionHooks.attackChampion(attacker, victim, remaining);
        }
        original.call(source, remaining);
    }
}
