package thaumcraft.neoforge.testing.mixin;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.players.PlayerList;
import net.neoforged.neoforge.network.registration.ChannelAttributes;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Gives the mock players of a GameTest the negotiated NeoForge channels.
 *
 * <p>NeoForge refuses to send a modded payload to a connection without a negotiated channel
 * setup. The vanilla GameTest players - both the helper mock player and the shared runtime
 * fixtures - join with a bare connection, so every server-to-client payload of the mod (the
 * original research settings on login, the knowledge snapshot, the research answers) would be
 * rejected with "Payload ... may not be sent to the client!". The NeoForge testing API supplies the
 * channels of the local mods, exactly as the real configuration phase does for a real client.</p>
 *
 * <p>A connection that already carries a negotiated setup (a real client) is never touched.</p>
 */
@Mixin(PlayerList.class)
public abstract class GameTestHelperMockConnectionMixin {
    @Inject(method = "placeNewPlayer(Lnet/minecraft/network/Connection;Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/server/network/CommonListenerCookie;)V", at = @At("HEAD"))
    private void thaumcraft$negotiatedMockConnection(Connection connection, ServerPlayer player,
                                                      CommonListenerCookie cookie, CallbackInfo ci) {
        if (ChannelAttributes.getPayloadSetup(connection) == null) {
            NetworkRegistry.configureMockConnection(connection);
        }
    }
}
