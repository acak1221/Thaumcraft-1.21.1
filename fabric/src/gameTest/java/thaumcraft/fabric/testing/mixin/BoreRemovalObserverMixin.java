package thaumcraft.fabric.testing.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import thaumcraft.common.testing.BoreProtectionRuntimeChecks.DeclaredRemovalFailure;
import thaumcraft.fabric.testing.BoreProtectionProbe;

/** TEST MODULE ONLY. No production permission/hook and no native call replay.
 * NONE observes the actual vanilla entry/return passively. FALSE/THROW are only
 * the declared fixture's FIRST exact FINISH/native-tick target entry AFTER ARM.
 * No exception catch here: production must contain its own RuntimeExceptions. */
@Mixin(Level.class)
public abstract class BoreRemovalObserverMixin {
    @Inject(method = "removeBlock(Lnet/minecraft/core/BlockPos;Z)Z", at = @At("HEAD"), cancellable = true, require = 1, allow = 1)
    private void thaumcraft$boreProtectionNativeEntry(BlockPos pos, boolean moving, CallbackInfoReturnable<Boolean> cir) {
        switch (BoreProtectionProbe.nativeEntry((Level)(Object)this, pos, moving)) {
            case FALSE -> cir.setReturnValue(false);
            case THROW -> throw new DeclaredRemovalFailure();
            case NONE -> { }
        }
    }
    @Inject(method = "removeBlock(Lnet/minecraft/core/BlockPos;Z)Z", at = @At("RETURN"), require = 1, allow = 1)
    private void thaumcraft$boreProtectionNativeReturn(BlockPos pos, boolean moving, CallbackInfoReturnable<Boolean> cir) {
        BoreProtectionProbe.nativeReturn((Level)(Object)this, pos, cir.getReturnValue());
    }
}
