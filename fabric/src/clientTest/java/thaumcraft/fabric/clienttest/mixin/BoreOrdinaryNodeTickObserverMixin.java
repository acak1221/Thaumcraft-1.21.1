package thaumcraft.fabric.clienttest.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thaumcraft.common.tiles.TileNode;
import thaumcraft.fabric.clienttest.BoreClientSequence;

/** Private client test: actual native prune/regeneration observation, never a
 * manual tick, clock/RNG write, permission or claimed outcome from the script. */
@Mixin(value = TileNode.class, remap = false)
public abstract class BoreOrdinaryNodeTickObserverMixin {
    @Inject(method = "serverTick(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lthaumcraft/common/tiles/TileNode;)V", at = @At("HEAD"), require = 1, allow = 1)
    private static void thaumcraft$boreOrdinaryHead(Level level, BlockPos pos, BlockState state, TileNode node, CallbackInfo ci) {
        BoreClientSequence.observeOrdinaryNodeTickHead(level, pos, state, node);
    }
    @Inject(method = "serverTick(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lthaumcraft/common/tiles/TileNode;)V", at = @At("RETURN"), require = 1)
    private static void thaumcraft$boreOrdinaryReturn(Level level, BlockPos pos, BlockState state, TileNode node, CallbackInfo ci) {
        BoreClientSequence.observeOrdinaryNodeTickReturn(level, pos, state, node);
    }
}
