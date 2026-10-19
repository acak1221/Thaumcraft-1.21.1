package thaumcraft.common.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.TickingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import thaumcraft.common.lib.bore.BoreNativeContext;

/** Observe full native state requests BEFORE early returns/chunk access; no
 * cancellation. The ticker redirect establishes only native invocation context. */
@Mixin(Level.class)
public abstract class BoreLevelMutationMixin {
    @Inject(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z",
            at = @At("HEAD"), require = 1, allow = 1)
    private void thaumcraft$boreLevelMutation(BlockPos pos, BlockState state, int flags, int recursionLeft,
                                             CallbackInfoReturnable<Boolean> cir) {
        BoreNativeContext.nativeLevelRequest((Level) (Object) this, pos);
    }
    @Redirect(method = "tickBlockEntities()V", require = 1, allow = 1,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/TickingBlockEntity;tick()V"))
    private void thaumcraft$boreLevelTicker(TickingBlockEntity ticker) {
        BoreNativeContext.nativeLevelTick((Level) (Object) this, ticker);
    }
}
