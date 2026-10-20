package thaumcraft.common.mixin;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import thaumcraft.common.lib.mods.ChampionHooks;

/**
 * The original champion death reward.
 *
 * <p>The 1.7 original answered the living drops event of every mob that died and paid the
 * champion reward there: five to seven extra experience and one loot bag for a champion mob
 * that was hit by a player (oracle {@code EventHandlerEntity.livingDrops:453-461}). The
 * native port stands in the same place, inside the death loot of the mob, and the recently
 * hit flag of the native death loot is the native form of the original one.
 */
@Mixin(LivingEntity.class)
public abstract class ChampionDeathLootMixin {
    /** The original living drops event of the champion reward. */
    @ModifyArgs(method = "dropAllDeathLoot", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/world/entity/LivingEntity;dropCustomDeathLoot(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;Z)V"))
    private void thaumcraft$championDeathLoot(Args arguments) {
        // Observe the arguments of the native death loot without replacing any of them.
        ChampionHooks.onDeathLoot((LivingEntity) (Object) this, arguments.get(0), arguments.get(1),
                arguments.get(2));
    }
}
