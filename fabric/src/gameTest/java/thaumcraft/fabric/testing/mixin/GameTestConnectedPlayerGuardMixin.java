package thaumcraft.fabric.testing.mixin;

import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thaumcraft.common.testing.GameTestStateGuard;

@Mixin(PlayerList.class)
public abstract class GameTestConnectedPlayerGuardMixin {
    @Inject(method = "placeNewPlayer", at = @At("RETURN"))
    private void thaumcraft$acquirePlayer(Connection connection, ServerPlayer player, CommonListenerCookie cookie, CallbackInfo ci) {
        GameTestStateGuard.acquired(GameTestStateGuard.resource("connected player", player));
    }

    @Inject(method = "remove", at = @At("RETURN"))
    private void thaumcraft$releasePlayer(ServerPlayer player, CallbackInfo ci) {
        GameTestStateGuard.released(GameTestStateGuard.resource("connected player", player));
    }
}
