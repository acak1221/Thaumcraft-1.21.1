package thaumcraft.common.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import thaumcraft.common.lib.world.WardedBehaviour;

/** Original source callbacks must not remove the ward while producing their walking/attack/entity effects. */
@Mixin(Level.class)
public abstract class WardedCallbackMutationMixin {
    @Inject(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z", at = @At("HEAD"), cancellable = true)
    private void thaumcraft$keepWardDuringSourceCallback(BlockPos pos, BlockState state, int flags, int recursion, CallbackInfoReturnable<Boolean> callback) {
        if (WardedBehaviour.deniesCallbackReplacement((Level) (Object) this, pos, state)) callback.setReturnValue(false);
    }
}
