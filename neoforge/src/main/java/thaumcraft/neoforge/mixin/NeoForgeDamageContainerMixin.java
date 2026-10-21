package thaumcraft.neoforge.mixin;

import java.util.Stack;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Gives a direct damage of the shared module the damage container NeoForge expects.
 *
 * <p>NeoForge 21.1 rewrites the damage pipeline of a living entity: {@code LivingEntity.hurt}
 * pushes a {@link DamageContainer} on the protected stack of the entity and
 * {@code LivingEntity.actuallyHurt} reads the reductions of that container. The shared module
 * applies the damage of the original in two places without the ordinary hurt path, because the
 * original damage ignores the generic hurt grace:
 * {@code EntityTaintacle.tick} and {@code EntityTaintacleSmall.tick} call the protected
 * {@code actuallyHurt} directly. Without a container the NeoForge patch reads an empty stack and
 * the entity tick dies with an {@link java.util.EmptyStackException}, which the NeoForge
 * GameTest suite reported as a scenario without any message.</p>
 *
 * <p>The mixin supplies the container of the damage that NeoForge itself would have pushed, so
 * the direct damage behaves exactly like the damage of the ordinary hurt path. A damage that
 * arrives through {@code hurt} already owns a container and is left untouched.</p>
 */
@Mixin(LivingEntity.class)
public abstract class NeoForgeDamageContainerMixin {
    /** The NeoForge damage container stack of the entity. */
    @Shadow(remap = false) protected Stack<DamageContainer> damageContainers;

    /** Whether this mixin pushed the container of the current damage. */
    @Unique private boolean thaumcraft$ownedContainer;

    @Inject(method = "actuallyHurt(Lnet/minecraft/world/damagesource/DamageSource;F)V", at = @At("HEAD"), require = 1, allow = 1)
    private void thaumcraft$supplyDamageContainer(DamageSource source, float amount, CallbackInfo ci) {
        if (damageContainers.isEmpty()) {
            damageContainers.push(new DamageContainer(source, amount));
            thaumcraft$ownedContainer = true;
        }
    }

    @Inject(method = "actuallyHurt(Lnet/minecraft/world/damagesource/DamageSource;F)V", at = @At("RETURN"), require = 1, allow = 1)
    private void thaumcraft$releaseDamageContainer(DamageSource source, float amount, CallbackInfo ci) {
        if (thaumcraft$ownedContainer) {
            thaumcraft$ownedContainer = false;
            damageContainers.pop();
        }
    }
}
