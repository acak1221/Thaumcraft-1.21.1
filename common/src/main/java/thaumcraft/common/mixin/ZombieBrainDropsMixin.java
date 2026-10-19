package thaumcraft.common.mixin;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.lib.loot.ZombieBrainDrops;

/** Add the original ordinary zombie roll only inside native, gated death loot. */
@Mixin(LivingEntity.class)
public abstract class ZombieBrainDropsMixin {
    @ModifyArgs(method = "dropAllDeathLoot", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/world/entity/LivingEntity;dropCustomDeathLoot(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;Z)V"))
    private void thaumcraft$ordinaryZombieBrain(Args arguments) {
        // Observe the actual recentlyHit boolean without replacing any arguments.
        // In particular, tamed-wolf credit need not imply a non-null player killer.
        ZombieBrainDrops.onNormalDeathLoot((LivingEntity) (Object) this, arguments.get(0), arguments.get(1),
                arguments.get(2), ThaumcraftContent.MATERIALS.get("zombie_brain").get());
    }
}
