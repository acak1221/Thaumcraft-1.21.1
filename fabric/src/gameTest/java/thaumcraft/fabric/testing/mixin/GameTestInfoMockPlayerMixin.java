package thaumcraft.fabric.testing.mixin;

import net.minecraft.gametest.framework.GameTestInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thaumcraft.common.testing.MockPlayerCleanup;

/** Release all helper-owned players at the verdict shared by success and every failure. */
@Mixin(GameTestInfo.class)
public abstract class GameTestInfoMockPlayerMixin {
    @Inject(method = "finish", at = @At("RETURN"))
    private void thaumcraft$releaseMockPlayers(CallbackInfo ci) {
        MockPlayerCleanup.release((GameTestInfo) (Object) this);
    }
}
