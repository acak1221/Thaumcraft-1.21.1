package thaumcraft.fabric.clienttest.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thaumcraft.fabric.clienttest.BoreClientSequence;

/** Actual dying owner AND actual death argument, never inferred from cache age. */
@Mixin(LivingEntity.class)
public abstract class BoreAmbientDeathLootObserverMixin {
    @Inject(method = "dropAllDeathLoot(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;)V",
            at = @At("HEAD"), require = 1, allow = 1)
    private void thaumcraft$boreAmbientDeathHead(ServerLevel world, DamageSource damage, CallbackInfo ci) {
        BoreClientSequence.observeAmbientDeathHead((LivingEntity)(Object)this, world, damage);
    }

    @Inject(method = "dropAllDeathLoot(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;)V",
            at = @At("RETURN"), require = 1, allow = 1)
    private void thaumcraft$boreAmbientDeathReturn(ServerLevel world, DamageSource damage, CallbackInfo ci) {
        BoreClientSequence.observeAmbientDeathReturn((LivingEntity)(Object)this, world, damage);
    }
}
