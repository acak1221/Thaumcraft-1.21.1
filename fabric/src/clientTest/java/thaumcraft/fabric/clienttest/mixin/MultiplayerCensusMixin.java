package thaumcraft.fabric.clienttest.mixin;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.Connection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thaumcraft.fabric.clienttest.MultiplayerPairLink;

/**
 * The payload census has to be in the netty pipeline BEFORE the first play
 * packet is read.
 *
 * <p>The server sends its join time payloads (the knowledge snapshot, the
 * research settings and the accessory contents) in the same burst as the
 * login packet, and the Fabric join event only fires while that burst is
 * already being processed. A census that is installed there therefore misses
 * exactly the payloads of the join, which is why the second client of the
 * acceptance reported zero knowledge snapshots.</p>
 *
 * <p>The {@code ClientPacketListener} is built before the play phase starts,
 * so its constructor is the earliest point at which the real play connection
 * exists.</p>
 */
@Mixin(ClientPacketListener.class)
public abstract class MultiplayerCensusMixin {
    @Inject(method = "<init>(Lnet/minecraft/client/Minecraft;Lnet/minecraft/network/Connection;Lnet/minecraft/client/multiplayer/CommonListenerCookie;)V",
            at = @At("RETURN"), require = 1, allow = 1)
    private void thaumcraft$censusInstall(net.minecraft.client.Minecraft minecraft, Connection connection,
                                         net.minecraft.client.multiplayer.CommonListenerCookie cookie, CallbackInfo ci) {
        if (!Boolean.getBoolean("thaumcraft.multiplayerClient") && !Boolean.getBoolean("thaumcraft.multiplayerPeer")) return;
        MultiplayerPairLink.CENSUS.install(connection);
    }
}
