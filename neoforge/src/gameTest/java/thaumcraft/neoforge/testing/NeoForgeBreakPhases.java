package thaumcraft.neoforge.testing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;
import thaumcraft.common.testing.NativeBreakPhases;

/**
 * The NeoForge half of the native block-break phases.
 *
 * <p>NeoForge 21.1 owns no {@code PlayerBlockBreakEvents}; its native graph of a break is:</p>
 * <ul>
 *   <li>the cancellable {@link BlockEvent.BreakEvent}, which
 *       {@code net.neoforged.neoforge.common.CommonHooks.fireBlockBreak} posts at the head of
 *       {@code ServerPlayerGameMode.destroyBlock}. The event is already cancelled when the
 *       adventure restriction, a game master block or the tool tier forbids the break, which is
 *       exactly the set of conditions that keeps the Fabric BEFORE from running;</li>
 *   <li>the real removal itself, which the NeoForge patch routes through the private helper
 *       {@code ServerPlayerGameMode.removeBlock(BlockPos, BlockState, boolean)}.</li>
 * </ul>
 *
 * <p>The BEFORE phase therefore observes the native event at the lowest priority, so it sees
 * the final cancellation state of the event - the Architectury {@code BlockEvent.BREAK} of this
 * mod runs at {@code EventPriority.HIGH} and cancels the native event when it denies the break.
 * The AFTER phase observes the real removal, so it only runs after a block was really removed,
 * exactly like the Fabric AFTER event.</p>
 */
public final class NeoForgeBreakPhases implements NativeBreakPhases {
    /** The phases of one player, in one level. */
    private static final class Session {
        final List<Listener> before = new ArrayList<>();
        final List<Listener> canceled = new ArrayList<>();
        final List<Listener> after = new ArrayList<>();
    }

    /** The identity key of a session: the exact level and the exact player. */
    private static final class Key {
        private final ServerLevel level;
        private final ServerPlayer player;

        Key(ServerLevel level, ServerPlayer player) {
            this.level = level;
            this.player = player;
        }

        @Override public boolean equals(Object other) {
            return other instanceof Key key && key.level == level && key.player == player;
        }

        @Override public int hashCode() {
            return 31 * System.identityHashCode(level) + System.identityHashCode(player);
        }
    }

    private static final Map<Key, Session> SESSIONS = new HashMap<>();
    private static boolean registered;

    private final Key key;
    private final Phase phase;
    private final Listener listener;

    private NeoForgeBreakPhases(Key key, Phase phase, Listener listener) {
        this.key = key;
        this.phase = phase;
        this.listener = listener;
    }

    @Override
    public void close() {
        var session = SESSIONS.get(key);
        if (session == null) return;
        switch (phase) {
            case BEFORE -> session.before.remove(listener);
            case CANCELED -> session.canceled.remove(listener);
            case AFTER -> session.after.remove(listener);
        }
        if (session.before.isEmpty() && session.canceled.isEmpty() && session.after.isEmpty()) {
            SESSIONS.remove(key);
        }
    }

    /** Installs the observer of one phase of the breaks of one player. */
    public static NativeBreakPhases install(ServerLevel level, ServerPlayer player, Phase phase, Listener listener) {
        var key = new Key(level, player);
        var session = SESSIONS.get(key);
        if (session == null) {
            session = new Session();
            SESSIONS.put(key, session);
        }
        switch (phase) {
            case BEFORE -> session.before.add(listener);
            case CANCELED -> session.canceled.add(listener);
            case AFTER -> session.after.add(listener);
        }
        if (!registered) {
            registered = true;
            NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, NeoForgeBreakPhases::onBreak);
        }
        return new NeoForgeBreakPhases(key, phase, listener);
    }

    /** The native BEFORE phase of NeoForge, at the lowest priority of the event bus. */
    private static void onBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        var session = SESSIONS.get(new Key(level, player));
        if (session == null || session.before.isEmpty()) return;
        // An event that is already cancelled never reaches the Fabric BEFORE either:
        // the adventure restriction, a game master block and the tool tier all
        // refuse the break before the destruction continues.
        if (event.isCanceled()) return;
        var pos = event.getPos();
        var state = event.getState();
        boolean allowed = true;
        for (var listener : List.copyOf(session.before)) {
            if (!listener.before(level, player, pos, state)) {
                allowed = false;
                break;
            }
        }
        if (allowed) return;
        event.setCanceled(true);
        for (var listener : List.copyOf(session.canceled)) {
            listener.canceled(level, player, pos, state);
        }
    }

    /** The real removal of a block, observed by the AFTER phase. */
    public static void afterRemoval(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState state) {
        var session = SESSIONS.get(new Key(level, player));
        if (session == null || session.after.isEmpty()) return;
        for (var listener : List.copyOf(session.after)) {
            listener.after(level, player, pos, state);
        }
    }
}
