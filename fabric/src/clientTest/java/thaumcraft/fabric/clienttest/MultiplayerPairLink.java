package thaumcraft.fabric.clienttest;

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerChatPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;

/**
 * The shared parts of the TWO physical client acceptance.
 *
 * <p>Wave 2 proved the payloads of ONE physical client against a dedicated
 * server. The gap that was left open is the SECOND physical client: a payload
 * that is sent to the wrong player, a state that is not synchronised between
 * two viewers, a screen that opens for one player and not for the other, and a
 * race between the two.</p>
 *
 * <p>Two separate JVMs cannot share memory, so the two clients talk through
 * three plain files in one owned directory. The files are written atomically
 * (write to a temporary file, then an atomic rename), so a reader never sees
 * a half written line.</p>
 */
public final class MultiplayerPairLink {
    private MultiplayerPairLink() {}

    /** The file the LEAD writes and the PEER reads. */
    public static Path commandFile(Path directory) { return directory.resolve("lead.cmd"); }
    /** The file the PEER writes and the LEAD reads. */
    public static Path resultFile(Path directory) { return directory.resolve("peer.result"); }
    /** The file the PEER writes continuously and the LEAD reads. */
    public static Path statusFile(Path directory) { return directory.resolve("peer.status"); }

    /**
     * Counts every clientbound and serverbound custom payload that really
     * crossed the socket of ONE physical client.
     *
     * <p>The census has to sit BEFORE the native {@code Connection} handler in
     * the netty pipeline: {@code Connection} extends
     * {@code SimpleChannelInboundHandler} and therefore never forwards an
     * inbound packet to a handler placed after it.</p>
     */
    public static final class Census {
        public final Map<String, AtomicInteger> clientbound = new ConcurrentHashMap<>();
        public final Map<String, AtomicInteger> serverbound = new ConcurrentHashMap<>();
        public final List<String> messages = java.util.Collections.synchronizedList(new ArrayList<>());
        private Channel channel;

        public void install(Connection connection) {
            try {
                Channel candidate = (Channel) field(connection, "channel");
                if (candidate == channel) return;
                channel = candidate;
                var handler = new ChannelDuplexHandler() {
                    @Override
                    public void channelRead(ChannelHandlerContext context, Object message) throws Exception {
                        if (message instanceof ClientboundCustomPayloadPacket packet) {
                            count(clientbound, packet.payload().type().id().toString());
                        } else if (message instanceof ClientboundSystemChatPacket packet) {
                            messages.add(packet.content().getString());
                        } else if (message instanceof ClientboundPlayerChatPacket packet) {
                            var unsigned = packet.unsignedContent();
                            if (unsigned != null) messages.add(unsigned.getString());
                        }
                        super.channelRead(context, message);
                    }

                    @Override
                    public void write(ChannelHandlerContext context, Object message, ChannelPromise promise) throws Exception {
                        if (message instanceof ServerboundCustomPayloadPacket packet) {
                            count(serverbound, packet.payload().type().id().toString());
                        }
                        super.write(context, message, promise);
                    }
                };
                var pipeline = candidate.pipeline();
                for (String name : pipeline.names()) {
                    if (pipeline.get(name) instanceof Connection) {
                        pipeline.addBefore(name, "thaumcraft-multiplayer-census", handler);
                        return;
                    }
                }
                pipeline.addLast("thaumcraft-multiplayer-census", handler);
            } catch (ReflectiveOperationException failure) {
                throw new IllegalStateException("Cannot install the multiplayer payload census", failure);
            }
        }

        public int clientbound(String id) { return clientbound.getOrDefault(id, new AtomicInteger()).get(); }
        public int serverbound(String id) { return serverbound.getOrDefault(id, new AtomicInteger()).get(); }

        public String lastMessageContaining(String needle) {
            String found = null;
            synchronized (messages) {
                for (String message : messages) if (message.contains(needle)) found = message;
            }
            return found;
        }

        private static void count(Map<String, AtomicInteger> counters, String id) {
            counters.computeIfAbsent(id, ignored -> new AtomicInteger()).incrementAndGet();
        }
    }

    /** Writes the file so that a reader never observes a half written line. */
    public static void writeAtomic(Path file, String content) {
        try {
            Files.createDirectories(file.getParent());
            Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
            Files.writeString(temporary, content, StandardCharsets.UTF_8);
            try {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException atomicUnsupported) {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException failure) {
            throw new IllegalStateException("Cannot write the pair link file " + file, failure);
        }
    }

    /** The whole file, or null when it does not exist yet. */
    public static String read(Path file) {
        try {
            if (!Files.isRegularFile(file)) return null;
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException failure) {
            return null;
        }
    }

    /** The first line of a "seq\npayload" file, or -1 when there is none. */
    public static int readSequence(Path file) {
        String content = read(file);
        if (content == null) return -1;
        int newline = content.indexOf('\n');
        String head = newline < 0 ? content : content.substring(0, newline);
        try {
            return Integer.parseInt(head.trim());
        } catch (NumberFormatException malformed) {
            return -1;
        }
    }

    /** The body of a "seq\npayload" file. */
    public static String readPayload(Path file) {
        String content = read(file);
        if (content == null) return null;
        int newline = content.indexOf('\n');
        return newline < 0 ? "" : content.substring(newline + 1).trim();
    }

    private static Object field(Object owner, String name) throws ReflectiveOperationException {
        for (Class<?> type = owner.getClass(); type != null; type = type.getSuperclass()) {
            try {
                Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                return field.get(owner);
            } catch (NoSuchFieldException ignored) {
                // keep walking
            }
        }
        throw new NoSuchFieldException(name);
    }
}
