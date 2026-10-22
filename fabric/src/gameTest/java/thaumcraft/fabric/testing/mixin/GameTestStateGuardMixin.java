package thaumcraft.fabric.testing.mixin;

import net.minecraft.gametest.framework.GameTestInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thaumcraft.common.testing.GameTestStateGuard;

@Mixin(GameTestInfo.class)
public abstract class GameTestStateGuardMixin {
    @Inject(method = "startTest", at = @At("HEAD"))
    private void thaumcraft$snapshotScenario(CallbackInfo ci) {
        GameTestStateGuard.begin((GameTestInfo) (Object) this);
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void thaumcraft$enterScenario(CallbackInfo ci) {
        GameTestStateGuard.enter((GameTestInfo) (Object) this);
    }

    // After user callbacks/finally, before native pass/failure listeners publish the verdict.
    @Inject(method = "tick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/gametest/framework/GameTestInfo;tickInternal()V", shift = At.Shift.AFTER))
    private void thaumcraft$checkScenario(CallbackInfo ci) {
        GameTestStateGuard.leave((GameTestInfo) (Object) this);
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void thaumcraft$exitScenario(CallbackInfo ci) {
        GameTestStateGuard.exit();
    }
}
