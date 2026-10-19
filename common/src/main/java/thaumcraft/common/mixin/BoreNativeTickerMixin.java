package thaumcraft.common.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thaumcraft.common.lib.bore.BoreNativeContext;

/** Native private inner class; annotation processor/refmap must remap its name,
 * native fields and descriptors. No synthetic outer-field-name guess. */
@Mixin(targets = "net.minecraft.world.level.chunk.LevelChunk$BoundTickingBlockEntity")
public abstract class BoreNativeTickerMixin implements BoreNativeContext.BoundAccess {
    @Shadow @Final private BlockEntity blockEntity;
    @Shadow @Final private BlockEntityTicker<BlockEntity> ticker;
    @Unique private LevelChunk thaumcraft$boreNativeChunk;

    @Inject(method = "<init>(Lnet/minecraft/world/level/chunk/LevelChunk;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/level/block/entity/BlockEntityTicker;)V",
            at = @At("RETURN"), require = 1, allow = 1)
    private void thaumcraft$boreCaptureChunk(LevelChunk chunk, BlockEntity owner, BlockEntityTicker<BlockEntity> bound, CallbackInfo ci) {
        thaumcraft$boreNativeChunk = chunk;
    }
    @Override public LevelChunk thaumcraft$boreBoundChunk() { return thaumcraft$boreNativeChunk; }
    @Override public BlockEntity thaumcraft$boreBoundOwner() { return blockEntity; }
    @Override public BlockEntityTicker<BlockEntity> thaumcraft$boreBoundTicker() { return ticker; }

    @Redirect(method = "tick()V", require = 1, allow = 1,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/BlockEntityTicker;tick(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/entity/BlockEntity;)V"))
    private void thaumcraft$boreBoundTicker(BlockEntityTicker<BlockEntity> actualTicker, Level world, BlockPos pos,
                                           BlockState state, BlockEntity owner) {
        BoreNativeContext.nativeBoundTick((BoreNativeContext.BoundAccess) (Object) this, actualTicker, world, pos, state, owner);
    }
}
