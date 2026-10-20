package thaumcraft.common.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import thaumcraft.common.blocks.BlockWarded;

/** A ward is removed by its owner's focus, not by a creative-mode mining bypass. Commands remain explicit administrative changes. */
@Mixin(ServerPlayerGameMode.class)
abstract class WardedPlayerGameModeMixin {
    @Shadow @Final protected ServerPlayer player;
    @Inject(method = "destroyBlock", at = @At("HEAD"), cancellable = true)
    private void thaumcraft$protectWard(BlockPos pos, CallbackInfoReturnable<Boolean> callback) {
        if (player.serverLevel().getBlockState(pos).getBlock() instanceof BlockWarded) callback.setReturnValue(false);
    }
}
