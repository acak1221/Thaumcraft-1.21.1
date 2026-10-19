package thaumcraft.fabric.clienttest;

import dev.architectury.event.events.common.LifecycleEvent;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.server.MinecraftServer;

/** Isolated client-test lifecycle subscriptions. Architectury13 iterates a live
 * ArrayList, so unregistering a SERVER_STOPPED callback inside that callback
 * invalidates its iterator. Register one dispatcher BEFORE any server starts;
 * retire per-server hooks before invoking a private snapshot instead. */
final class ClientTestServerLifecycle {
    private static final Map<MinecraftServer, List<LifecycleEvent.ServerState>> HOOKS = new IdentityHashMap<>();
    private static boolean initialized;

    private ClientTestServerLifecycle() {}

    static synchronized void init() {
        if (initialized) return;
        LifecycleEvent.SERVER_STOPPED.register(ClientTestServerLifecycle::stopped);
        initialized = true;
    }

    static synchronized void register(MinecraftServer server, LifecycleEvent.ServerState hook) {
        if (!initialized || server == null || hook == null) throw new IllegalStateException("Early client-test lifecycle initialization required");
        var hooks = HOOKS.computeIfAbsent(server, ignored -> new ArrayList<>());
        if (hooks.contains(hook)) throw new IllegalStateException("Duplicate owned server-stop hook");
        hooks.add(hook);
    }

    static synchronized void unregister(LifecycleEvent.ServerState hook) {
        for (var hooks : HOOKS.values()) hooks.remove(hook);
        HOOKS.values().removeIf(List::isEmpty);
    }

    private static void stopped(MinecraftServer server) {
        List<LifecycleEvent.ServerState> hooks;
        synchronized (ClientTestServerLifecycle.class) {
            var owned = HOOKS.remove(server);
            hooks = owned == null ? List.of() : List.copyOf(owned);
        }
        Throwable first = null;
        for (var hook : hooks) {
            try { hook.stateChanged(server); }
            catch (RuntimeException | Error failure) {
                if (first == null) first = failure; else first.addSuppressed(failure);
            }
        }
        if (first instanceof RuntimeException failure) throw failure;
        if (first instanceof Error failure) throw failure;
    }
}
