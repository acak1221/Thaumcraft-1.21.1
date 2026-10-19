package thaumcraft.common.lib.network;

import dev.architectury.networking.NetworkManager;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerPlayer;
import thaumcraft.common.container.ContainerFocalManipulator;

/** Authenticated menu/session/revision admission; raw inventory buttons cannot start jobs. */
public final class FocalManipulatorNetworking {
    private static boolean initialized;
    private static Consumer<FocalManipulatorAckPayload> clientAckReceiver = ignored -> {};

    private FocalManipulatorNetworking() {}

    public static void init() {
        if (initialized) return;
        initialized = true;
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, FocalManipulatorStartPayload.TYPE,
                FocalManipulatorStartPayload.STREAM_CODEC, (payload, context) -> context.queue(() -> {
                    if (context.getPlayer() instanceof ServerPlayer player) handle(player, payload);
                }));
        if (Platform.getEnvironment() == Env.SERVER) {
            NetworkManager.registerS2CPayloadType(FocalManipulatorAckPayload.TYPE, FocalManipulatorAckPayload.STREAM_CODEC);
        } else {
            NetworkManager.registerReceiver(NetworkManager.Side.S2C, FocalManipulatorAckPayload.TYPE,
                    FocalManipulatorAckPayload.STREAM_CODEC, (payload, context) -> context.queue(() -> clientAckReceiver.accept(payload)));
        }
    }

    public static void setClientAckReceiver(Consumer<FocalManipulatorAckPayload> receiver) {
        clientAckReceiver = Objects.requireNonNull(receiver, "receiver");
    }

    public static void requestStart(ContainerFocalManipulator menu, int upgradeId) {
        NetworkManager.sendToServer(new FocalManipulatorStartPayload(menu.containerId, menu.sessionId(),
                menu.selectionRevision(), upgradeId));
    }

    /** A direct native test call is not evidence of real wire dispatch. */
    public static boolean handle(ServerPlayer player, FocalManipulatorStartPayload payload) {
        if (!player.server.isSameThread()) throw new IllegalStateException("Focal START requires the server thread");
        if (payload == null || player.connection == null || player.hasDisconnected()
                || player.server.getPlayerList().getPlayer(player.getUUID()) != player
                || !(player.containerMenu instanceof ContainerFocalManipulator menu)) return false;
        boolean accepted = payload.containerId() == menu.containerId
                && menu.requestStart(player, payload.sessionId(), payload.expectedRevision(), payload.upgradeId());
        if (player.containerMenu == menu && menu.stillValid(player)) {
            // Correct stale and failed requests too, before releasing the GUI's
            // pending action. Neither an ACK nor a preview creates local output.
            menu.sendAllDataToRemote();
            var tile = menu.tile();
            if (tile != null) player.connection.send(ClientboundBlockEntityDataPacket.create(tile));
            NetworkManager.sendToPlayer(player, new FocalManipulatorAckPayload(payload.containerId(), payload.sessionId(),
                    payload.expectedRevision(), payload.upgradeId(), accepted, menu.selectionRevision()));
        }
        return accepted;
    }
}
