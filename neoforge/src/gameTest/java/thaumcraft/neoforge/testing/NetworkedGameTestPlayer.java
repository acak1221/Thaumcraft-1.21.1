package thaumcraft.neoforge.testing;

import com.mojang.authlib.GameProfile;
import dev.architectury.impl.NetworkAggregator;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.util.ReferenceCountUtil;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.BundlePacket;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.neoforged.neoforge.network.connection.ConnectionType;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.network.KnowledgeSyncPayload;

/** A native NeoForge mock handshake followed by the real player login and knowledge sync. */
final class NetworkedGameTestPlayer {
    private NetworkedGameTestPlayer() {}

    static ServerPlayer create(GameTestHelper test) {
        return createSession(test).player();
    }

    static Session createSession(GameTestHelper test) {
        test.assertTrue(NetworkRegistry.getCodec(KnowledgeSyncPayload.TYPE.id(),
                        ConnectionProtocol.PLAY, PacketFlow.CLIENTBOUND) != null,
                "knowledge payload is registered for clientbound play");

        var level = test.getLevel();
        var profile = new GameProfile(UUID.randomUUID(), "tc4-test-player");
        var information = ClientInformation.createDefault();
        var player = new ServerPlayer(level.getServer(), level, profile, information);
        var connection = new Connection(PacketFlow.SERVERBOUND);
        var channel = new EmbeddedChannel(connection);

        // The vanilla GameTest helper skips negotiation. NeoForge's testing API supplies
        // the negotiated mod channels before placeNewPlayer fires the real JOIN event.
        NetworkRegistry.configureMockConnection(connection);
        thaumcraft.common.Thaumcraft.LOG.info("DIAG after configure: channelId={} setupPresent={}",
                System.identityHashCode(connection.channel()),
                net.neoforged.neoforge.network.registration.ChannelAttributes.getPayloadSetup(connection) != null);
        thaumcraft.common.Thaumcraft.LOG.info("DIAG connection identity = {}", System.identityHashCode(connection));
        var cookie = new CommonListenerCookie(profile, 0, information, false, ConnectionType.NEOFORGE);
        level.getServer().getPlayerList().placeNewPlayer(connection, player, cookie);

        var session = new Session(player, channel);
        session.captureKnowledge(test, KnowledgeAccess.get(player).save(), true, false);
        return session;
    }

    record Session(ServerPlayer player, EmbeddedChannel channel) {
        void captureKnowledge(GameTestHelper test, CompoundTag expected, boolean required) {
            captureKnowledge(test, expected, required, true);
        }

        private void captureKnowledge(GameTestHelper test, CompoundTag expected, boolean required, boolean requireEncodedBytes) {
            test.assertValueEqual(KnowledgeAccess.get(player).save(), expected, "observer expects current server state");
            channel.runPendingTasks();
            boolean knowledgeSent = false;
            boolean[] encodedBytesSeen = {false};
            Object outbound;
            while ((outbound = channel.readOutbound()) != null) {
                try {
                    knowledgeSent |= checkKnowledgePacket(outbound, player, test, encodedBytesSeen);
                } finally {
                    ReferenceCountUtil.release(outbound);
                }
            }
            test.assertTrue(!required || knowledgeSent, "negotiated connection emits the expected knowledge sync payload");
            test.assertTrue(!requireEncodedBytes || !knowledgeSent || encodedBytesSeen[0],
                    "purchase observer decodes actual outgoing Architectury knowledge bytes");
        }
    }

    private static boolean checkKnowledgePacket(Object outbound, ServerPlayer player, GameTestHelper test, boolean[] encodedBytesSeen) {
        if (outbound instanceof BundlePacket<?> bundle) {
            boolean found = false;
            for (var packet : bundle.subPackets()) {
                found |= checkKnowledgePacket(packet, player, test, encodedBytesSeen);
            }
            return found;
        }
        if (!(outbound instanceof ClientboundCustomPayloadPacket packet)
                || !packet.payload().type().id().equals(KnowledgeSyncPayload.TYPE.id())) {
            return false;
        }

        KnowledgeSyncPayload knowledge;
        if (packet.payload() instanceof KnowledgeSyncPayload direct) {
            knowledge = direct;
        } else if (packet.payload() instanceof NetworkAggregator.BufCustomPacketPayload wrapped) {
            // Architectury transports registered records as encoded bytes in this wrapper.
            var buffer = new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(wrapped.payload()), player.registryAccess());
            try {
                knowledge = KnowledgeSyncPayload.STREAM_CODEC.decode(buffer);
                test.assertTrue(!buffer.isReadable(), "knowledge payload consumes its exact wire bytes");
                encodedBytesSeen[0] = true;
            } finally {
                buffer.release();
            }
        } else {
            throw new IllegalStateException("Unexpected knowledge payload representation: "
                    + packet.payload().getClass().getName());
        }
        test.assertValueEqual(knowledge.state(), KnowledgeAccess.get(player).save(),
                "actual emitted S2C snapshot equals the current authoritative knowledge");
        return true;
    }
}
