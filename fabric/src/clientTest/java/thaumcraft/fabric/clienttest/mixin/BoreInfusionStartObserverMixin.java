package thaumcraft.fabric.clienttest.mixin;

import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import thaumcraft.common.tiles.TileInfusionMatrix;
import thaumcraft.fabric.clienttest.BoreClientSequence;

@Mixin(value = TileInfusionMatrix.class, remap = false)
public abstract class BoreInfusionStartObserverMixin {
    @Inject(method = "startCrafting(Lnet/minecraft/server/level/ServerPlayer;)Z", at = @At("HEAD"), require = 1, allow = 1)
    private void thaumcraft$boreInfusionHead(ServerPlayer player, CallbackInfoReturnable<Boolean> cir) {
        BoreClientSequence.observeInfusionStartHead((TileInfusionMatrix)(Object)this, player);
    }
    @Inject(method = "startCrafting(Lnet/minecraft/server/level/ServerPlayer;)Z", at = @At("RETURN"), require = 1)
    private void thaumcraft$boreInfusionReturn(ServerPlayer player, CallbackInfoReturnable<Boolean> cir) {
        BoreClientSequence.observeInfusionStartReturn((TileInfusionMatrix)(Object)this, player);
    }
}
