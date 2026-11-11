package thaumcraft.fabric.testing;

import io.netty.buffer.Unpooled;
import java.util.List;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.impl.networking.RegistrationPayload;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.server.network.CommonListenerCookie;
import thaumcraft.common.lib.inventoryscan.ScanReply;
import thaumcraft.common.lib.inventoryscan.ScanRequest;
import thaumcraft.common.testing.InventoryScanRuntimeChecks;
import thaumcraft.fabric.testing.mixin.GameTestHelperAccess;

/**
 * A genuine Fabric play connection for the inventory scanning checks: the actor joins through the
 * player list and announces the reply channel as a client would.
 *
 * <p>The mod's channels are Architectury's, whose wire payload wraps the mod's own and is not public
 * API. So a request crosses its codec and then enters the server where the registered receiver would
 * hand it over ({@code InventoryScanServer.receive}), after checking that this receiver exists; and a
 * receipt is observed in the reply transport, which sends it for real under the same channel test as
 * the mod's own transport. The wrapper itself is not exercised here.</p>
 */
final class InventoryScanPlatform {
    private InventoryScanPlatform() {}

    static void install() {
        InventoryScanRuntimeChecks.installPlatform((player, connection) -> {
            // The mod's transport with an observer; installed here, after the mod's own initialisation.
            thaumcraft.common.lib.inventoryscan.InventoryScanServer.installTransport((actor, reply) -> {
                if (!dev.architectury.networking.NetworkManager.canPlayerReceive(actor, ScanReply.TYPE)) return;
                InventoryScanRuntimeChecks.sent(actor, reply);
                dev.architectury.networking.NetworkManager.sendToPlayer(actor, reply);
            });
            player.server.getPlayerList().placeNewPlayer(connection, player,
                    CommonListenerCookie.createInitial(player.getGameProfile(), false));
            player.connection.handleCustomPayload(new ServerboundCustomPayloadPacket(
                    new RegistrationPayload(RegistrationPayload.REGISTER, List.of(ScanReply.TYPE.id()))));
            if (!ServerPlayNetworking.canSend(player, ScanReply.TYPE)) throw new AssertionError("real Fabric registration announces the reply channel");
        }, (player, request) -> {
            if (!ServerPlayNetworking.getGlobalReceivers().contains(ScanRequest.TYPE.id())) throw new AssertionError("native Fabric C2S receiver was not registered");
            var wire = new RegistryFriendlyByteBuf(Unpooled.buffer(), player.registryAccess());
            try {
                ScanRequest.STREAM_CODEC.encode(wire, request);
                var decoded = ScanRequest.STREAM_CODEC.decode(wire);
                if (wire.isReadable() || !request.equals(decoded)) throw new AssertionError("request consumes its exact Fabric payload bytes");
                thaumcraft.common.lib.inventoryscan.InventoryScanServer.receive(player, decoded);
            } finally { wire.release(); }
        }, helper -> ((GameTestHelperAccess) helper).thaumcraft$testInfo());
    }
}
