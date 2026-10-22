package thaumcraft.common.testing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.server.level.ServerPlayer;

/**
 * Owns mock players until their framework verdict, on both loaders.
 *
 * <p>Vanilla adds connected mock players to the server player list but does not remove
 * them on failure. They retain chunk tickets for the session, causing the suite's
 * measured chunk-save memory exhaustion. Key by test info rather than helper so all
 * helpers belonging to the same test share its lifetime. Only the framework's finish
 * hook drains this registry: assertion failures, callbacks and timeouts bypass helper
 * succeed/fail methods. All calls occur on the GameTest server thread.</p>
 */
public final class MockPlayerCleanup {
    private static final Map<GameTestInfo, List<ServerPlayer>> MOCK_PLAYERS =
            Collections.synchronizedMap(new WeakHashMap<>());

    private MockPlayerCleanup() { }

    public static void track(GameTestInfo info, ServerPlayer player) {
        if (player != null) MOCK_PLAYERS.computeIfAbsent(info, ignored -> new ArrayList<>()).add(player);
    }

    /** Idempotent; remove ownership before releasing players, even if cleanup fails. */
    public static void release(GameTestInfo info) {
        List<ServerPlayer> players = MOCK_PLAYERS.remove(info);
        if (players == null) return;
        for (ServerPlayer player : players) {
            try {
                var list = player.level() == null ? null : player.level().getServer().getPlayerList();
                if (list != null && list.getPlayer(player.getUUID()) == player) list.remove(player);
            } catch (RuntimeException ignored) {
                // A finished test must never fail because its mock player could not be released.
            } finally {
                try { player.discard(); } catch (RuntimeException ignored) { }
            }
        }
    }
}
