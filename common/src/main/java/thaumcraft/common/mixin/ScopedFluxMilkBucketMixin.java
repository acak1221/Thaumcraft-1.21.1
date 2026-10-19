package thaumcraft.common.mixin;

import java.util.ArrayList;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.MilkBucketItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import thaumcraft.common.lib.potions.FluxEffects;

/** Fabric/vanilla milk only; NeoForge instead honors each instance's empty native cure set. */
@Mixin(MilkBucketItem.class)
public abstract class ScopedFluxMilkBucketMixin {
    @Redirect(method = "finishUsingItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;removeAllEffects()Z"), require = 0)
    private boolean thaumcraft$milkLeavesUncurableFlux(LivingEntity entity) {
        boolean removed = false;
        for (var effect : new ArrayList<>(entity.getActiveEffects())) {
            if (!FluxEffects.isUncurable(effect)) removed |= entity.removeEffect(effect.getEffect());
        }
        return removed;
    }
}
