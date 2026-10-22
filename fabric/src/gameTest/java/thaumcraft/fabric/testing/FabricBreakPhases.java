package thaumcraft.fabric.testing;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.common.testing.NativeBreakPhases;

/**
 * The Fabric half of the native block-break phases.
 *
 * <p>Fabric owns the three events of
 * {@code net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents}. The shared harness needs
 * to observe them without naming a loader class, so this adapter installs the observer of one
 * phase by decorating the actual invoker of that event and restores the exact previous invoker
 * when the observer is closed. The behaviour is the one the shared scenarios already relied
 * on: the existing listeners run first, the observed phase is delivered afterwards and the
 * BEFORE phase may refuse the break.</p>
 */
public final class FabricBreakPhases implements NativeBreakPhases {
    private final Object event;
    private final Object previous;
    private final Field invoker;

    private FabricBreakPhases(Object event, Object previous, Field invoker) {
        this.event = event;
        this.previous = previous;
        this.invoker = invoker;
    }

    @Override
    public void close() {
        try {
            invoker.set(event, previous);
        } catch (IllegalAccessException failure) {
            throw new IllegalStateException("Cannot restore the Fabric native block break phase invoker", failure);
        }
    }

    /** Installs the observer of one phase of the breaks of one player. */
    public static NativeBreakPhases install(ServerLevel level, ServerPlayer player, Phase phase, Listener listener) {
        try {
            var owner = Class.forName("net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents");
            var event = owner.getField(phase.name()).get(null);
            var invoker = event.getClass().getSuperclass().getDeclaredField("invoker");
            invoker.setAccessible(true);
            var previous = invoker.get(event);
            var callback = Class.forName(owner.getName() + "$"
                    + (phase == Phase.BEFORE ? "Before" : phase == Phase.CANCELED ? "Canceled" : "After"));
            invoker.set(event, Proxy.newProxyInstance(callback.getClassLoader(), new Class<?>[]{callback},
                    (proxy, method, args) -> {
                        Object original;
                        try {
                            original = method.invoke(previous, args);
                        } catch (InvocationTargetException failure) {
                            throw failure.getCause();
                        }
                        if (args != null && args.length == 5 && args[0] == level && args[1] == player) {
                            var observedLevel = (ServerLevel) args[0];
                            var observedPlayer = (ServerPlayer) args[1];
                            var pos = (BlockPos) args[2];
                            var state = (BlockState) args[3];
                            if (phase == Phase.BEFORE) {
                                // Another listener already refused the break: keep that result.
                                if (original instanceof Boolean allowed && !allowed) return false;
                                return listener.before(observedLevel, observedPlayer, pos, state);
                            }
                            if (phase == Phase.CANCELED) {
                                listener.canceled(observedLevel, observedPlayer, pos, state);
                            } else {
                                listener.after(observedLevel, observedPlayer, pos, state);
                            }
                        }
                        return original;
                    }));
            return new FabricBreakPhases(event, previous, invoker);
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("Fabric native block break phase probe unavailable", failure);
        }
    }
}
