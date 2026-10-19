package thaumcraft.fabric.clienttest.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thaumcraft.common.tiles.TileArcaneBore;
import thaumcraft.fabric.clienttest.BoreClientSequence;

/** Custom mod method; observation only, including every normal early return. */
@Mixin(value = TileArcaneBore.class, remap = false)
public abstract class BoreMachineTickObserverMixin {
    @Inject(method = "tick(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lthaumcraft/common/tiles/TileArcaneBore;)V", at = @At("HEAD"), require = 1, allow = 1)
    private static void thaumcraft$boreTickHead(Level level, BlockPos pos, BlockState state, TileArcaneBore top, CallbackInfo ci) {
        BoreClientSequence.observeBoreTickHead(level, pos, state, top);
    }
    @Inject(method = "tick(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lthaumcraft/common/tiles/TileArcaneBore;)V", at = @At("RETURN"), require = 1)
    private static void thaumcraft$boreTickReturn(Level level, BlockPos pos, BlockState state, TileArcaneBore top, CallbackInfo ci) {
        BoreClientSequence.observeBoreTickReturn(level, pos, state, top);
    }
}
