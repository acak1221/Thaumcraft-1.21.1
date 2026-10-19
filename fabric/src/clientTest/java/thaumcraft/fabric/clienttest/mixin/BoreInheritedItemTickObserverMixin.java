package thaumcraft.fabric.clienttest.mixin;

import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thaumcraft.fabric.clienttest.BoreInheritedItemWitness;

/** Private passive callbacks, independent of active Ledger/bootstrap readiness.
 * Never cancels native ticking, changes a return, or writes native item state. */
@Mixin(ItemEntity.class)
public abstract class BoreInheritedItemTickObserverMixin {
    @Inject(method = "tick()V", at = @At("HEAD"), require = 1, allow = 1)
    private void thaumcraft$boreInheritedItemHead(CallbackInfo ci) {
        BoreInheritedItemWitness.tickHead((ItemEntity)(Object)this);
    }

    @Inject(method = "tick()V", at = @At("RETURN"), require = 1)
    private void thaumcraft$boreInheritedItemReturn(CallbackInfo ci) {
        BoreInheritedItemWitness.tickReturn((ItemEntity)(Object)this);
    }
}
