package thaumcraft.fabric.clienttest.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thaumcraft.common.tiles.TileAlchemyFurnace;
import thaumcraft.fabric.clienttest.BoreClientSequence;

@Mixin(value = TileAlchemyFurnace.class, remap = false)
public abstract class BoreAlchemyTickObserverMixin {
    @Inject(method = "tick(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lthaumcraft/common/tiles/TileAlchemyFurnace;)V", at = @At("HEAD"), require = 1, allow = 1)
    private static void thaumcraft$boreAlchemyHead(Level level, BlockPos pos, BlockState state, TileAlchemyFurnace furnace, CallbackInfo ci) {
        BoreClientSequence.observeAlchemyTickHead(level, pos, state, furnace);
    }
    @Inject(method = "tick(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lthaumcraft/common/tiles/TileAlchemyFurnace;)V", at = @At("RETURN"), require = 1)
    private static void thaumcraft$boreAlchemyReturn(Level level, BlockPos pos, BlockState state, TileAlchemyFurnace furnace, CallbackInfo ci) {
        BoreClientSequence.observeAlchemyTickReturn(level, pos, state, furnace);
    }
}
